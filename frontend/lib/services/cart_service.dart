import '../models/cart_item_model.dart';

/// In-memory cart shared across the app session.
///
/// Mocked for now — no network call, no persistence across app restarts.
/// The real backend already has everything needed (`GET/POST/PATCH/DELETE
/// /api/cart/items`, scoped to `ProductVariant`), but wiring it requires
/// the authentication this session deliberately deferred (see
/// `CartItemModel`'s doc comment for the shape it maps onto).
class CartService {
  CartService._internal();
  static final CartService _instance = CartService._internal();
  factory CartService() => _instance;

  final List<CartItemModel> _items = [];

  List<CartItemModel> get items => List.unmodifiable(_items);

  num get subtotal => _items.fold(0, (sum, item) => sum + item.lineTotal);

  int get itemCount => _items.fold(0, (sum, item) => sum + item.quantity);

  /// Adds [quantity] of a product/variant, merging into an existing line
  /// (same product + variant) rather than creating a duplicate row.
  void addItem({
    required String productId,
    required String productName,
    String? variantId,
    String? variantLabel,
    String? imageUrl,
    required num unitPrice,
    int quantity = 1,
  }) {
    final existingIndex = _items.indexWhere((i) => i.productId == productId && i.variantId == variantId);
    if (existingIndex != -1) {
      final existing = _items[existingIndex];
      _items[existingIndex] = existing.copyWith(quantity: existing.quantity + quantity);
      return;
    }
    _items.add(CartItemModel(
      id: '${productId}_${variantId ?? 'default'}_${_items.length}',
      productId: productId,
      productName: productName,
      variantId: variantId,
      variantLabel: variantLabel,
      imageUrl: imageUrl,
      unitPrice: unitPrice,
      quantity: quantity,
    ));
  }

  void updateQuantity(String itemId, int quantity) {
    final index = _items.indexWhere((i) => i.id == itemId);
    if (index == -1) return;
    if (quantity <= 0) {
      _items.removeAt(index);
      return;
    }
    _items[index] = _items[index].copyWith(quantity: quantity);
  }

  void removeItem(String itemId) {
    _items.removeWhere((i) => i.id == itemId);
  }

  void clear() {
    _items.clear();
  }
}
