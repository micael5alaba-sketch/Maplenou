import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Stockage sécurisé (Keychain/Keystore) du JWT — jamais via
/// `SharedPreferences`, qui écrit en clair sur le disque.
///
/// Les lectures sont défensives (timeout + capture d'erreur) : un plugin de
/// stockage sécurisé indisponible (device chiffré non déverrouillé,
/// environnement de test sans implémentation native, etc.) doit dégrader en
/// "pas de session", jamais bloquer indéfiniment le démarrage de l'app.
class TokenStorage {
  TokenStorage._internal();
  static final TokenStorage _instance = TokenStorage._internal();
  factory TokenStorage() => _instance;

  static const _storage = FlutterSecureStorage();
  static const _accessTokenKey = 'access_token';
  static const _refreshTokenKey = 'refresh_token';
  static const _readTimeout = Duration(seconds: 3);

  Future<void> saveTokens({required String accessToken, required String refreshToken}) async {
    await _storage.write(key: _accessTokenKey, value: accessToken);
    await _storage.write(key: _refreshTokenKey, value: refreshToken);
  }

  Future<String?> getAccessToken() => _readSafely(_accessTokenKey);

  Future<String?> getRefreshToken() => _readSafely(_refreshTokenKey);

  Future<void> clear() async {
    await _storage.delete(key: _accessTokenKey);
    await _storage.delete(key: _refreshTokenKey);
  }

  Future<String?> _readSafely(String key) async {
    try {
      return await _storage.read(key: key).timeout(_readTimeout);
    } catch (_) {
      return null;
    }
  }
}
