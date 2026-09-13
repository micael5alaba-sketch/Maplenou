import 'package:flutter/material.dart';
import 'package:flutter_staggered_grid_view/flutter_staggered_grid_view.dart';

import '../models/product_model.dart';
import '../services/favorites_service.dart';
import '../theme/app_color_scheme.dart';
import '../widgets/product_card.dart';
import 'product_details_screen.dart';

/// "Mes Favoris" — reached from [ProfileScreen]'s activity section.
///
/// Backed by [FavoritesService], an in-memory singleton shared across the
/// app session — maps onto the real, already working `FavoriteController`
/// once auth is wired up.
class FavoritesScreen extends StatefulWidget {
  const FavoritesScreen({super.key});

  @override
  State<FavoritesScreen> createState() => _FavoritesScreenState();
}

class _FavoritesScreenState extends State<FavoritesScreen> {
  final _service = FavoritesService();

  void _openProductDetails(ProductModel product) {
    Navigator.of(context)
        .push(MaterialPageRoute(builder: (_) => ProductDetailsScreen(product: product)))
        .then((_) => setState(() {}));
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    final favorites = _service.items;

    return Scaffold(
      backgroundColor: colors.homeBackground,
      body: SafeArea(
        child: Column(
          children: [
            _buildHeader(colors),
            Expanded(
              child: favorites.isEmpty
                  ? _buildEmptyState(colors)
                  : MasonryGridView.count(
                      padding: const EdgeInsets.all(16),
                      crossAxisCount: 2,
                      mainAxisSpacing: 16,
                      crossAxisSpacing: 16,
                      itemCount: favorites.length,
                      itemBuilder: (context, index) {
                        final product = favorites[index];
                        return ProductCard(product: product, onTap: () => _openProductDetails(product));
                      },
                    ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader(AppColorScheme colors) {
    return Container(
      color: colors.surface,
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
      child: Row(
        children: [
          IconButton(icon: Icon(Icons.arrow_back_rounded, color: colors.textDark), onPressed: () => Navigator.of(context).pop()),
          Expanded(child: Text('Mes Favoris', textAlign: TextAlign.center, style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark))),
          const SizedBox(width: 48),
        ],
      ),
    );
  }

  Widget _buildEmptyState(AppColorScheme colors) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.favorite_border_rounded, size: 48, color: colors.textMuted),
            const SizedBox(height: 12),
            Text("Vous n'avez pas encore de favoris.", style: TextStyle(color: colors.textMuted, fontWeight: FontWeight.w500)),
          ],
        ),
      ),
    );
  }
}
