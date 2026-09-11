/// Promotional badge shown on a [ProductModel] card, if any.
///
/// [ProductBadgeType.isNew] has no backend signal yet (no "new" flag on
/// `ProductSummaryResponse`) and always resolves to [ProductBadgeType.none].
/// [ProductBadgeType.discount] is derived from [ProductModel.oldPrice] — see
/// its doc comment for the backend field this needs.
enum ProductBadgeType { none, discount, isNew }

/// A marketplace product, as returned by `GET /api/products`
/// (`ProductSummaryResponse` on the backend).
class ProductModel {
  final String id;
  final String name;
  final String slug;

  /// First product image (Cloudinary URL), or null if the product has none
  /// yet — [ProductCard] shows a placeholder in that case.
  final String? imageUrl;

  /// Width / height ratio used to size the "Produits populaires" staggered
  /// grid. The backend doesn't provide this, so every card uses the same
  /// ratio for now instead of a fabricated per-product value.
  final double imageAspectRatio;

  final num price;

  /// Prix barré avant réduction, si le produit est en promo.
  ///
  /// Correspond à un champ `oldPrice` que le backend n'expose pas encore
  /// sur `ProductSummaryResponse`/`ProductDetailResponse` (à ajouter côté
  /// `Product` : un `BigDecimal` nullable, prix affiché barré au-dessus du
  /// prix courant quand il est renseigné et supérieur à `basePrice`). Reste
  /// `null` tant que ce n'est pas fait — pas de promo affichée par défaut.
  final num? oldPrice;

  /// From `GET /api/products/{id}/reviews/summary` — null until that call
  /// has been made for this product (not fetched in bulk for a list yet).
  final double? rating;
  final int? reviewCount;

  const ProductModel({
    required this.id,
    required this.name,
    required this.slug,
    this.imageUrl,
    this.imageAspectRatio = 1.2,
    required this.price,
    this.oldPrice,
    this.rating,
    this.reviewCount,
  });

  /// Pourcentage de réduction, calculé côté client à partir de [oldPrice] et
  /// [price] — pas besoin que le backend renvoie un pourcentage séparé.
  int? get discountPercent {
    final old = oldPrice;
    if (old == null || old <= price) return null;
    return (((old - price) / old) * 100).round();
  }

  ProductBadgeType get badgeType =>
      discountPercent != null ? ProductBadgeType.discount : ProductBadgeType.none;

  factory ProductModel.fromSummaryJson(Map<String, dynamic> json) {
    return ProductModel(
      id: json['id'] as String,
      name: json['name'] as String,
      slug: json['slug'] as String,
      imageUrl: json['thumbnailUrl'] as String?,
      price: json['basePrice'] as num,
      oldPrice: json['oldPrice'] as num?,
    );
  }
}
