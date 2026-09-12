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

/// One product in the seller's catalog. Generic enough to represent any
/// product type in the marketplace (vêtements, chaussures, téléphones,
/// bijoux, cosmétiques, accessoires, maison, électronique, alimentation...).
class VendorProductModel {
  static const int lowStockThreshold = 5;

  final String id;
  final String name;
  final String description;
  final num price;
  final int stock;
  final String? imageUrl;

  const VendorProductModel({
    required this.id,
    required this.name,
    required this.description,
    required this.price,
    required this.stock,
    this.imageUrl,
  });

  /// Derived from [stock] rather than stored separately, so the count and
  /// the badge can never disagree with each other.
  ProductStockStatus get status {
    if (stock <= 0) return ProductStockStatus.outOfStock;
    if (stock <= lowStockThreshold) return ProductStockStatus.lowStock;
    return ProductStockStatus.inStock;
  }
}
