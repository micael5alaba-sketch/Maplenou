import 'dart:math';

import '../models/delivery_info_model.dart';
import '../models/product_detail_model.dart';
import '../models/product_model.dart';
import '../models/product_spec_entry.dart';
import '../models/product_variant_model.dart';
import '../models/review_model.dart';
import 'delivery_pricing_service.dart';
import 'product_service.dart';
import 'review_service.dart';

/// Assemble la fiche produit affichée par [ProductDetailsScreen] : variantes,
/// images, description et spécifications viennent du vrai backend
/// (`GET /api/products/{slug}`, `ProductService.getProductDetail`) ; avis
/// réels aussi (`ReviewService`). Le frais de livraison vient du palier le
/// moins cher du barème (`GET /api/delivery-pricing-tiers`) — aucune adresse
/// n'est encore choisie à ce stade du parcours. Seuls les délais textuels, la
/// politique de retour et les badges (`Bio`/`Artisanal`/...) restent
/// décoratifs — le backend n'a pas de champ équivalent et ce n'est pas
/// bloquant pour l'achat.
class ProductDetailService {
  final _productService = ProductService();
  final _reviewService = ReviewService();
  final _deliveryPricingService = DeliveryPricingService();

  // Utilisé uniquement si /api/delivery-pricing-tiers est injoignable.
  static const num _fallbackDeliveryFee = 1500;

  Future<ProductDetailModel> buildDetail(
    ProductModel product, {
    List<ProductModel> catalog = const [],
  }) async {
    final detailJson = await _productService.getProductDetail(product.slug);

    final images = (detailJson['images'] as List<dynamic>? ?? [])
        .map((json) => (json as Map<String, dynamic>)['url'] as String)
        .toList();
    final variants = (detailJson['variants'] as List<dynamic>? ?? [])
        .map((json) => ProductVariantModel.fromJson(json as Map<String, dynamic>))
        .toList();
    final specifications = (detailJson['specifications'] as List<dynamic>? ?? [])
        .map((json) => ProductSpecEntry(
              label: (json as Map<String, dynamic>)['label'] as String,
              value: json['value'] as String,
            ))
        .toList();

    List<ReviewModel> reviews;
    try {
      reviews = await _reviewService.getProductReviews(product.id);
    } catch (_) {
      // La fiche produit doit s'afficher même si les avis échouent à charger.
      reviews = const [];
    }

    final related = catalog.where((p) => p.id != product.id).take(6).toList();

    num deliveryFee = _fallbackDeliveryFee;
    try {
      final cheapest = await _deliveryPricingService.getCheapestPrice();
      if (cheapest != null) deliveryFee = cheapest;
    } catch (_) {
      // Barème de livraison injoignable : on garde le repli plutôt que de
      // bloquer l'affichage de la fiche produit.
    }

    return ProductDetailModel(
      id: detailJson['id'] as String,
      name: detailJson['name'] as String,
      shopId: detailJson['shopId'] as String,
      shopName: detailJson['shopName'] as String? ?? 'Boutique',
      images: images.isEmpty && product.imageUrl != null ? [product.imageUrl!] : images,
      badges: _mockBadges(product),
      price: (detailJson['basePrice'] as num),
      oldPrice: detailJson['oldPrice'] as num?,
      rating: product.rating,
      reviewCount: product.reviewCount,
      variants: variants,
      description: detailJson['description'] as String? ?? '',
      specifications: specifications,
      delivery: DeliveryInfoModel(
        standardDelay: '3 à 5 jours ouvrés',
        expressDelay: '24h (Grand Lomé uniquement)',
        deliveryFee: deliveryFee,
        returnPolicy: 'Retour gratuit sous 30 jours après livraison',
      ),
      reviews: reviews,
      relatedProducts: related,
    );
  }

  /// Deterministic pseudo-random pick (seeded on the product id) so the
  /// same product always shows the same badges — there's no backend field
  /// driving this yet.
  List<String> _mockBadges(ProductModel product) {
    final pool = ['Bio', 'Artisanal', 'Nouveau', 'Promotion'];
    final rnd = Random(product.id.hashCode);
    final count = 1 + rnd.nextInt(2);
    return (pool..shuffle(rnd)).take(count).toList();
  }
}
