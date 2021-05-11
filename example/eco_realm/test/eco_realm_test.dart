import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:eco_realm/eco_realm.dart';

void main() {
  const MethodChannel channel = MethodChannel('eco_realm');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await EcoRealm.platformVersion, '42');
  });
}
