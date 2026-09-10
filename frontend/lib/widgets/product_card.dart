import 'package:flutter/material.dart';

import '../models/product_model.dart';
import '../theme/app_colors.dart';
import '../utils/formatters.dart';

/// Product tile used in the "Produits populaires" grid: photo with a
/// promo/new badge and a favorite toggle, rating, name, price (with the
/// old price struck through when discounted) and a circular add-to-cart
/// button.
///
/// The favorite state is local UI state only (mocked) — wire it to a
/// cart/wishlist service once one exists.
class ProductCard extends StatefulWidget {
  final ProductModel product;
  final VoidCallback? onTap;
  final VoidCallback? onAddToCart;

  const ProductCard({
    super.key,
    required this.product,
    this.onTap,
    this.onAddToCart,
  });

  @override
  State<ProductCard> createState() => _ProductCardState();
}

class _ProductCardState extends State<ProductCard> {
  bool _isFavorite = false;

  void _toggleFavorite() => setState(() => _isFavorite = !_isFavorite);

  @override
  Widget build(BuildContext context) {
    final product = widget.product;

    return GestureDetector(
      onTap: widget.onTap,
      child: Container(
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(16),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: 0.06),
              blurRadius: 10,
              offset: const Offset(0, 4),
            ),
          ],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _buildImage(product),
            Padding(
              padding: const EdgeInsets.fromLTRB(10, 6, 10, 6),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (product.rating != null) ...[
                    _buildRating(product),
                    const SizedBox(height: 3),
                  ],
                  Text(
                    product.name,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontSize: 13,
                      fontWeight: FontWeight.w600,
                      color: AppColors.textDark,
                      height: 1.2,
                    ),
                  ),
                  const SizedBox(height: 6),
                  _buildPriceRow(product),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildImage(ProductModel product) {
    return Stack(
      children: [
        ClipRRect(
          borderRadius: const BorderRadius.vertical(top: Radius.circular(16)),
          child: AspectRatio(
            aspectRatio: product.imageAspectRatio,
            child: product.imageUrl != null
                ? Image.network(
                    product.imageUrl!,
                    fit: BoxFit.cover,
                    errorBuilder: (context, error, stackTrace) => _buildImagePlaceholder(),
                    loadingBuilder: (context, child, progress) =>
                        progress == null ? child : _buildImagePlaceholder(loading: true),
                  )
                : _buildImagePlaceholder(),
          ),
        ),
        if (product.badgeType != ProductBadgeType.none)
          Positioned(top: 8, left: 8, child: _ProductBadge(product: product)),
        Positioned(
          top: 8,
          right: 8,
          child: GestureDetector(
            onTap: _toggleFavorite,
            child: CircleAvatar(
              radius: 15,
              backgroundColor: Colors.white,
              child: Icon(
                _isFavorite ? Icons.favorite : Icons.favorite_border,
                size: 15,
                color: _isFavorite ? AppColors.error : AppColors.textMuted,
              ),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildImagePlaceholder({bool loading = false}) {
    return Container(
      color: AppColors.inputFill,
      alignment: Alignment.center,
      child: loading
          ? const SizedBox(
              width: 20,
              height: 20,
              child: CircularProgressIndicator(strokeWidth: 2, color: AppColors.primary),
            )
          : const Icon(Icons.image_not_supported_outlined, color: AppColors.textMuted),
    );
  }

  Widget _buildRating(ProductModel product) {
    return Row(
      children: [
        const Icon(Icons.star_rounded, size: 16, color: AppColors.accentOrange),
        const SizedBox(width: 3),
        Text(
          product.rating!.toStringAsFixed(1),
          style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: AppColors.textDark),
        ),
        const SizedBox(width: 3),
        Flexible(
          child: Text(
            '(${product.reviewCount ?? 0})',
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(fontSize: 12, color: AppColors.textMuted),
          ),
        ),
      ],
    );
  }

  Widget _buildPriceRow(ProductModel product) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                formatFcfa(product.price),
                style: const TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.bold,
                  color: AppColors.primary,
                ),
              ),
              if (product.oldPrice != null)
                Text(
                  formatFcfa(product.oldPrice!),
                  style: const TextStyle(
                    fontSize: 11,
                    color: AppColors.textMuted,
                    decoration: TextDecoration.lineThrough,
                  ),
                ),
            ],
          ),
        ),
        GestureDetector(
          onTap: widget.onAddToCart,
          child: const CircleAvatar(
            radius: 16,
            backgroundColor: AppColors.primary,
            child: Icon(Icons.shopping_cart_rounded, size: 16, color: Colors.white),
          ),
        ),
      ],
    );
  }
}

/// Small pill badge overlaid on the product photo ("-15%", "Nouveau"...).
class _ProductBadge extends StatelessWidget {
  final ProductModel product;

  const _ProductBadge({required this.product});

  @override
  Widget build(BuildContext context) {
    final isNew = product.badgeType == ProductBadgeType.isNew;
    final label = isNew ? 'Nouveau' : '-${product.discountPercent}%';
    final color = isNew ? AppColors.primary : AppColors.accentOrange;

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(color: color, borderRadius: BorderRadius.circular(8)),
      child: Text(
        label,
        style: const TextStyle(color: Colors.white, fontSize: 10, fontWeight: FontWeight.bold),
      ),
    );
  }
}
