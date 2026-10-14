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

  /// `adb reverse tcp:8080 tcp:8080` : the phone's own "localhost" is
  /// tunnelled straight to this machine's backend. Works over *either* a USB
  /// cable *or* an active wireless-debugging adb session — `adb reverse` only
  /// needs a live adb connection, not a physical cable. Re-enabled (true) on
  /// 04/10/2026 : the Wi-Fi/LAN-IP approach below kept breaking because this
  /// network's DHCP changes the dev machine's IP every so often (3 separate
  /// incidents in one session), each requiring a manual rebuild. The tunnel
  /// instead needs `adb reverse tcp:8080 tcp:8080` re-run after any adb
  /// disconnect (USB re-enumeration or Wi-Fi adb drop) — no rebuild, just a
  /// 10-second shell command — which is the lesser evil of the two failure
  /// modes seen so far.
  static const bool _useUsbReverseTunnel = true;

  /// LAN IP of the machine running the backend (find it with `ipconfig`,
  /// look under "Carte réseau sans fil Wi-Fi"). Used only when
  /// `_useUsbReverseTunnel` is false. A physical phone must be on the *same*
  /// Wi-Fi network as this machine to reach it, and Windows Firewall must
  /// allow inbound connections on port 8080. This changes whenever the dev
  /// machine joins a different network.
  static const String _devMachineLanIp = '10.5.51.49';

  /// URL du backend en production (voir Bakend/ABONNEMENTS_A_SOUSCRIRE.md) — utilisée par
  /// défaut pour la version web, puisqu'un site hébergé ne peut jamais atteindre "localhost"
  /// (celui du visiteur, pas celui du développeur). Remplaçable à la compilation via
  /// `--dart-define=API_BASE_URL=...` si besoin de pointer ailleurs (ex: tester le web en
  /// local contre un backend local).
  static const String _productionBaseUrl = 'https://maplenou-backend.onrender.com';

  static String get baseUrl {
    const envUrl = String.fromEnvironment('API_BASE_URL');
    if (envUrl.isNotEmpty) return envUrl;

    if (kIsWeb) return _productionBaseUrl;

    if (Platform.isAndroid) {
      if (_useAndroidEmulator) return 'http://10.0.2.2:$_port';
      if (_useUsbReverseTunnel) return 'http://localhost:$_port';
      return 'http://$_devMachineLanIp:$_port';
    }
    return 'http://localhost:$_port';
  }
}
