package com.mtpcamera;

import static com.mtpcamera.ImageLoadingService.EXTRA_USB_DEVICE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.mtp.MtpConstants;
import android.mtp.MtpDevice;
import android.mtp.MtpEvent;
import android.mtp.MtpObjectInfo;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ReactModule(name = MtpCameraModule.NAME)
public class MtpCameraModule extends NativeMtpCameraSpec {
  public static final String NAME = "MtpCamera";
  private static final int EVENT_OBJECT_ADDED = 16386;
  private final ReactApplicationContext reactContext;
  private BroadcastReceiver imageReceiver;
  private ScheduledExecutorService scheduler;
  private MtpDevice mtpDevice;
  private int lastObjectHandle = -1;

  public MtpCameraModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
    initializeReceiver();
  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }

  @ReactMethod
  public void startImageLoadingService( Promise promise) {
    try {
      UsbDevice usbDevice = reactContext.getCurrentActivity().getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE);
      if (usbDevice != null) {
        Intent serviceIntent = new Intent(reactContext, ImageLoadingService.class);
        serviceIntent.putExtra(EXTRA_USB_DEVICE, usbDevice);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          reactContext.startForegroundService(serviceIntent);
        } else {
          reactContext.startService(serviceIntent);
        }
        openMtpDevice(usbDevice);
        startImageCheckScheduler();
        promise.resolve(null);
      } else {
        promise.reject("USB_DEVICE_NOT_FOUND", "USB device not found");
      }
    } catch (Exception e) {
      promise.reject("SERVICE_START_FAILED", e);
    }
  }

  @ReactMethod
  public void stopImageLoadingService(Promise promise) {
    try {
      Intent serviceIntent = new Intent(reactContext, ImageLoadingService.class);
      reactContext.stopService(serviceIntent);
      stopImageCheckScheduler();
      closeMtpDevice();
      promise.resolve(null);
    } catch (Exception e) {
      promise.reject("SERVICE_STOP_FAILED", e);
    }
  }



  private void openMtpDevice(UsbDevice usbDevice) {
    UsbManager usbManager = (UsbManager) reactContext.getSystemService(Context.USB_SERVICE);
    UsbDeviceConnection connection = usbManager.openDevice(usbDevice);
    if (connection != null) {
      mtpDevice = new MtpDevice(usbDevice);
      if (!mtpDevice.open(connection)) {
        Log.e("MtpCameraModule", "Failed to open MTP device.");
        closeMtpDevice();
      }
    } else {
      Log.e("MtpCameraModule", "Failed to open USB device connection.");
    }
  }

  private void closeMtpDevice() {
    if (mtpDevice != null) {
      mtpDevice.close();
      mtpDevice = null;
    }
  }

  private void startImageCheckScheduler() {
    scheduler = Executors.newSingleThreadScheduledExecutor();
    scheduler.scheduleWithFixedDelay(this::checkForNewImages, 1, 500, TimeUnit.MILLISECONDS);
  }

  private void stopImageCheckScheduler() {
    if (scheduler != null) {
      scheduler.shutdown();
      scheduler = null;
    }
  }

  private void checkForNewImages() {
    if (mtpDevice != null) {
      try {
        MtpEvent event = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          event = mtpDevice.readEvent(null);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          if (event.getEventCode() == EVENT_OBJECT_ADDED) {
            int objectHandle = 0;
            objectHandle = event.getParameter1();
            if (objectHandle != lastObjectHandle) {
              lastObjectHandle = objectHandle;
              processNewImage(objectHandle);
            }
          }
        }
      } catch (IOException e) {
        Log.e("MtpCameraModule", "Error reading MTP event", e);
      }
    }
  }

  private void processNewImage(int objectHandle) {
    if (mtpDevice == null) {
      return;
    }

    MtpObjectInfo objectInfo = mtpDevice.getObjectInfo(objectHandle);
    if (objectInfo == null || objectInfo.getFormat() != MtpConstants.FORMAT_EXIF_JPEG) {
      return;
    }

    byte[] objectData = mtpDevice.getObject(objectHandle, objectInfo.getCompressedSize());
    if (objectData == null) {
      return;
    }

    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inSampleSize = calculateInSampleSize(objectInfo.getImagePixWidth(), objectInfo.getImagePixHeight());
    Bitmap bitmap = BitmapFactory.decodeByteArray(objectData, 0, objectData.length, options);
    sendNewImageBroadcast(bitmap);
  }

  private int calculateInSampleSize(int width, int height) {
    final int MAX_IMAGE_WIDTH = 800;
    final int MAX_IMAGE_HEIGHT = 600;
    int inSampleSize = 1;

    if (width > MAX_IMAGE_WIDTH || height > MAX_IMAGE_HEIGHT) {
      final int halfWidth = width / 2;
      final int halfHeight = height / 2;

      while ((halfWidth / inSampleSize) >= MAX_IMAGE_WIDTH
        && (halfHeight / inSampleSize) >= MAX_IMAGE_HEIGHT) {
        inSampleSize *= 2;
      }
    }

    return inSampleSize;
  }

  private void sendNewImageBroadcast(Bitmap bitmap) {
    Intent intent = new Intent(ImageLoadingService.ACTION_NEW_IMAGE);
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
    byte[] byteArray = stream.toByteArray();
    intent.putExtra(ImageLoadingService.EXTRA_IMAGE_DATA, Base64.encodeToString(byteArray, Base64.DEFAULT));
    LocalBroadcastManager.getInstance(reactContext).sendBroadcast(intent);
  }

  private void initializeReceiver() {
    imageReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        if (ImageLoadingService.ACTION_NEW_IMAGE.equals(intent.getAction())) {
          String encodedImage = intent.getStringExtra(ImageLoadingService.EXTRA_IMAGE_DATA);
          if (encodedImage != null && !encodedImage.isEmpty()) {
            WritableMap params = Arguments.createMap();
            params.putString("imageData", encodedImage);
            sendEvent("onNewImage", params);
          }
        }
      }
    };

    IntentFilter filter = new IntentFilter(ImageLoadingService.ACTION_NEW_IMAGE);
    LocalBroadcastManager.getInstance(reactContext).registerReceiver(imageReceiver, filter);
  }

  private void sendEvent(String eventName, @NonNull WritableMap params) {
    if (reactContext.hasActiveCatalystInstance()) {
      reactContext
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
        .emit(eventName, params);
    }
  }
}
