
#ifdef RCT_NEW_ARCH_ENABLED
#import "RNMtpCameraSpec.h"

@interface MtpCamera : NSObject <NativeMtpCameraSpec>
#else
#import <React/RCTBridgeModule.h>

@interface MtpCamera : NSObject <RCTBridgeModule>
#endif

@end
