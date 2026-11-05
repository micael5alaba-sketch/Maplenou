/// One purchasable variant of a product. Miroir de `VariantResponse`
/// (`catalog/dto/VariantResponse.java`) — c'est cet `id` (pas l'id du
/// produit) que le panier référence (`AddToCartRequest.productVariantId`).
class ProductVariantModel {
  final String id;
  final String label;
  final num? priceOverride;

  /// Prix paye par l'acheteur pour cette variante (commission incluse) ; null avec un ancien backend.
  final num? displayPrice;
  final int stockQuantity;
  final String? sku;

  const ProductVariantModel({
    required this.id,
    required this.label,
    this.priceOverride,
    this.displayPrice,
    required this.stockQuantity,
    this.sku,
  });

  bool get inStock => stockQuantity > 0;

  factory ProductVariantModel.fromJson(Map<String, dynamic> json) {
    return ProductVariantModel(
      id: json['id'] as String,
      label: json['label'] as String,
      priceOverride: json['priceOverride'] as num?,
      displayPrice: json['displayPrice'] as num?,
      stockQuantity: json['stockQuantity'] as int? ?? 0,
      sku: json['sku'] as String?,
    );
  }
}
