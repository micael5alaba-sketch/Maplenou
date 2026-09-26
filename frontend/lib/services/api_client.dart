import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;

import '../config/api_config.dart';
import '../navigation.dart';
import '../screens/login_screen.dart';
import 'token_storage.dart';

/// Erreur levée pour toute réponse HTTP non 2xx, avec le message renvoyé par
/// le backend (`ErrorResponse.message`, voir `GlobalExceptionHandler`).
class ApiException implements Exception {
  final int statusCode;
  final String message;

  ApiException(this.statusCode, this.message);

  @override
  String toString() => message;
}

/// Client HTTP central pour tous les appels authentifiés au backend.
///
/// Injecte automatiquement `Authorization: Bearer <token>` depuis
/// [TokenStorage], uniformise timeout/décodage JSON/erreurs, et gère
/// l'expiration de session : un 401 déclenche une tentative de
/// rafraîchissement (`POST /api/auth/refresh`) puis, en cas d'échec,
/// déconnecte et redirige vers [LoginScreen].
class ApiClient {
  ApiClient._internal();
  static final ApiClient _instance = ApiClient._internal();
  factory ApiClient() => _instance;

  final _tokenStorage = TokenStorage();
  static const _timeout = Duration(seconds: 8);

  Future<dynamic> get(String path, {Map<String, String>? query}) =>
      _send('GET', path, query: query);

  Future<dynamic> post(String path, {Object? body}) => _send('POST', path, body: body);

  Future<dynamic> patch(String path, {Object? body}) => _send('PATCH', path, body: body);

  Future<dynamic> put(String path, {Object? body}) => _send('PUT', path, body: body);

  Future<dynamic> delete(String path, {Object? body}) => _send('DELETE', path, body: body);

  Future<dynamic> _send(
    String method,
    String path, {
    Map<String, String>? query,
    Object? body,
    bool isRetryAfterRefresh = false,
  }) async {
    final uri = Uri.parse('${ApiConfig.baseUrl}$path').replace(queryParameters: query);
    final token = await _tokenStorage.getAccessToken();

    final request = http.Request(method, uri)
      ..headers['Content-Type'] = 'application/json'
      ..headers.addAll(token != null ? {'Authorization': 'Bearer $token'} : {});
    if (body != null) request.body = jsonEncode(body);

    final http.Response response;
    try {
      final streamed = await request.send().timeout(_timeout);
      response = await http.Response.fromStream(streamed);
    } on Exception {
      throw ApiException(0, 'Le serveur ne répond pas (délai dépassé ou hors ligne)');
    }

    if (response.statusCode == 401 && !isRetryAfterRefresh && token != null) {
      if (await _tryRefresh()) {
        return _send(method, path, query: query, body: body, isRetryAfterRefresh: true);
      }
      await _tokenStorage.clear();
      _redirectToLogin();
      throw ApiException(401, 'Session expirée, veuillez vous reconnecter');
    }

    if (response.statusCode >= 200 && response.statusCode < 300) {
      if (response.body.isEmpty) return null;
      return jsonDecode(response.body);
    }

    throw ApiException(
      response.statusCode,
      _extractMessage(response.body) ?? 'Erreur serveur (${response.statusCode})',
    );
  }

  String? _extractMessage(String body) {
    try {
      final decoded = jsonDecode(body);
      if (decoded is Map<String, dynamic>) {
        final fieldErrors = decoded['fieldErrors'] as List<dynamic>?;
        if (fieldErrors != null && fieldErrors.isNotEmpty) {
          return fieldErrors.map((e) => (e as Map<String, dynamic>)['message']).join('\n');
        }
        return decoded['message'] as String?;
      }
    } catch (_) {
      // Corps non-JSON (ex. erreur réseau brute) : on retombe sur le message générique.
    }
    return null;
  }

  Future<bool> _tryRefresh() async {
    final refreshToken = await _tokenStorage.getRefreshToken();
    if (refreshToken == null) return false;

    try {
      final uri = Uri.parse('${ApiConfig.baseUrl}/api/auth/refresh');
      final response = await http
          .post(
            uri,
            headers: {'Content-Type': 'application/json'},
            body: jsonEncode({'refreshToken': refreshToken}),
          )
          .timeout(_timeout);
      if (response.statusCode != 200) return false;

      final data = jsonDecode(response.body) as Map<String, dynamic>;
      await _tokenStorage.saveTokens(
        accessToken: data['accessToken'] as String,
        refreshToken: data['refreshToken'] as String,
      );
      return true;
    } catch (_) {
      return false;
    }
  }

  void _redirectToLogin() {
    final navigatorState = navigatorKey.currentState;
    if (navigatorState == null) return;
    navigatorState.pushAndRemoveUntil(
      MaterialPageRoute(builder: (_) => const LoginScreen()),
      (route) => false,
    );
  }
}
