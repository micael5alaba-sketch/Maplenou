import 'package:flutter/material.dart';

import '../models/vendor_product_model.dart';
import '../theme/app_color_scheme.dart';

/// Colored stock-status badge overlaid on a product photo ("EN STOCK",
/// "STOCK FAIBLE", "RUPTURE"). Semantic traffic-light colors, recognizable
/// regardless of the app's light/dark theme.
class ProductStatusBadge extends StatelessWidget {
  final ProductStockStatus status;

  const ProductStatusBadge({super.key, required this.status});

  Color _color(BuildContext context) {
    final colors = context.colors;
    switch (status) {
      case ProductStockStatus.inStock:
        return colors.primary;
      case ProductStockStatus.lowStock:
        return colors.accentOrange;
      case ProductStockStatus.outOfStock:
        return colors.error;
    }
  }

  @override
  Widget build(BuildContext context) {
    final color = _color(context);

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
      decoration: BoxDecoration(color: color, borderRadius: BorderRadius.circular(8)),
      child: Text(
        status.badgeLabel,
        style: const TextStyle(color: Colors.white, fontSize: 11, fontWeight: FontWeight.bold),
      ),
    );
  }
}
