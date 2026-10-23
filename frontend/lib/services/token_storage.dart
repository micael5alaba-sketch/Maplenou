import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Stockage sécurisé (Keychain/Keystore) du JWT — jamais via
/// `SharedPreferences`, qui écrit en clair sur le disque.
///
/// Toutes les opérations sont défensives (timeout + capture d'erreur), pas
/// seulement les lectures : sur le web, ce plugin passe par `localStorage`
/// du navigateur, que Safari bloque entièrement en navigation privée (ITP —
/// toute écriture y lève une exception, contrairement à Chrome/Firefox qui
/// l'autorisent juste sans la persister). Sans cette protection, `saveTokens`
/// plantait silencieusement après une connexion pourtant réussie côté
/// serveur : le bouton arrêtait de charger, mais rien ne se passait ensuite,
/// aucun message d'erreur affiché (l'exception n'était pas un `ApiException`,
/// donc jamais rattrapée par les écrans). Dégrade désormais en "session non
/// persistée" (l'utilisateur reste connecté pour cette page, mais devra se
/// reconnecter après un rechargement) plutôt que de bloquer le flux entier.
class TokenStorage {
  TokenStorage._internal();
  static final TokenStorage _instance = TokenStorage._internal();
  factory TokenStorage() => _instance;

  static const _storage = FlutterSecureStorage();
  static const _accessTokenKey = 'access_token';
  static const _refreshTokenKey = 'refresh_token';
  static const _opTimeout = Duration(seconds: 3);

  Future<void> saveTokens({required String accessToken, required String refreshToken}) async {
    await _writeSafely(_accessTokenKey, accessToken);
    await _writeSafely(_refreshTokenKey, refreshToken);
  }

  Future<String?> getAccessToken() => _readSafely(_accessTokenKey);

  Future<String?> getRefreshToken() => _readSafely(_refreshTokenKey);

  Future<void> clear() async {
    await _deleteSafely(_accessTokenKey);
    await _deleteSafely(_refreshTokenKey);
  }

  Future<String?> _readSafely(String key) async {
    try {
      return await _storage.read(key: key).timeout(_opTimeout);
    } catch (_) {
      return null;
    }
  }

  Future<void> _writeSafely(String key, String value) async {
    try {
      await _storage.write(key: key, value: value).timeout(_opTimeout);
    } catch (e) {
      debugPrint('TokenStorage: échec d\'écriture ($key) — session non persistée : $e');
    }
  }

  Future<void> _deleteSafely(String key) async {
    try {
      await _storage.delete(key: key).timeout(_opTimeout);
    } catch (_) {
      // Rien à faire : au pire la clé reste en place, sans conséquence
      // puisque l'appelant considère de toute façon la session terminée.
    }
  }
}
