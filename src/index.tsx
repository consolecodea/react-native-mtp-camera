import { NativeEventEmitter } from 'react-native';

const MtpCamera = require('./NativeMtpCamera').default;
const mtpCameraEmitter = new NativeEventEmitter(MtpCamera);

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

export const onNewImage = (callback: (e: any) => void) => {
  return mtpCameraEmitter.addListener('onNewImage', callback);
};
