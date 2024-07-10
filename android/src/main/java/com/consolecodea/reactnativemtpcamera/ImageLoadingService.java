package com.consolecodea.reactnativemtpcamera;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.facebook.react.ReactApplication;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.modules.core.DeviceEventManagerModule;

public class ImageLoadingService extends Service {
  private static final int NOTIFICATION_ID = 123;
  public static final String EXTRA_USB_DEVICE = "extra_usb_device";
  public static final String ACTION_NEW_IMAGE = "com.rupiapps.cameraapp.NEW_IMAGE";
  public static final String EXTRA_IMAGE_DATA = "extra_image_data";

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      String channelId = createNotificationChannel();
      Notification notification = buildNotification(channelId);
      startForeground(NOTIFICATION_ID, notification);
    }

    // Handle intent and do your service work here...

    return START_STICKY;
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  private Notification buildNotification(String channelId) {
    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
      .setContentTitle("Image Loading Service")
      .setContentText("Running...")
      .setSmallIcon(R.drawable.baseline_notifications_24)
      .setAutoCancel(true); // Optional: Auto cancel notification when tapped

    return builder.build();
  }



  private String createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      String channelId = "ImageLoadingServiceChannel";
      String channelName = "Image Loading Service Channel";
      NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
      NotificationManager notificationManager = getSystemService(NotificationManager.class);
      if (notificationManager != null) {
        notificationManager.createNotificationChannel(channel);
      }
      return channelId;
    }
    return null;
  }
}
