import 'package:flutter/material.dart';

import '../models/product_detail_model.dart';
import '../models/product_model.dart';
import '../services/product_detail_service.dart';
import '../theme/app_color_scheme.dart';
import '../utils/formatters.dart';
import '../widgets/badge_chip.dart';
import '../widgets/delivery_info_section.dart';
import '../widgets/expandable_text.dart';
import '../widgets/primary_button.dart';
import '../widgets/product_image_gallery.dart';
import '../widgets/product_review_card.dart';
import '../widgets/product_specification.dart';
import '../widgets/quantity_selector.dart';
import '../widgets/rating_summary.dart';
import '../widgets/related_products_carousel.dart';
import '../widgets/variant_option_selector.dart';

/// Generic product detail page — one single layout reused for every
/// product type in the marketplace (mode, électronique, beauté, maison,
/// alimentaire...). Takes a real [ProductModel] from the catalog; anything
/// the backend doesn't expose yet (galerie étendue, avis, spécifications,
/// livraison, produits similaires) is filled in by [ProductDetailService]
/// with mock data — see that file's doc comment.
class ProductDetailsScreen extends StatefulWidget {
  final ProductModel product;

  /// Other products to draw "Vous pourriez aussi aimer" from (typically
  /// whatever list the caller already had loaded — home grid, category
  /// results...).
  final List<ProductModel> relatedCatalog;

  const ProductDetailsScreen({
    super.key,
    required this.product,
    this.relatedCatalog = const [],
  });

  @override
  State<ProductDetailsScreen> createState() => _ProductDetailsScreenState();
}

class _ProductDetailsScreenState extends State<ProductDetailsScreen> {
  late final ProductDetailModel _detail;

  final Map<String, String> _selectedOptions = {};
  int _quantity = 1;
  bool _isFavorite = false;
  bool _allReviewsVisible = false;

  @override
  void initState() {
    super.initState();
    _detail = ProductDetailService().buildMockDetail(
      widget.product,
      catalog: widget.relatedCatalog,
    );
    for (final group in _detail.variantGroups) {
      if (group.options.isNotEmpty) {
        _selectedOptions[group.label] = group.options.first;
      }
    }
  }

  void _showComingSoon(String feature) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text('$feature bientôt disponible.')));
  }

  void _openProduct(ProductModel product) {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => ProductDetailsScreen(
          product: product,
          relatedCatalog: widget.relatedCatalog,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final detail = _detail;
    final colors = context.colors;

    return Scaffold(
      backgroundColor: colors.surface,
      appBar: AppBar(
        backgroundColor: colors.surface,
        elevation: 0,
        surfaceTintColor: colors.surface,
        iconTheme: IconThemeData(color: colors.textDark),
        actions: [
          IconButton(
            icon: const Icon(Icons.share_outlined),
            onPressed: () => _showComingSoon('Le partage'),
          ),
          IconButton(
            icon: Icon(
              _isFavorite ? Icons.favorite_rounded : Icons.favorite_border_rounded,
              color: _isFavorite ? colors.error : colors.textDark,
            ),
            onPressed: () => setState(() => _isFavorite = !_isFavorite),
          ),
          const SizedBox(width: 4),
        ],
      ),
      body: SafeArea(
        top: false,
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              ProductImageGallery(images: detail.images),
              const SizedBox(height: 16),
              if (detail.badges.isNotEmpty) ...[
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: detail.badges.map((label) => BadgeChip(label: label)).toList(),
                ),
                const SizedBox(height: 14),
              ],
              Text(
                detail.name,
                style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: colors.textDark),
              ),
              const SizedBox(height: 8),
              RatingSummary(rating: detail.rating ?? 0, reviewCount: detail.reviewCount ?? 0),
              const SizedBox(height: 12),
              _buildPriceRow(detail),
              const SizedBox(height: 22),
              for (final group in detail.variantGroups) ...[
                VariantOptionSelector(
                  group: group,
                  selectedOption: _selectedOptions[group.label],
                  onSelected: (option) => setState(() => _selectedOptions[group.label] = option),
                ),
                const SizedBox(height: 20),
              ],
              _buildSectionTitle('Quantité'),
              const SizedBox(height: 10),
              QuantitySelector(
                quantity: _quantity,
                onChanged: (value) => setState(() => _quantity = value),
              ),
              const SizedBox(height: 26),
              _buildSectionTitle('Description'),
              const SizedBox(height: 10),
              ExpandableText(text: detail.description),
              const SizedBox(height: 26),
              _buildSectionTitle('Spécifications'),
              const SizedBox(height: 6),
              Column(
                children: detail.specifications
                    .map((entry) => ProductSpecification(entry: entry))
                    .toList(),
              ),
              const SizedBox(height: 26),
              _buildSectionTitle('Livraison et retours'),
              const SizedBox(height: 6),
              DeliveryInfoSection(info: detail.delivery),
              const SizedBox(height: 26),
              _buildReviewsSection(detail),
              const SizedBox(height: 26),
              if (detail.relatedProducts.isNotEmpty) ...[
                _buildSectionTitle('Vous pourriez aussi aimer'),
                const SizedBox(height: 12),
                RelatedProductsCarousel(
                  products: detail.relatedProducts,
                  onProductTap: _openProduct,
                ),
              ],
            ],
          ),
        ),
      ),
      bottomNavigationBar: _buildBottomActionBar(),
    );
  }

  Widget _buildSectionTitle(String title) {
    return Text(
      title,
      style: TextStyle(fontSize: 17, fontWeight: FontWeight.bold, color: context.colors.textDark),
    );
  }

  Widget _buildPriceRow(ProductDetailModel detail) {
    final colors = context.colors;
    final discount = detail.discountPercent;
    return Row(
      crossAxisAlignment: CrossAxisAlignment.end,
      children: [
        Text(
          formatFcfa(detail.price),
          style: TextStyle(fontSize: 26, fontWeight: FontWeight.bold, color: colors.primary),
        ),
        if (detail.oldPrice != null) ...[
          const SizedBox(width: 10),
          Padding(
            padding: const EdgeInsets.only(bottom: 4),
            child: Text(
              formatFcfa(detail.oldPrice!),
              style: TextStyle(
                fontSize: 15,
                color: colors.textMuted,
                decoration: TextDecoration.lineThrough,
              ),
            ),
          ),
        ],
        if (discount != null) ...[
          const SizedBox(width: 10),
          Padding(
            padding: const EdgeInsets.only(bottom: 4),
            child: BadgeChip(label: '-$discount%'),
          ),
        ],
      ],
    );
  }

  Widget _buildReviewsSection(ProductDetailModel detail) {
    final colors = context.colors;
    final reviews = detail.reviews;
    final visibleReviews = _allReviewsVisible ? reviews : reviews.take(2).toList();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _buildSectionTitle('Avis clients'),
        const SizedBox(height: 10),
        if (reviews.isEmpty)
          Text('Aucun avis pour le moment.', style: TextStyle(color: colors.textMuted))
        else ...[
          for (final review in visibleReviews) ...[
            ProductReviewCard(review: review),
            Divider(height: 1, color: colors.border),
          ],
          if (reviews.length > 2)
            Padding(
              padding: const EdgeInsets.only(top: 8),
              child: GestureDetector(
                onTap: () => setState(() => _allReviewsVisible = !_allReviewsVisible),
                child: Text(
                  _allReviewsVisible ? 'Réduire' : 'Voir tout (${reviews.length} avis)',
                  style: TextStyle(color: colors.primary, fontWeight: FontWeight.w600),
                ),
              ),
            ),
        ],
      ],
    );
  }

  Widget _buildBottomActionBar() {
    final colors = context.colors;

    return SafeArea(
      child: Container(
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
        decoration: BoxDecoration(
          color: colors.surface,
          border: Border(top: BorderSide(color: colors.border)),
        ),
        child: Row(
          children: [
            Expanded(
              child: SizedBox(
                height: 54,
                child: OutlinedButton.icon(
                  onPressed: () => _showComingSoon('L\'ajout au panier'),
                  style: OutlinedButton.styleFrom(
                    foregroundColor: colors.primary,
                    side: BorderSide(color: colors.primary, width: 1.4),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(30)),
                  ),
                  icon: const Icon(Icons.shopping_cart_outlined),
                  label: const Text('Ajouter au panier', style: TextStyle(fontWeight: FontWeight.w600)),
                ),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: PrimaryButton(
                label: 'Acheter maintenant',
                backgroundColor: colors.accentOrange,
                onPressed: () => _showComingSoon('L\'achat immédiat'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
