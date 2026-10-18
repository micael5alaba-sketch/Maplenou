import '../models/delivery_pricing_tier_model.dart';
import 'api_client.dart';

/// `GET /api/delivery-pricing-tiers` (public, `DeliveryPricingController.java`) — barème
/// géré dynamiquement par l'admin (paliers de distance, plus de notion de zone nommée
/// depuis la refonte du 18/10/2026). Mis en cache en mémoire pour la session en cours.
class DeliveryPricingService {
  static List<DeliveryPricingTierModel>? _cachedTiers;

  Future<List<DeliveryPricingTierModel>> getTiers() async {
    final cached = _cachedTiers;
    if (cached != null) return cached;
    final data = await ApiClient().get('/api/delivery-pricing-tiers') as List<dynamic>;
    final tiers = data
        .map((json) => DeliveryPricingTierModel.fromJson(json as Map<String, dynamic>))
        .toList();
    _cachedTiers = tiers;
    return tiers;
  }

  /// Prix le plus bas du barème — utilisé en fiche produit comme estimation "à partir de"
  /// avant qu'une adresse (donc une distance réelle) ne soit connue.
  Future<num?> getCheapestPrice() async {
    final tiers = await getTiers();
    if (tiers.isEmpty) return null;
    return tiers.map((t) => t.price).reduce((a, b) => a < b ? a : b);
  }

  /// `GET /api/delivery-pricing-tiers/quote` — même calcul (distance réelle + palier) que
  /// celui figé côté backend au moment de la commande, pour que le checkout affiche
  /// exactement ce qui sera facturé plutôt qu'une estimation qui peut diverger.
  Future<DeliveryFeeQuoteModel> getQuote({
    required String shopId,
    double? latitude,
    double? longitude,
  }) async {
    final data = await ApiClient().get('/api/delivery-pricing-tiers/quote', query: {
      'shopId': shopId,
      if (latitude != null) 'latitude': '$latitude',
      if (longitude != null) 'longitude': '$longitude',
    }) as Map<String, dynamic>;
    return DeliveryFeeQuoteModel.fromJson(data);
  }
}
