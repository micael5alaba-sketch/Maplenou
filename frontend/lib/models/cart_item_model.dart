/// One line in the buyer's cart. Maps onto the backend's `CartItem`
/// (which references a `ProductVariant`, not a bare product — see
/// `MODELE_DONNEES.md`), hence [variantLabel] and [variantId].
class CartItemModel {
  final String id;
  final String productId;
  final String productName;
  final String? variantId;
  final String? variantLabel;
  final String? imageUrl;
  final num unitPrice;
  final int quantity;

  const CartItemModel({
    required this.id,
    required this.productId,
    required this.productName,
    this.variantId,
    this.variantLabel,
    this.imageUrl,
    required this.unitPrice,
    required this.quantity,
  });

  num get lineTotal => unitPrice * quantity;

  CartItemModel copyWith({int? quantity}) {
    return CartItemModel(
      id: id,
      productId: productId,
      productName: productName,
      variantId: variantId,
      variantLabel: variantLabel,
      imageUrl: imageUrl,
      unitPrice: unitPrice,
      quantity: quantity ?? this.quantity,
    );
  }
}
