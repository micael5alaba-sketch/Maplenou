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

  /// Tant qu'une catégorie n'a pas de vraie photo (`imageUrl`, upload Cloudinary admin), une
  /// icône + couleur propres à son thème valent mieux qu'un même pictogramme générique gris
  /// répété pour toutes — correspondance par slug (stable, indépendante du libellé affiché).
  Widget _buildPlaceholder(BuildContext context) {
    final (icon, color) = _iconAndColorFor(category.slug);
    return Container(
      color: color.withValues(alpha: 0.12),
      alignment: Alignment.center,
      child: Icon(icon, color: color, size: 28),
    );
  }

  (IconData, Color) _iconAndColorFor(String slug) {
    switch (slug) {
      case 'alimentation':
        return (Icons.restaurant_rounded, const Color(0xFFE2772E));
      case 'mode-vetements':
        return (Icons.checkroom_rounded, const Color(0xFF8E5CD9));
      case 'electronique-high-tech':
        return (Icons.devices_rounded, const Color(0xFF2E7FE2));
      case 'maison-jardin':
        return (Icons.yard_rounded, const Color(0xFF2E9E5B));
      case 'beaute-hygiene':
        return (Icons.spa_rounded, const Color(0xFFD9548E));
      case 'sante-bien-etre':
        return (Icons.favorite_rounded, const Color(0xFFD94A4A));
      case 'enfants-jouets':
        return (Icons.toys_rounded, const Color(0xFFE2B52E));
      case 'autres':
        return (Icons.apps_rounded, const Color(0xFF7A7A7A));
      default:
        return (Icons.category_rounded, const Color(0xFF1E5A24));
    }
  }
}
