import 'package:flutter/material.dart';

import '../models/product_spec_entry.dart';
import '../theme/app_color_scheme.dart';

/// One row of the "Spécifications" list — a generic label/value pair that
/// adapts to any product type (Marque, Matière, Pointure, Garantie...).
class ProductSpecification extends StatelessWidget {
  final ProductSpecEntry entry;

  const ProductSpecification({super.key, required this.entry});

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            flex: 2,
            child: Text(entry.label, style: TextStyle(color: colors.textMuted)),
          ),
          Expanded(
            flex: 3,
            child: Text(
              entry.value,
              style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark),
            ),
          ),
        ],
      ),
    );
  }
}
