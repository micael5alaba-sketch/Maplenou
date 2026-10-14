import '../models/vendor_product_model.dart';
import 'api_client.dart';

/// `catalog/ProductController.java` — CRUD produits du vendeur connecté
/// (`/api/shops/mine/products`).
class VendorCatalogService {
  final _client = ApiClient();

  Future<List<VendorProductModel>> getProducts({int size = 50}) async {
    final data = await _client.get('/api/shops/mine/products', query: {'size': '$size'}) as Map<String, dynamic>;
    final content = data['content'] as List<dynamic>? ?? [];
    return content.map((json) => VendorProductModel.fromJson(json as Map<String, dynamic>)).toList();
  }

  /// Crée un produit avec une ou plusieurs variantes (voir `AddProductScreen`
  /// — chaque variante a son propre libellé libre, stock et prix optionnel),
  /// puis l'active immédiatement (`status: ACTIVE`) : un produit créé reste
  /// `DRAFT` par défaut côté backend, invisible du catalogue public tant
  /// qu'il n'est pas activé.
  Future<void> createProduct({
    required String name,
    String? description,
    required num basePrice,
    num? oldPrice,
    required String categoryId,
    required List<Map<String, dynamic>> variants,
    List<Map<String, String>> specifications = const [],
    List<String> imageUrls = const [],
    num? packageWeightKg,
    String? estimatedDeliveryDelay,
    bool deliveryAvailable = true,
  }) async {
    final data = await _client.post(
      '/api/shops/mine/products',
      body: {
        'name': name,
        'description': description,
        'basePrice': basePrice,
        'oldPrice': oldPrice,
        'categoryId': categoryId,
        'variants': variants,
        'specifications': specifications,
        'packageWeightKg': packageWeightKg,
        'estimatedDeliveryDelay': estimatedDeliveryDelay,
        'deliveryAvailable': deliveryAvailable,
      },
    ) as Map<String, dynamic>;

    final productId = data['id'] as String;

    for (var i = 0; i < imageUrls.length; i++) {
      await _client.post(
        '/api/shops/mine/products/$productId/images',
        body: {'url': imageUrls[i], 'position': i},
      );
    }

    await _client.patch('/api/shops/mine/products/$productId', body: {'status': 'ACTIVE'});
  }

  Future<void> deleteProduct(String productId) => _client.delete('/api/shops/mine/products/$productId');

  /// Duplique un produit existant (`ProductController.duplicate`) : copie
  /// intégrale (variantes, images, specs) en `DRAFT`, à réactiver ensuite.
  Future<void> duplicateProduct(String productId) => _client.post('/api/shops/mine/products/$productId/duplicate');

  /// Fiche complète d'un de mes produits (`GET /.../products/{id}`,
  /// `ProductDetailResponse`) — pour préremplir [EditProductScreen].
  Future<Map<String, dynamic>> getProductDetail(String productId) async {
    return await _client.get('/api/shops/mine/products/$productId') as Map<String, dynamic>;
  }

  Future<void> updateProduct({
    required String productId,
    required String name,
    String? description,
    required num basePrice,
    num? oldPrice,
    required String categoryId,
    required String status,
    List<Map<String, String>> specifications = const [],
    num? packageWeightKg,
    String? estimatedDeliveryDelay,
    bool deliveryAvailable = true,
  }) {
    return _client.patch(
      '/api/shops/mine/products/$productId',
      body: {
        'name': name,
        'description': description,
        'basePrice': basePrice,
        'oldPrice': oldPrice,
        'categoryId': categoryId,
        'status': status,
        'specifications': specifications,
        'packageWeightKg': packageWeightKg,
        'estimatedDeliveryDelay': estimatedDeliveryDelay,
        'deliveryAvailable': deliveryAvailable,
      },
    );
  }

  Future<Map<String, dynamic>> addVariant(String productId, {required String label, required int stockQuantity, num? priceOverride}) async {
    return await _client.post(
      '/api/shops/mine/products/$productId/variants',
      body: {'label': label, 'stockQuantity': stockQuantity, if (priceOverride != null) 'priceOverride': priceOverride},
    ) as Map<String, dynamic>;
  }

  Future<void> updateVariant(String productId, String variantId, {required String label, required int stockQuantity, num? priceOverride}) {
    return _client.patch(
      '/api/shops/mine/products/$productId/variants/$variantId',
      body: {'label': label, 'stockQuantity': stockQuantity, 'priceOverride': priceOverride},
    );
  }

  Future<void> deleteVariant(String productId, String variantId) =>
      _client.delete('/api/shops/mine/products/$productId/variants/$variantId');

  Future<void> addImage(String productId, String url, int position) {
    return _client.post('/api/shops/mine/products/$productId/images', body: {'url': url, 'position': position});
  }

  Future<void> deleteImage(String productId, String imageId) =>
      _client.delete('/api/shops/mine/products/$productId/images/$imageId');
}
