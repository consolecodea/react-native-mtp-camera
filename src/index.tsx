import { NativeEventEmitter } from 'react-native';

const MtpCamera = require('./NativeMtpCamera').default;
const mtpCameraEmitter = new NativeEventEmitter(MtpCamera);

export const startService = async () => {
  try {
    await MtpCamera.startImageLoadingService();
  } catch (error) {
    console.error('Failed to start service:', error);
  }
};

export const stopService = async () => {
  try {
    await MtpCamera.stopImageLoadingService();
  } catch (error) {
    console.error('Failed to stop service:', error);
  }
};

export const onNewImage = (callback: () => void) => {
  return mtpCameraEmitter.addListener('onNewImage', callback);
};
