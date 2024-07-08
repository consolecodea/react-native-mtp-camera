# react-native-mtp-camera

get live image from nikon or canon camera to mobile through usb (Android only)

[![npm version](https://img.shields.io/npm/v/@consolecodea/react-native-mtp-camera.svg)](https://www.npmjs.com/package/react-native-simple-recyclerlistview)
[![appveyor](https://ci.appveyor.com/api/projects/status/foon3b5reptapqgo/branch/main?svg=true)](https://ci.appveyor.com/project/consolecodea/react-native-mtp-camera)
[![MIT License](https://img.shields.io/badge/License-MIT-green.svg)](https://choosealicense.com/licenses/mit/)

## Installation

```sh
npm install @consolecodea/react-native-mtp-camera

```

## Android Setup

### Permissions

To integrate react-native-mtp-camera into your Android project, follow these steps to configure your Android manifest:

- Open Your Android Manifest File:
  Navigate to your Android project's android/app/src/main/AndroidManifest.xml file.
- Add Permissions:
  Ensure the following permissions are included within the <manifest> tag:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />

```

These permissions are required for network communication, notifications, and running services in the foreground.

- Declare USB Host Feature:
  Add the USB host feature declaration. This is essential for communicating with USB devices:

```xml
<uses-feature android:name="android.hardware.usb.host" android:required="true" />
```

- Configure MainActivity:
  Update your <activity> tag for the MainActivity to handle USB device attachments and set launch configuration:

```xml
<activity
    android:name=".MainActivity"
    android:label="@string/app_name"
    android:configChanges="keyboard|keyboardHidden|orientation|screenLayout|screenSize|smallestScreenSize|uiMode"
    android:launchMode="singleTask"
    android:windowSoftInputMode="adjustResize"
    android:exported="true">

    <!-- Main launcher intent filter -->
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>

    <!-- Intent filter for USB device attached -->
    <intent-filter>
        <action android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED" />
    </intent-filter>

    <!-- Metadata for USB device filter -->
    <meta-data android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED"
        android:resource="@xml/device_filter" />
</activity>
```

- Create a new file named device_filter.xml under android/app/src/main/res/xml/ (if it doesn't exist already), and paste the following content:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <usb-device class="6" />
</resources>
```

- Add ImageLoadingService:
  If your package includes a service (e.g., ImageLoadingService), declare it within the <application> tag:

```xml
<service
    android:name="com.mtpcamera.ImageLoadingService"
    android:foregroundServiceType="dataSync" />

```

### Explanation:

- **Permissions**: Make sure to explain any critical permissions required by your package, such as `INTERNET`, `POST_NOTIFICATIONS`, and `FOREGROUND_SERVICE`. These are already included in your manifest.
- **Manifest Updates**: Provide a clear overview of how to integrate your package into an existing Android project, highlighting key aspects like `MainActivity` setup, USB device attachment handling, and the `ImageLoadingService`.

This updated README will help users understand how to configure their Android projects to work with your `react-native-mtp-camera` package effectively.

## Usage

```js
import {
  startService,
  stopService,
  onNewImage,
} from '@consolecodea/react-native-mtp-camera';

// Start the image loading service
startService()
  .then(() => {
    console.log('Service started');
  })
  .catch((error) => {
    console.error('Failed to start service:', error);
  });

// Stop the image loading service
stopService()
  .then(() => {
    console.log('Service stopped');
  })
  .catch((error) => {
    console.error('Failed to stop service:', error);
  });

// Listen for new images
const subscription = onNewImage((event) => {
  console.log('New image received:', event);
});

// Remember to remove the listener when it's no longer needed
subscription.remove();
```

## Props

| Method                 | Description                        |
| ---------------------- | ---------------------------------- |
| `startService()`       | Starts the image loading service.  |
| `stopService()`        | Stops the image loading service.   |
| `onNewImage(callback)` | Sets up a listener for new images. |

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT
