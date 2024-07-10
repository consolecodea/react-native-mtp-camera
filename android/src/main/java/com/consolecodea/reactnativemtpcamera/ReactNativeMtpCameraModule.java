package com.consolecodea.reactnativemtpcamera;

import static com.consolecodea.reactnativemtpcamera.ImageLoadingService.EXTRA_USB_DEVICE;

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
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ReactNativeMtpCameraModule extends ReactNativeMtpCameraSpec {
  public static final String NAME = "ReactNativeMtpCamera";
  private static final int EVENT_OBJECT_ADDED = 16386;
  private final ReactApplicationContext reactContext;
  private BroadcastReceiver imageReceiver;
  private ScheduledExecutorService scheduler;
  private MtpDevice mtpDevice;
  private int lastObjectHandle = -1;

  ReactNativeMtpCameraModule(ReactApplicationContext context) {
    super(context);
    this.reactContext = context;
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
    try {
      File imageFile = saveImageToFile(bitmap);

      if (imageFile != null) {
        // Convert bitmap to Base64 string
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        String imageBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT);

        // Broadcast intent with both imagePath and imageBase64
        Intent intent = new Intent(ImageLoadingService.ACTION_NEW_IMAGE);
        intent.putExtra(ImageLoadingService.EXTRA_IMAGE_DATA, imageFile.getAbsolutePath());
        intent.putExtra("imageBase64", imageBase64);
        LocalBroadcastManager.getInstance(reactContext).sendBroadcast(intent);
      }
    }
    catch (IOException e) {
      Log.e("MtpCameraModule", "Error saving image", e);
    }

  }


  private File saveImageToFile(Bitmap bitmap) throws IOException {
    File cacheDir = reactContext.getCacheDir();
    if (cacheDir == null) {
      throw new IOException("Failed to get cache directory");
    }

    String fileName = "image_" + System.currentTimeMillis() + ".jpg";
    File imageFile = new File(cacheDir, fileName);

    try (FileOutputStream out = new FileOutputStream(imageFile)) {
      bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
    }

    return imageFile;
  }



  private void initializeReceiver() {
    imageReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        if (ImageLoadingService.ACTION_NEW_IMAGE.equals(intent.getAction())) {
          String imagePath = intent.getStringExtra(ImageLoadingService.EXTRA_IMAGE_DATA);
          String imageBase64 = intent.getStringExtra("imageBase64");

          if (imagePath != null && !imagePath.isEmpty() && imageBase64 != null && !imageBase64.isEmpty()) {
            WritableMap params = Arguments.createMap();
            params.putString("imagePath", "file://" + imagePath); // Assuming you want to use this format
            params.putString("imageBase64", "data:image/jpeg;base64," + imageBase64);
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
