# react-native-mtp-camera

get live image from nikon or canon camera to mobile through usb

[![npm version](https://img.shields.io/npm/v/@consolecodea/react-native-mtp-camera.svg)](https://www.npmjs.com/package/react-native-simple-recyclerlistview)
[![appveyor](https://ci.appveyor.com/api/projects/status/foon3b5reptapqgo/branch/main?svg=true)](https://ci.appveyor.com/project/consolecodea/react-native-mtp-camera)
[![MIT License](https://img.shields.io/badge/License-MIT-green.svg)](https://choosealicense.com/licenses/mit/)

## Installation

```sh
npm install @consolecodea/react-native-mtp-camera
```

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
