import 'package:flutter/material.dart';

import '../models/product_detail_model.dart';
import '../models/product_model.dart';
import '../services/api_client.dart';
import '../services/cart_service.dart';
import '../services/favorites_service.dart';
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
import 'cart_screen.dart';
import 'checkout_screen.dart';

/// Generic product detail page — one single layout reused for every
/// product type in the marketplace (mode, électronique, beauté, maison,
/// alimentaire...). Takes a real [ProductModel] from the catalog; variantes,
/// images, description et avis viennent du vrai backend
/// ([ProductDetailService]) — seules la fiche "Livraison et retours" et les
/// badges restent décoratifs (voir ce service).
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
  late Future<ProductDetailModel> _detailFuture;

  final Map<String, String> _selectedOptions = {};
  int _quantity = 1;
  bool? _isFavorite;
  bool _isAddingToCart = false;
  bool _allReviewsVisible = false;

  @override
  void initState() {
    super.initState();
    _detailFuture = ProductDetailService()
        .buildDetail(widget.product, catalog: widget.relatedCatalog)
        .then((detail) {
      for (final group in detail.variantGroups) {
        if (group.options.isNotEmpty) {
          _selectedOptions[group.label] = group.options.first;
        }
      }
      return detail;
    });
    FavoritesService().isFavorite(widget.product.id).then((value) {
      if (mounted) setState(() => _isFavorite = value);
    });
  }

  void _showComingSoon(String feature) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text('$feature bientôt disponible.')));
  }

  void _showMessage(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(message)));
  }

  Future<void> _toggleFavorite() async {
    final previous = _isFavorite ?? false;
    setState(() => _isFavorite = !previous);
    try {
      final result = await FavoritesService().toggle(widget.product.id);
      if (mounted) setState(() => _isFavorite = result);
    } on ApiException catch (e) {
      if (mounted) setState(() => _isFavorite = previous);
      _showMessage(e.message);
    }
  }

  /// Résout la variante réelle correspondant à la sélection courante — sans
  /// groupe de choix (un seul modèle), c'est simplement l'unique variante.
  String? _resolveSelectedVariantId(ProductDetailModel detail) {
    if (!detail.hasVariants) return null;
    if (detail.variants.length == 1) return detail.variants.first.id;
    final selectedLabel = _selectedOptions['Choix'];
    if (selectedLabel == null) return null;
    return detail.variantByLabel(selectedLabel)?.id;
  }

  Future<void> _addToCart(ProductDetailModel detail, {bool showConfirmation = true}) async {
    final variantId = _resolveSelectedVariantId(detail);
    if (variantId == null) {
      _showMessage('Ce produit n\'est plus disponible à la vente.');
      return;
    }

    setState(() => _isAddingToCart = true);
    try {
      await CartService().addItem(productVariantId: variantId, quantity: _quantity);
      if (!showConfirmation || !mounted) return;
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(SnackBar(
          content: Text('"${widget.product.name}" ajouté au panier.'),
          action: SnackBarAction(
            label: 'Voir',
            onPressed: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const CartScreen())),
          ),
        ));
    } on ApiException catch (e) {
      _showMessage(e.message);
    } finally {
      if (mounted) setState(() => _isAddingToCart = false);
    }
  }

  Future<void> _buyNow(ProductDetailModel detail) async {
    await _addToCart(detail, showConfirmation: false);
    if (!mounted) return;
    Navigator.of(context).push(MaterialPageRoute(builder: (_) => const CheckoutScreen()));
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
              _isFavorite == true ? Icons.favorite_rounded : Icons.favorite_border_rounded,
              color: _isFavorite == true ? colors.error : colors.textDark,
            ),
            onPressed: _isFavorite == null ? null : _toggleFavorite,
          ),
          const SizedBox(width: 4),
        ],
      ),
      body: SafeArea(
        top: false,
        child: FutureBuilder<ProductDetailModel>(
          future: _detailFuture,
          builder: (context, snapshot) {
            if (snapshot.connectionState == ConnectionState.waiting) {
              return const Center(child: CircularProgressIndicator());
            }
            if (snapshot.hasError || !snapshot.hasData) {
              return _buildErrorState(colors, snapshot.error);
            }
            return _buildContent(colors, snapshot.data!);
          },
        ),
      ),
      bottomNavigationBar: FutureBuilder<ProductDetailModel>(
        future: _detailFuture,
        builder: (context, snapshot) {
          if (!snapshot.hasData) return const SizedBox.shrink();
          return _buildBottomActionBar(snapshot.data!);
        },
      ),
    );
  }

  Widget _buildErrorState(AppColorScheme colors, Object? error) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.error_outline_rounded, size: 48, color: colors.error),
            const SizedBox(height: 12),
            Text(
              error is ApiException ? error.message : 'Impossible de charger ce produit.',
              textAlign: TextAlign.center,
              style: TextStyle(color: colors.textMuted),
            ),
            const SizedBox(height: 12),
            TextButton(
              onPressed: () => setState(() {
                _detailFuture = ProductDetailService()
                    .buildDetail(widget.product, catalog: widget.relatedCatalog);
              }),
              child: const Text('Réessayer'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildContent(AppColorScheme colors, ProductDetailModel detail) {
    return SingleChildScrollView(
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

  Widget _buildBottomActionBar(ProductDetailModel detail) {
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
                  onPressed: _isAddingToCart ? null : () => _addToCart(detail),
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
                isLoading: _isAddingToCart,
                onPressed: () => _buyNow(detail),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
