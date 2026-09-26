/// Stock state of a [VendorProductModel], derived from its stock count.
enum ProductStockStatus { inStock, lowStock, outOfStock }

extension ProductStockStatusLabels on ProductStockStatus {
  /// Shown on the badge overlaid on the product image.
  String get badgeLabel {
    switch (this) {
      case ProductStockStatus.inStock:
        return 'EN STOCK';
      case ProductStockStatus.lowStock:
        return 'STOCK FAIBLE';
      case ProductStockStatus.outOfStock:
        return 'RUPTURE';
    }
  }

  /// Shown on the filter chip.
  String get filterLabel {
    switch (this) {
      case ProductStockStatus.inStock:
        return 'En stock';
      case ProductStockStatus.lowStock:
        return 'Stock faible';
      case ProductStockStatus.outOfStock:
        return 'Rupture';
    }
  }
}

/// One product in the seller's catalog. Miroir de `SellerProductSummaryResponse`
/// (`catalog/dto/SellerProductSummaryResponse.java`) — [stock] est la somme
/// du stock de toutes les variantes du produit (`totalStock`).
class VendorProductModel {
  static const int lowStockThreshold = 5;

  final String id;
  final String slug;
  final String name;
  final num price;
  final int stock;
  final String? imageUrl;
  final String status;

  const VendorProductModel({
    required this.id,
    required this.slug,
    required this.name,
    required this.price,
    required this.stock,
    this.imageUrl,
    required this.status,
  });

  /// Derived from [stock] rather than stored separately, so the count and
  /// the badge can never disagree with each other.
  ProductStockStatus get stockStatus {
    if (stock <= 0) return ProductStockStatus.outOfStock;
    if (stock <= lowStockThreshold) return ProductStockStatus.lowStock;
    return ProductStockStatus.inStock;
  }

  factory VendorProductModel.fromJson(Map<String, dynamic> json) {
    return VendorProductModel(
      id: json['id'] as String,
      slug: json['slug'] as String,
      name: json['name'] as String,
      price: json['basePrice'] as num,
      stock: json['totalStock'] as int,
      imageUrl: json['thumbnailUrl'] as String?,
      status: json['status'] as String,
    );
  }
}
