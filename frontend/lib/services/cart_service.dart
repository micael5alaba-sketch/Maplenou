import '../models/cart_model.dart';
import '../models/product_model.dart';
import 'api_client.dart';
import 'product_service.dart';

/// `GET/POST/PATCH/DELETE /api/cart[/items]` (`cart/CartController.java`).
class CartService {
  final _client = ApiClient();
  final _productService = ProductService();

  Future<CartModel> getCart() async {
    final data = await _client.get('/api/cart') as Map<String, dynamic>;
    return CartModel.fromJson(data);
  }

  /// Ajout rapide depuis une carte produit (grille catalogue), sans passer
  /// par le choix de variante de la fiche détail : prend la première
  /// variante du produit (le cas courant d'un produit à variante unique).
  Future<void> addProductQuickly(ProductModel product, {int quantity = 1}) async {
    final detail = await _productService.getProductDetail(product.slug);
    final variants = detail['variants'] as List<dynamic>? ?? [];
    if (variants.isEmpty) {
      throw ApiException(0, "Ce produit n'a pas encore de variante en vente.");
    }
    final variantId = (variants.first as Map<String, dynamic>)['id'] as String;
    await addItem(productVariantId: variantId, quantity: quantity);
  }

  Future<CartModel> addItem({required String productVariantId, int quantity = 1}) async {
    final data = await _client.post(
      '/api/cart/items',
      body: {'productVariantId': productVariantId, 'quantity': quantity},
    ) as Map<String, dynamic>;
    return CartModel.fromJson(data);
  }

  Future<CartModel> updateQuantity(String cartItemId, int quantity) async {
    final data = await _client.patch(
      '/api/cart/items/$cartItemId',
      body: {'quantity': quantity},
    ) as Map<String, dynamic>;
    return CartModel.fromJson(data);
  }

  Future<CartModel> removeItem(String cartItemId) async {
    final data = await _client.delete('/api/cart/items/$cartItemId') as Map<String, dynamic>;
    return CartModel.fromJson(data);
  }

  Future<void> clear() => _client.delete('/api/cart');
}
