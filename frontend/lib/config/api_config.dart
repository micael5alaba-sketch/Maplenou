import 'dart:io' show Platform;

import 'package:flutter/foundation.dart' show kIsWeb;

/// Base URL of the Spring Boot backend, dockerized and listening on port
/// 8080 (see `Bakend/docker-compose.yml`).
///
/// "localhost" doesn't mean the same host from every runtime:
/// - Android emulator: the host machine is reachable at 10.0.2.2, not
///   localhost (which would point at the emulator itself).
/// - iOS simulator / desktop / web: localhost reaches the host directly.
/// - Physical device: replace with your machine's LAN IP (e.g.
///   `http://192.168.1.42:8080`), since neither localhost nor 10.0.2.2
///   resolves to your computer from a real phone.
class ApiConfig {
  static const String _port = '8080';

  /// Flip to true when running on the Android *emulator*. Leave false for a
  /// physical phone — 10.0.2.2 doesn't exist on a real network, so a real
  /// device would hang trying to reach it until the connection times out.
  static const bool _useAndroidEmulator = false;

  /// LAN IP of the machine running the backend (find it with `ipconfig`,
  /// look under "Carte réseau sans fil Wi-Fi"). A physical phone must be on
  /// the *same* Wi-Fi network as this machine to reach it, and Windows
  /// Firewall must allow inbound connections on port 8080. This changes
  /// whenever the dev machine joins a different network.
  static const String _devMachineLanIp = '10.149.225.251';

  static String get baseUrl {
    if (!kIsWeb && Platform.isAndroid) {
      return _useAndroidEmulator ? 'http://10.0.2.2:$_port' : 'http://$_devMachineLanIp:$_port';
    }
    return 'http://localhost:$_port';
  }
}
