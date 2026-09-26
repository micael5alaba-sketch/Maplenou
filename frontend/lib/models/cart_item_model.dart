/// One line in the buyer's cart. Miroir de `CartItemResponse`
/// (`cart/dto/CartItemResponse.java`) — référence un `ProductVariant`, pas
/// un produit nu.
class CartItemModel {
  final String id;
  final String productId;
  final String productName;
  final String productSlug;
  final String variantId;
  final String? variantLabel;
  final String? imageUrl;
  final num unitPrice;
  final int quantity;
  final String shopId;
  final String shopName;
  final bool available;

  const CartItemModel({
    required this.id,
    required this.productId,
    required this.productName,
    required this.productSlug,
    required this.variantId,
    this.variantLabel,
    this.imageUrl,
    required this.unitPrice,
    required this.quantity,
    required this.shopId,
    required this.shopName,
    this.available = true,
  });

  num get lineTotal => unitPrice * quantity;

  factory CartItemModel.fromJson(Map<String, dynamic> json) {
    return CartItemModel(
      id: json['cartItemId'] as String,
      productId: json['productId'] as String,
      productName: json['productName'] as String,
      productSlug: json['productSlug'] as String,
      variantId: json['productVariantId'] as String,
      variantLabel: json['variantLabel'] as String?,
      imageUrl: json['thumbnailUrl'] as String?,
      unitPrice: json['effectivePrice'] as num,
      quantity: json['quantity'] as int,
      shopId: json['shopId'] as String,
      shopName: json['shopName'] as String,
      available: json['available'] as bool? ?? true,
    );
  }
}
