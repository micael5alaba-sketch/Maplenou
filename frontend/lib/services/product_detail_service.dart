import 'dart:math';

import '../models/delivery_info_model.dart';
import '../models/product_detail_model.dart';
import '../models/product_model.dart';
import '../models/product_spec_entry.dart';
import '../models/review_model.dart';
import '../models/variant_group_model.dart';

/// Builds the extra data a product detail page needs (galerie, avis,
/// spécifications, livraison, produits similaires...) around a real
/// [ProductModel] coming from the catalog.
///
/// Entirely mocked for now — the backend doesn't expose most of this yet
/// (voir DEMANDES_MODIFICATIONS_BACKEND.md). Kept isolated in this one
/// service so swapping it for real endpoints later only touches this file,
/// not [ProductDetailsScreen] or its widgets.
class ProductDetailService {
  ProductDetailModel buildMockDetail(ProductModel product, {List<ProductModel> catalog = const []}) {
    final images = [
      if (product.imageUrl != null) product.imageUrl!,
      ...List.generate(3, (i) => 'https://picsum.photos/seed/${product.slug}-$i/800/800'),
    ];

    final related = catalog.where((p) => p.id != product.id).take(6).toList();

    return ProductDetailModel(
      id: product.id,
      name: product.name,
      images: images,
      badges: _mockBadges(product),
      price: product.price,
      oldPrice: product.oldPrice,
      rating: product.rating ?? 4.6,
      reviewCount: product.reviewCount ?? 32,
      variantGroups: _mockVariantGroups(product),
      description:
          "Ce produit est fabriqué avec soin par des artisans locaux. Idéal pour un usage "
          "quotidien, il allie qualité, durabilité et style. Chaque pièce est unique et "
          "reflète le savoir-faire traditionnel togolais, avec une attention particulière "
          "portée aux finitions et aux matériaux utilisés tout au long du processus de "
          "fabrication. Un produit pensé pour durer, à la fois fonctionnel et élégant.",
      specifications: _mockSpecifications(),
      delivery: const DeliveryInfoModel(
        standardDelay: '3 à 5 jours ouvrés',
        expressDelay: '24h (Grand Lomé uniquement)',
        deliveryFee: 1500,
        returnPolicy: 'Retour gratuit sous 30 jours après livraison',
      ),
      reviews: _mockReviews(),
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

  /// Picks a plausible variant group from the product name — purely
  /// cosmetic, so the generic detail page still feels adapted to the
  /// product type even without a real backend signal for it.
  List<VariantGroupModel> _mockVariantGroups(ProductModel product) {
    final name = product.name.toLowerCase();
    if (name.contains('sandale') || name.contains('sac') || name.contains('vêtement')) {
      return const [
        VariantGroupModel(label: 'Taille', options: ['S', 'M', 'L', 'XL']),
      ];
    }
    if (name.contains('huile') || name.contains('parfum')) {
      return const [
        VariantGroupModel(label: 'Format', options: ['10 ml', '30 ml', '50 ml']),
      ];
    }
    return const [
      VariantGroupModel(label: 'Couleur', options: ['Noir', 'Blanc', 'Bleu']),
    ];
  }

  List<ProductSpecEntry> _mockSpecifications() {
    return const [
      ProductSpecEntry(label: 'Marque', value: 'Maplenou Artisanat'),
      ProductSpecEntry(label: 'Matière', value: 'Naturelle'),
      ProductSpecEntry(label: 'Origine', value: 'Togo'),
      ProductSpecEntry(label: 'Utilisation', value: 'Usage quotidien'),
      ProductSpecEntry(label: 'Garantie', value: 'Satisfait ou remboursé 30 jours'),
    ];
  }

  List<ReviewModel> _mockReviews() {
    return [
      ReviewModel(
        customerName: 'Akofa M.',
        rating: 5,
        comment: 'Très bonne qualité, exactement comme sur les photos. Livraison rapide !',
        date: DateTime.now().subtract(const Duration(days: 6)),
      ),
      ReviewModel(
        customerName: 'Kodjo A.',
        rating: 4,
        comment: 'Beau produit, artisanat local vraiment soigné. Je recommande.',
        date: DateTime.now().subtract(const Duration(days: 21)),
      ),
      ReviewModel(
        customerName: 'Sena D.',
        rating: 5,
        comment: "Conforme à la description, vendeur réactif. J'en recommande d'autres.",
        date: DateTime.now().subtract(const Duration(days: 40)),
      ),
    ];
  }
}
