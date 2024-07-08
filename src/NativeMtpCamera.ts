import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
  startImageLoadingService(): Promise<void>;
  stopImageLoadingService(): Promise<void>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('MtpCamera');
