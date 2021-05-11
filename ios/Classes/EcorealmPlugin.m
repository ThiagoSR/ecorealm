#import "EcorealmPlugin.h"
#if __has_include(<ecorealm/ecorealm-Swift.h>)
#import <ecorealm/ecorealm-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "ecorealm-Swift.h"
#endif

@implementation EcorealmPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftEcorealmPlugin registerWithRegistrar:registrar];
}
@end
