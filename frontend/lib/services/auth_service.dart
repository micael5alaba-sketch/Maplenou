import 'dart:convert';

import 'package:http/http.dart' as http;

import '../config/api_config.dart';
import '../models/user_model.dart';
import 'api_client.dart';
import 'firebase_auth_bridge.dart';
import 'push_notification_service.dart';
import 'token_storage.dart';

/// Résultat d'une tentative de connexion : soit une session complète, soit
/// un défi 2FA (`LoginResponse.mfaRequired`, voir `AuthController.login`) —
/// dans ce second cas, l'écran doit demander le code TOTP et appeler
/// [AuthService.verifyMfaLogin] avec [mfaToken].
class LoginResult {
  final bool mfaRequired;
  final String? mfaToken;
  final UserModel? user;

  const LoginResult.success(UserModel this.user) : mfaRequired = false, mfaToken = null;

  const LoginResult.mfaChallenge(String this.mfaToken) : mfaRequired = true, user = null;
}

/// `/api/auth/*` (`auth/AuthController.java`) — inscription/connexion,
/// stockage du JWT via [TokenStorage]. `AuthController`/`TwoFactorController`
/// sont les seuls endpoints appelés sans JWT existant, donc ce service parle
/// directement en `http` plutôt que via [ApiClient] (qui suppose une session).
class AuthService {
  static const _timeout = Duration(seconds: 8);

  Future<UserModel> register({
    required String fullName,
    required String phoneNumber,
    required String password,
    String? accountType,
  }) async {
    final data = await _post('/api/auth/register', {
      'fullName': fullName,
      'phoneNumber': phoneNumber,
      'password': password,
      if (accountType != null) 'accountType': accountType,
    });
    return _storeSessionAndReturnUser(data);
  }

  Future<LoginResult> login({required String phoneNumber, required String password}) async {
    final data = await _post('/api/auth/login', {
      'phoneNumber': phoneNumber,
      'password': password,
    });

    if (data['mfaRequired'] == true) {
      return LoginResult.mfaChallenge(data['mfaToken'] as String);
    }
    final user = await _storeSessionAndReturnUser(data);
    return LoginResult.success(user);
  }

  Future<UserModel> verifyMfaLogin({required String mfaToken, required String code}) async {
    final data = await _post('/api/auth/2fa/verify-login', {'mfaToken': mfaToken, 'code': code});
    return _storeSessionAndReturnUser(data);
  }

  /// Envoie un code à 6 chiffres par email au compte associé à ce numéro.
  Future<void> forgotPassword({required String phoneNumber}) async {
    await _post('/api/auth/forgot-password', {'phoneNumber': phoneNumber});
  }

  /// Vérifie le code reçu par email et définit le nouveau mot de passe.
  Future<void> resetPassword({
    required String phoneNumber,
    required String code,
    required String newPassword,
  }) async {
    await _post('/api/auth/reset-password', {
      'phoneNumber': phoneNumber,
      'code': code,
      'newPassword': newPassword,
    });
  }

  Future<void> logout() async {
    final tokenStorage = TokenStorage();
    final refreshToken = await tokenStorage.getRefreshToken();
    final accessToken = await tokenStorage.getAccessToken();
    await PushNotificationService().unregisterAll();
    if (refreshToken != null) {
      try {
        await _post('/api/auth/logout', {
          'refreshToken': refreshToken,
          if (accessToken != null) 'accessToken': accessToken,
        });
      } catch (_) {
        // Le logout local doit réussir même si l'appel réseau échoue.
      }
    }
    await tokenStorage.clear();
    await FirebaseAuthBridge().signOut();
  }

  Future<UserModel> _storeSessionAndReturnUser(Map<String, dynamic> data) async {
    await TokenStorage().saveTokens(
      accessToken: data['accessToken'] as String,
      refreshToken: data['refreshToken'] as String,
    );
    PushNotificationService().initialize();
    return UserModel.fromJson(data['user'] as Map<String, dynamic>);
  }

  Future<Map<String, dynamic>> _post(String path, Map<String, dynamic> body) async {
    final uri = Uri.parse('${ApiConfig.baseUrl}$path');
    final http.Response response;
    try {
      response = await http
          .post(uri, headers: {'Content-Type': 'application/json'}, body: jsonEncode(body))
          .timeout(_timeout);
    } on Exception {
      throw ApiException(0, 'Le serveur ne répond pas (délai dépassé ou hors ligne)');
    }

    final decoded = response.body.isEmpty ? <String, dynamic>{} : jsonDecode(response.body);
    if (response.statusCode < 200 || response.statusCode >= 300) {
      final message = decoded is Map<String, dynamic> ? decoded['message'] as String? : null;
      throw ApiException(response.statusCode, message ?? 'Erreur serveur (${response.statusCode})');
    }
    return decoded as Map<String, dynamic>;
  }
}
