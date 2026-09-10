import 'package:flutter/material.dart';

import '../models/category_model.dart';
import '../theme/app_color_scheme.dart';

/// Round category avatar with its name below, used in the horizontal
/// "Catégories" list on the home screen.
class CategoryWidget extends StatelessWidget {
  final CategoryModel category;
  final VoidCallback? onTap;

  const CategoryWidget({
    super.key,
    required this.category,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      behavior: HitTestBehavior.opaque,
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          ClipOval(
            child: SizedBox(
              width: 64,
              height: 64,
              child: category.imageUrl != null
                  ? Image.network(
                      category.imageUrl!,
                      fit: BoxFit.cover,
                      errorBuilder: (context, error, stackTrace) => _buildPlaceholder(context),
                      loadingBuilder: (context, child, progress) =>
                          progress == null ? child : _buildPlaceholder(context),
                    )
                  : _buildPlaceholder(context),
            ),
          ),
          const SizedBox(height: 8),
          Text(
            category.name,
            style: TextStyle(
              fontSize: 13,
              fontWeight: FontWeight.w500,
              color: context.colors.textDark,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildPlaceholder(BuildContext context) {
    return Container(
      color: context.colors.inputFill,
      alignment: Alignment.center,
      child: Icon(Icons.category_rounded, color: context.colors.primary, size: 26),
    );
  }
}
