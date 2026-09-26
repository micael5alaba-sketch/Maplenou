import '../models/product_model.dart';
import 'api_client.dart';

/// `GET/POST/DELETE /api/favorites` (`cart/FavoriteController.java`).
///
/// Garde en mémoire les ids déjà vus par [getFavorites] pour que
/// [isFavorite]/[toggle] (utilisés par l'icône cœur de la fiche produit)
/// n'aient pas à recharger toute la liste à chaque fois.
class FavoritesService {
  FavoritesService._internal();
  static final FavoritesService _instance = FavoritesService._internal();
  factory FavoritesService() => _instance;

  final _client = ApiClient();
  Set<String>? _knownFavoriteIds;

  Future<List<ProductModel>> getFavorites() async {
    final data = await _client.get('/api/favorites') as Map<String, dynamic>;
    final content = data['content'] as List<dynamic>? ?? [];
    final products = content.map((json) {
      final favorite = json as Map<String, dynamic>;
      return ProductModel(
        id: favorite['productId'] as String,
        name: favorite['productName'] as String,
        slug: favorite['productSlug'] as String,
        imageUrl: favorite['thumbnailUrl'] as String?,
        price: favorite['basePrice'] as num,
      );
    }).toList();
    _knownFavoriteIds = products.map((p) => p.id).toSet();
    return products;
  }

  Future<bool> isFavorite(String productId) async {
    _knownFavoriteIds ??= (await getFavorites()).map((p) => p.id).toSet();
    return _knownFavoriteIds!.contains(productId);
  }

  /// Bascule l'état et renvoie le nouvel état (favori ou non).
  Future<bool> toggle(String productId) async {
    if (await isFavorite(productId)) {
      await remove(productId);
      return false;
    }
    await add(productId);
    return true;
  }

  Future<void> add(String productId) async {
    await _client.post('/api/favorites', body: {'productId': productId});
    _knownFavoriteIds?.add(productId);
  }

  Future<void> remove(String productId) async {
    await _client.delete('/api/favorites/$productId');
    _knownFavoriteIds?.remove(productId);
  }
}
