import 'package:flutter/material.dart';

import '../models/vendor_product_model.dart';
import '../theme/app_color_scheme.dart';
import '../utils/formatters.dart';
import 'product_status_badge.dart';

/// One product row in [VendorCatalogScreen]'s list: photo with a stock
/// badge, name, short description, price, quantity, and a 3-dot action
/// menu (Modifier / Voir / Dupliquer / Supprimer).
///
/// Named `VendorProductCard` rather than `ProductCard` — that name is
/// already used for the buyer-facing catalog card, which has a different
/// layout and a different data model ([ProductModel] vs
/// [VendorProductModel]); reusing the name would just invite confusion.
class VendorProductCard extends StatelessWidget {
  final VendorProductModel product;
  final VoidCallback onEdit;
  final VoidCallback onView;
  final VoidCallback onDuplicate;
  final VoidCallback onDelete;

  const VendorProductCard({
    super.key,
    required this.product,
    required this.onEdit,
    required this.onView,
    required this.onDuplicate,
    required this.onDelete,
  });

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return Container(
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(18),
        boxShadow: [
          BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 12, offset: const Offset(0, 4)),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildImage(colors),
          Padding(
            padding: const EdgeInsets.all(14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(
                      child: Text(
                        product.name,
                        style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: colors.textDark),
                      ),
                    ),
                    _buildMenu(context),
                  ],
                ),
                const SizedBox(height: 4),
                Text(
                  product.description,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(fontSize: 13, color: colors.textMuted, height: 1.35),
                ),
                const SizedBox(height: 12),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      formatFcfa(product.price),
                      style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: colors.primary),
                    ),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                      decoration: BoxDecoration(
                        color: colors.inputFill,
                        borderRadius: BorderRadius.circular(20),
                      ),
                      child: Text(
                        'Qté: ${product.stock}',
                        style: TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: colors.textDark),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildImage(AppColorScheme colors) {
    return Stack(
      children: [
        ClipRRect(
          borderRadius: const BorderRadius.vertical(top: Radius.circular(18)),
          child: AspectRatio(
            aspectRatio: 16 / 10,
            child: product.imageUrl != null
                ? Image.network(
                    product.imageUrl!,
                    fit: BoxFit.cover,
                    errorBuilder: (context, error, stackTrace) => _buildImagePlaceholder(colors),
                  )
                : _buildImagePlaceholder(colors),
          ),
        ),
        Positioned(
          top: 10,
          left: 10,
          child: ProductStatusBadge(status: product.status),
        ),
      ],
    );
  }

  Widget _buildImagePlaceholder(AppColorScheme colors) {
    return Container(
      color: colors.inputFill,
      alignment: Alignment.center,
      child: Icon(Icons.image_not_supported_outlined, color: colors.textMuted, size: 32),
    );
  }

  Widget _buildMenu(BuildContext context) {
    return PopupMenuButton<VoidCallback>(
      icon: Icon(Icons.more_vert_rounded, color: context.colors.textMuted),
      onSelected: (action) => action(),
      itemBuilder: (context) => [
        PopupMenuItem(value: onEdit, child: const Text('Modifier')),
        PopupMenuItem(value: onView, child: const Text('Voir')),
        PopupMenuItem(value: onDuplicate, child: const Text('Dupliquer')),
        PopupMenuItem(
          value: onDelete,
          child: Text('Supprimer', style: TextStyle(color: context.colors.error)),
        ),
      ],
    );
  }
}
