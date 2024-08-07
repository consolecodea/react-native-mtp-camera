package com.consolecodea.reactnativemtpcamera;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.Promise;

abstract class ReactNativeMtpCameraSpec extends ReactContextBaseJavaModule {
  ReactNativeMtpCameraSpec(ReactApplicationContext context) {
    super(context);
  }

 public abstract void startImageLoadingService(Promise promise);


  public abstract void stopImageLoadingService(Promise promise);
}
