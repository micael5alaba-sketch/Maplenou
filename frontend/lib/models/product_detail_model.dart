import 'delivery_info_model.dart';
import 'product_model.dart';
import 'product_spec_entry.dart';
import 'product_variant_model.dart';
import 'review_model.dart';
import 'variant_group_model.dart';

/// Full data behind [ProductDetailsScreen] — generic enough to represent
/// any product type in the marketplace (mode, électronique, beauté,
/// maison, alimentaire...), whatever fields it does or doesn't use.
class ProductDetailModel {
  final String id;
  final String name;
  final String shopId;
  final String shopName;
  final String shopSlug;
  final List<String> images;
  final List<String> badges;
  final num price;
  final num? oldPrice;
  final double? rating;
  final int? reviewCount;

  /// Variantes réelles du produit (`VariantResponse` — voir
  /// `catalog/dto/VariantResponse.java`) : c'est leur `id` que le panier
  /// référence, pas celui du produit.
  final List<ProductVariantModel> variants;

  final String description;
  final List<ProductSpecEntry> specifications;
  final DeliveryInfoModel delivery;
  final List<ReviewModel> reviews;
  final List<ProductModel> relatedProducts;

  const ProductDetailModel({
    required this.id,
    required this.name,
    required this.shopId,
    required this.shopName,
    required this.shopSlug,
    required this.images,
    this.badges = const [],
    required this.price,
    this.oldPrice,
    this.rating,
    this.reviewCount,
    this.variants = const [],
    required this.description,
    this.specifications = const [],
    required this.delivery,
    this.reviews = const [],
    this.relatedProducts = const [],
  });

  int? get discountPercent {
    final old = oldPrice;
    if (old == null || old <= price) return null;
    return (((old - price) / old) * 100).round();
  }

  /// Un produit sans variante définie par le vendeur reçoit toujours au
  /// moins une variante par défaut côté backend (le stock vit sur
  /// `ProductVariant`, jamais sur `Product`) — cette liste n'est donc vide
  /// que si le produit n'a pas encore été chargé.
  bool get hasVariants => variants.isNotEmpty;

  /// Un seul groupe de sélection ("Choix") listant chaque variante par son
  /// libellé réel — réutilise [VariantOptionSelector] tel quel plutôt que
  /// d'introduire un nouveau widget, le backend ne décomposant pas les
  /// variantes en attributs (couleur/taille) séparés.
  List<VariantGroupModel> get variantGroups {
    if (variants.length <= 1) return const [];
    return [VariantGroupModel(label: 'Choix', options: variants.map((v) => v.label).toList())];
  }

  ProductVariantModel? variantByLabel(String label) {
    for (final variant in variants) {
      if (variant.label == label) return variant;
    }
    return null;
  }
}
