import 'package:flutter/material.dart';

import '../models/seller_dashboard_model.dart';
import '../theme/app_color_scheme.dart';

/// "Produit le plus vendu" card: thumbnail on the left, name and units
/// sold on the right.
class BestSellingProductWidget extends StatelessWidget {
  final BestSellingProductModel product;

  const BestSellingProductWidget({super.key, required this.product});

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 12, offset: const Offset(0, 4)),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Produit le plus vendu',
            style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: colors.textDark),
          ),
          const SizedBox(height: 14),
          Row(
            children: [
              ClipRRect(
                borderRadius: BorderRadius.circular(14),
                child: SizedBox(
                  width: 64,
                  height: 64,
                  child: product.imageUrl != null
                      ? Image.network(
                          product.imageUrl!,
                          fit: BoxFit.cover,
                          errorBuilder: (context, error, stackTrace) => _buildPlaceholder(colors),
                        )
                      : _buildPlaceholder(colors),
                ),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      product.name,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark),
                    ),
                    const SizedBox(height: 6),
                    Text(
                      '${product.unitsSold} unités vendues',
                      style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: colors.primary),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildPlaceholder(AppColorScheme colors) {
    return Container(
      color: colors.inputFill,
      alignment: Alignment.center,
      child: Icon(Icons.inventory_2_outlined, color: colors.textMuted),
    );
  }
}
