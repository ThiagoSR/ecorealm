#import "EcoRealmPlugin.h"
#if __has_include(<eco_realm/eco_realm-Swift.h>)
#import <eco_realm/eco_realm-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "eco_realm-Swift.h"
#endif

@implementation EcoRealmPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftEcoRealmPlugin registerWithRegistrar:registrar];
}
@end
