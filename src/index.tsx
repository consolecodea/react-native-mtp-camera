import { NativeModules, Platform } from 'react-native';
const LINKING_ERROR =
  `The package '@consolecodea/react-native-mtp-camera' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

// @ts-expect-error
const isTurboModuleEnabled = global.__turboModuleProxy != null;

const MtpCameraModule = isTurboModuleEnabled
  ? require('./NativeMtpCamera').default
  : NativeModules.MtpCamera;

const MtpCamera = MtpCameraModule
  ? MtpCameraModule
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

export const startService = async () => {
  try {
    await MtpCamera.startImageLoadingService();
  } catch (error) {
    throw error;
  }
};

export const stopService = async () => {
  try {
    await MtpCamera.stopImageLoadingService();
  } catch (error) {
    throw error;
  }
};

export interface cameraEventLister {
  onNewImage: string;
}
export interface cameraEventProps {
  imagePath: string;
  imageBase64: string;
}

export const cameraEventLister: cameraEventLister = {
  onNewImage: 'onNewImage',
};
