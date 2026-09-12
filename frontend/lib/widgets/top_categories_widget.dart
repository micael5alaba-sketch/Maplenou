import 'package:flutter/material.dart';

import '../models/seller_dashboard_model.dart';
import '../theme/app_color_scheme.dart';

/// "Catégories les plus vendues" card: each row shows the category name,
/// its product count, the percentage of sales it represents, and a
/// progress bar for that percentage.
class TopCategoriesWidget extends StatelessWidget {
  final List<TopCategoryModel> categories;

  const TopCategoriesWidget({super.key, required this.categories});

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
            'Catégories les plus vendues',
            style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: colors.textDark),
          ),
          const SizedBox(height: 16),
          for (var i = 0; i < categories.length; i++) ...[
            _buildRow(colors, categories[i]),
            if (i != categories.length - 1) const SizedBox(height: 16),
          ],
        ],
      ),
    );
  }

  Widget _buildRow(AppColorScheme colors, TopCategoryModel category) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          crossAxisAlignment: CrossAxisAlignment.end,
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    category.name,
                    style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    '${category.productCount} produits',
                    style: TextStyle(fontSize: 12, color: colors.textMuted),
                  ),
                ],
              ),
            ),
            Text(
              '${category.percent.toStringAsFixed(0)}%',
              style: TextStyle(fontWeight: FontWeight.bold, color: colors.primary),
            ),
          ],
        ),
        const SizedBox(height: 8),
        ClipRRect(
          borderRadius: BorderRadius.circular(8),
          child: LinearProgressIndicator(
            value: (category.percent / 100).clamp(0, 1),
            minHeight: 8,
            backgroundColor: colors.inputFill,
            valueColor: AlwaysStoppedAnimation(colors.primary),
          ),
        ),
      ],
    );
  }
}
