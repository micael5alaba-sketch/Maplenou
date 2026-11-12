import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../models/reseller_model.dart';
import 'api_client.dart';

/// `reseller/ResellerController.java` : espace du partenaire revendeur connecté, plus l'enregistrement
/// des clics sur un lien partenaire et la conservation locale des jetons d'attribution.
class ResellerService {
  final _client = ApiClient();

  static const _tokensKey = 'reseller_attribution_tokens';

  Future<ResellerSettingsModel> getSettings() async {
    final data = await _client.get('/api/reseller/settings') as Map<String, dynamic>;
    return ResellerSettingsModel.fromJson(data);
  }

  /// Profil partenaire du compte, ou null si aucune demande n'a été faite (404).
  Future<ResellerProfileModel?> getMyProfile() async {
    try {
      final data = await _client.get('/api/reseller/me') as Map<String, dynamic>;
      return ResellerProfileModel.fromJson(data);
    } on ApiException catch (e) {
      if (e.statusCode == 404) return null;
      rethrow;
    }
  }

  Future<ResellerProfileModel> apply() async {
    final data = await _client.post('/api/reseller/apply', body: {'acceptTerms': true}) as Map<String, dynamic>;
    return ResellerProfileModel.fromJson(data);
  }

  Future<List<ResellerCatalogItem>> getCatalog({int page = 0, int size = 20}) async {
    final data = await _client.get('/api/reseller/catalog', query: {'page': '$page', 'size': '$size'})
        as Map<String, dynamic>;
    return (data['content'] as List<dynamic>)
        .map((j) => ResellerCatalogItem.fromJson(j as Map<String, dynamic>))
        .toList();
  }

  /// Crée (ou retrouve) le lien du partenaire pour un produit ; renvoie l'URL à partager.
  Future<String> createLink(String productId) async {
    final data = await _client.post('/api/reseller/links', body: {'productId': productId}) as Map<String, dynamic>;
    return data['url'] as String;
  }

  Future<List<ResellerCommissionModel>> getCommissions({int size = 50}) async {
    final data = await _client.get('/api/reseller/commissions', query: {'size': '$size'}) as Map<String, dynamic>;
    return (data['content'] as List<dynamic>)
        .map((j) => ResellerCommissionModel.fromJson(j as Map<String, dynamic>))
        .toList();
  }

  /// Statistiques : [days] = 7, 30 ou 90, ou une période personnalisée [from]/[to].
  Future<ResellerStatsModel> getStats({int? days, DateTime? from, DateTime? to}) async {
    String day(DateTime d) => d.toIso8601String().substring(0, 10);
    final query = days != null ? {'period': '$days'} : {'from': day(from!), 'to': day(to!)};
    final data = await _client.get('/api/reseller/stats', query: query) as Map<String, dynamic>;
    return ResellerStatsModel.fromJson(data);
  }

  Future<ResellerWalletModel> getWallet() async {
    final data = await _client.get('/api/reseller/wallet') as Map<String, dynamic>;
    return ResellerWalletModel.fromJson(data);
  }

  Future<List<ResellerWithdrawalModel>> getWithdrawals({int size = 20}) async {
    final data = await _client.get('/api/reseller/withdrawals', query: {'size': '$size'}) as Map<String, dynamic>;
    return (data['content'] as List<dynamic>)
        .map((j) => ResellerWithdrawalModel.fromJson(j as Map<String, dynamic>))
        .toList();
  }

  /// [idempotencyKey] identifie CETTE demande : un renvoi (double clic, coupure réseau) avec la même clé
  /// ne crée jamais un second retrait.
  Future<void> requestWithdrawal({
    required int amount,
    required String method,
    required String paymentPhone,
    required String idempotencyKey,
  }) async {
    await _client.post('/api/reseller/withdrawals',
        body: {'amount': amount, 'method': method, 'paymentPhone': paymentPhone},
        headers: {'Idempotency-Key': idempotencyKey});
  }

  // ----- Attribution (clic sur un lien partenaire) -----

  /// Enregistre le clic pour [code] et conserve le jeton d'attribution (un par produit, le dernier clic
  /// remplace le précédent). Renvoie le slug du produit visé, ou null si le clic n'est pas valide.
  Future<String?> recordClick(String code) async {
    final data = await _client.post('/api/reseller/clicks', body: {'code': code, 'visitorId': await _visitorId()})
        as Map<String, dynamic>;
    if (data['valid'] != true) return null;
    await _saveToken(data['productId'] as String, data['token'] as String);
    return data['productSlug'] as String?;
  }

  /// Jetons d'attribution à joindre à la commande (le serveur ignore ceux qui sont expirés ou invalides).
  Future<List<String>> attributionTokens() async {
    final map = await _readTokens();
    return map.values.toList();
  }

  Future<String> _visitorId() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      var id = prefs.getString('reseller_visitor_id');
      if (id == null) {
        id = 'v${DateTime.now().microsecondsSinceEpoch}${UniqueKey().hashCode}';
        await prefs.setString('reseller_visitor_id', id);
      }
      return id;
    } catch (_) {
      return 'anonymous';
    }
  }

  Future<Map<String, String>> _readTokens() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final raw = prefs.getString(_tokensKey);
      if (raw == null) return {};
      return (jsonDecode(raw) as Map<String, dynamic>).map((k, v) => MapEntry(k, v as String));
    } catch (_) {
      return {};
    }
  }

  Future<void> _saveToken(String productId, String token) async {
    try {
      final map = await _readTokens();
      map[productId] = token;
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_tokensKey, jsonEncode(map));
    } catch (_) {
      // Stockage indisponible : l'attribution liée au compte (côté serveur) prend le relais.
    }
  }
}
