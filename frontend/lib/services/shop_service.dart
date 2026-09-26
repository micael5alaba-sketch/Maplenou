import '../models/shop_model.dart';
import 'api_client.dart';

/// `catalog/ShopController.java` — boutique du vendeur connecté.
class ShopService {
  final _client = ApiClient();

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
  }) async {
    final data = await _client.post(
      '/api/shops',
      body: {'name': name, 'description': description, 'city': city, 'district': district, 'taxId': taxId},
    ) as Map<String, dynamic>;
    return ShopModel.fromJson(data);
  }

  Future<ShopModel> updateMine({
    String? name,
    String? description,
    String? logoUrl,
    String? coverUrl,
    String? taxId,
  }) async {
    final data = await _client.patch(
      '/api/shops/mine',
      body: {
        if (name != null) 'name': name,
        if (description != null) 'description': description,
        if (logoUrl != null) 'logoUrl': logoUrl,
        if (coverUrl != null) 'coverUrl': coverUrl,
        if (taxId != null) 'taxId': taxId,
      },
    ) as Map<String, dynamic>;
    return ShopModel.fromJson(data);
  }
}
