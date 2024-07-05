const MtpCamera = require('./NativeMtpCamera').default;

export function multiply(a: number, b: number): number {
  return MtpCamera.multiply(a, b);
}
