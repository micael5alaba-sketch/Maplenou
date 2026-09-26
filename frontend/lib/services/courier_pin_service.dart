import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Code PIN local ("Sécurité de connexion" dans les réglages livreur) : un
/// deuxième verrou propre à l'appareil, demandé à chaque lancement de l'app
/// une fois configuré (voir [CourierPinLockScreen]). Ce n'est pas un moyen
/// d'authentification serveur — jamais transmis au backend, stocké
/// uniquement dans le Keychain/Keystore local, comme le JWT (`TokenStorage`).
class CourierPinService {
  CourierPinService._internal();
  static final CourierPinService _instance = CourierPinService._internal();
  factory CourierPinService() => _instance;

  static const _storage = FlutterSecureStorage();
  static const _pinKey = 'courier_pin';
  static const _readTimeout = Duration(seconds: 3);

  Future<bool> isPinSet() async {
    final pin = await _readSafely();
    return pin != null && pin.isNotEmpty;
  }

  Future<void> setPin(String pin) => _storage.write(key: _pinKey, value: pin);

  Future<bool> verifyPin(String pin) async {
    final stored = await _readSafely();
    return stored != null && stored == pin;
  }

  Future<void> clearPin() => _storage.delete(key: _pinKey);

  Future<String?> _readSafely() async {
    try {
      return await _storage.read(key: _pinKey).timeout(_readTimeout);
    } catch (_) {
      return null;
    }
  }
}
