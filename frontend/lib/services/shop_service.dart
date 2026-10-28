import '../models/shop_model.dart';
import 'api_client.dart';

/// `catalog/ShopController.java` — boutique du vendeur connecté.
class ShopService {
  final _client = ApiClient();

  /// Fiche publique d'une boutique (`GET /api/shops/{slug}`, pas d'authentification requise) —
  /// reachée depuis [ShopDetailScreen] (bouton "Voir la boutique" sur une fiche produit, signalé
  /// manquant dans le rapport de modification du 28/10/2026 : le backend était prêt, mais rien
  /// côté app ne permettait à un acheteur de visiter la boutique d'un vendeur).
  Future<ShopModel> getBySlug(String slug) async {
    final data = await _client.get('/api/shops/$slug') as Map<String, dynamic>;
    return ShopModel.fromJson(data);
  }

  /// `null` si le vendeur (déjà approuvé) n'a pas encore créé sa boutique
  /// (404 attendu dans ce cas, pas une erreur à afficher).
  Future<ShopModel?> getMine() async {
    try {
      final data = await _client.get('/api/shops/mine') as Map<String, dynamic>;
      return ShopModel.fromJson(data);
    } on ApiException catch (e) {
      if (e.statusCode == 404) return null;
      rethrow;
    }
  }

  Future<ShopModel> create({
    required String name,
    String? description,
    required String city,
    String? district,
    String? taxId,
    double? latitude,
    double? longitude,
  }) async {
    final data = await _client.post(
      '/api/shops',
      body: {
        'name': name,
        'description': description,
        'city': city,
        'district': district,
        'taxId': taxId,
        'latitude': latitude,
        'longitude': longitude,
      },
    ) as Map<String, dynamic>;
    return ShopModel.fromJson(data);
  }

  Future<ShopModel> updateMine({
    String? name,
    String? description,
    String? logoUrl,
    String? coverUrl,
    String? taxId,
    double? latitude,
    double? longitude,
  }) async {
    final data = await _client.patch(
      '/api/shops/mine',
      body: {
        if (name != null) 'name': name,
        if (description != null) 'description': description,
        if (logoUrl != null) 'logoUrl': logoUrl,
        if (coverUrl != null) 'coverUrl': coverUrl,
        if (taxId != null) 'taxId': taxId,
        if (latitude != null) 'latitude': latitude,
        if (longitude != null) 'longitude': longitude,
      },
    ) as Map<String, dynamic>;
    return ShopModel.fromJson(data);
  }

  /// Remplace l'horaire d'ouverture complet (au plus 7 entrées) — point 1 du rapport de
  /// modification du 28/10/2026.
  Future<ShopModel> updateOpeningHours(List<OpeningHourModel> hours) async {
    final data = await _client.put(
      '/api/shops/mine/opening-hours',
      body: {'hours': hours.map((h) => h.toJson()).toList()},
    ) as Map<String, dynamic>;
    return ShopModel.fromJson(data);
  }
}
