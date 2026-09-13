import '../models/product_model.dart';

/// In-memory favorites shared across the app session (same pattern as
/// [CartService]).
///
/// Mocked for now — no network call, no persistence across app restarts.
/// Maps onto the real, already working `FavoriteController`
/// (`GET/POST/DELETE /api/favorites`) once auth is wired up.
class FavoritesService {
  FavoritesService._internal();
  static final FavoritesService _instance = FavoritesService._internal();
  factory FavoritesService() => _instance;

  final Map<String, ProductModel> _favoritesById = {};

  List<ProductModel> get items => List.unmodifiable(_favoritesById.values);

  bool isFavorite(String productId) => _favoritesById.containsKey(productId);

  /// Returns the new favorite state after toggling.
  bool toggle(ProductModel product) {
    if (_favoritesById.containsKey(product.id)) {
      _favoritesById.remove(product.id);
      return false;
    }
    _favoritesById[product.id] = product;
    return true;
  }

  void remove(String productId) {
    _favoritesById.remove(productId);
  }
}
