
import 'dart:async';

import 'package:flutter/services.dart';

class EcoRealm {
  static const MethodChannel _channel =
      const MethodChannel('eco_realm');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
}
