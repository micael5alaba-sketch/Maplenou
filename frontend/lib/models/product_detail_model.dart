import 'delivery_info_model.dart';
import 'product_model.dart';
import 'product_spec_entry.dart';
import 'review_model.dart';
import 'variant_group_model.dart';

/// Full data behind [ProductDetailsScreen] — generic enough to represent
/// any product type in the marketplace (mode, électronique, beauté,
/// maison, alimentaire...), whatever fields it does or doesn't use.
class ProductDetailModel {
  final String id;
  final String name;
  final List<String> images;
  final List<String> badges;
  final num price;
  final num? oldPrice;
  final double? rating;
  final int? reviewCount;
  final List<VariantGroupModel> variantGroups;
  final String description;
  final List<ProductSpecEntry> specifications;
  final DeliveryInfoModel delivery;
  final List<ReviewModel> reviews;
  final List<ProductModel> relatedProducts;

  const ProductDetailModel({
    required this.id,
    required this.name,
    required this.images,
    this.badges = const [],
    required this.price,
    this.oldPrice,
    this.rating,
    this.reviewCount,
    this.variantGroups = const [],
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
}
