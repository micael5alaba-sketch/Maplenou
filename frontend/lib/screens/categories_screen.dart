import 'package:flutter/material.dart';
import 'package:flutter_staggered_grid_view/flutter_staggered_grid_view.dart';

import '../models/product_model.dart';
import '../services/cart_service.dart';
import '../services/product_service.dart';
import '../theme/app_color_scheme.dart';
import '../widgets/app_drawer.dart';
import '../widgets/app_logo.dart';
import '../widgets/custom_bottom_navigation.dart';
import '../widgets/filter_pill.dart';
import '../widgets/product_card.dart';
import '../widgets/search_field.dart';
import 'cart_screen.dart';
import 'product_details_screen.dart';
import 'profile_screen.dart';

/// "Catégories" tab: search + filters over a staggered results grid.
///
/// Reached from [HomeScreen]'s bottom navigation. Products are fetched
/// from the backend catalog (`GET /api/products`) via [ProductService].
class CategoriesScreen extends StatefulWidget {
  const CategoriesScreen({super.key});

  @override
  State<CategoriesScreen> createState() => _CategoriesScreenState();
}

class _CategoriesScreenState extends State<CategoriesScreen> {
  final _scaffoldKey = GlobalKey<ScaffoldState>();
  final _productService = ProductService();

  List<ProductModel> _results = [];
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadResults();
  }

  Future<void> _loadResults() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });
    try {
      final results = await _productService.getCategoryResults();
      if (!mounted) return;
      setState(() {
        _results = results;
        _isLoading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _error = "Impossible de joindre le serveur. Vérifie que le backend tourne bien.";
        _isLoading = false;
      });
    }
  }

  void _showComingSoon(String feature) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text('$feature bientôt disponible.')));
  }

  void _openProductDetails(ProductModel product) {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => ProductDetailsScreen(product: product, relatedCatalog: _results),
      ),
    );
  }

  void _onTabSelected(HomeTab tab) {
    if (tab == HomeTab.categories) return;
    if (tab == HomeTab.home) {
      Navigator.of(context).pop();
      return;
    }
    if (tab == HomeTab.profile) {
      Navigator.of(context).push(MaterialPageRoute(builder: (_) => const ProfileScreen()));
      return;
    }
    Navigator.of(context).push(MaterialPageRoute(builder: (_) => const CartScreen()));
  }

  void _addToCart(ProductModel product) {
    CartService().addItem(
      productId: product.id,
      productName: product.name,
      imageUrl: product.imageUrl,
      unitPrice: product.price,
    );
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(
        content: Text('"${product.name}" ajouté au panier.'),
        action: SnackBarAction(
          label: 'Voir',
          onPressed: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const CartScreen())),
        ),
      ));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      key: _scaffoldKey,
      backgroundColor: context.colors.homeBackground,
      drawer: AppDrawer(onComingSoon: _showComingSoon),
      body: SafeArea(
        child: Column(
          children: [
            _buildHeader(),
            const SizedBox(height: 12),
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: 16),
              child: SearchField(hintText: 'Rechercher des produits, artisans...'),
            ),
            const SizedBox(height: 14),
            _buildFilters(),
            const SizedBox(height: 14),
            _buildResultsBar(),
            const SizedBox(height: 12),
            Expanded(child: _buildResultsGrid()),
          ],
        ),
      ),
      bottomNavigationBar: CustomBottomNavigation(
        currentTab: HomeTab.categories,
        onTabSelected: _onTabSelected,
      ),
    );
  }

  Widget _buildHeader() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      child: Row(
        children: [
          IconButton(
            icon: Icon(Icons.menu_rounded, color: context.colors.textDark),
            onPressed: () => _scaffoldKey.currentState?.openDrawer(),
          ),
          const Expanded(
            child: Center(
              child: AppLogo(variant: AppLogoVariant.horizontalColor, size: 34),
            ),
          ),
          IconButton(
            icon: Icon(Icons.favorite_border_rounded, color: context.colors.textDark),
            onPressed: () => _showComingSoon('Favoris'),
          ),
        ],
      ),
    );
  }

  Widget _buildFilters() {
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: Row(
        children: [
          const FilterPill(label: 'Tout voir', isActive: true),
          const SizedBox(width: 8),
          FilterPill(
            label: 'Catégorie',
            showChevron: true,
            onTap: () => _showComingSoon('Le filtre par catégorie'),
          ),
          const SizedBox(width: 8),
          FilterPill(
            label: 'Prix',
            showChevron: true,
            onTap: () => _showComingSoon('Le filtre par prix'),
          ),
        ],
      ),
    );
  }

  Widget _buildResultsBar() {
    final colors = context.colors;

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          // The backend paginates by cursor (no OFFSET), so it never
          // returns a total match count — this reflects only what's
          // actually loaded on this page.
          Flexible(
            child: Text.rich(
              TextSpan(
                children: [
                  TextSpan(
                    text: '${_results.length} ',
                    style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: colors.textDark),
                  ),
                  TextSpan(
                    text: 'Résultats',
                    style: TextStyle(fontSize: 14, color: colors.textMuted),
                  ),
                ],
              ),
              overflow: TextOverflow.ellipsis,
            ),
          ),
          const SizedBox(width: 8),
          Flexible(
            child: GestureDetector(
              onTap: () => _showComingSoon('Le tri des résultats'),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Flexible(
                    child: Text.rich(
                      TextSpan(
                        children: [
                          TextSpan(text: 'Trier par: ', style: TextStyle(fontSize: 13, color: colors.textMuted)),
                          TextSpan(
                            text: 'Plus récent',
                            style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: colors.textDark),
                          ),
                        ],
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                  const SizedBox(width: 2),
                  Icon(Icons.keyboard_arrow_down_rounded, size: 16, color: colors.textMuted),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildResultsGrid() {
    if (_isLoading) {
      return Center(child: CircularProgressIndicator(color: context.colors.primary));
    }
    if (_error != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 32),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.cloud_off_rounded, size: 42, color: context.colors.textMuted),
              const SizedBox(height: 12),
              Text(_error!, textAlign: TextAlign.center, style: TextStyle(color: context.colors.textMuted)),
              const SizedBox(height: 16),
              ElevatedButton(onPressed: _loadResults, child: const Text('Réessayer')),
            ],
          ),
        ),
      );
    }

    return SingleChildScrollView(
      padding: const EdgeInsets.fromLTRB(16, 0, 16, 24),
      child: MasonryGridView.count(
        shrinkWrap: true,
        physics: const NeverScrollableScrollPhysics(),
        crossAxisCount: 2,
        mainAxisSpacing: 16,
        crossAxisSpacing: 16,
        itemCount: _results.length,
        itemBuilder: (context, index) {
          final product = _results[index];
          return ProductCard(
            product: product,
            onTap: () => _openProductDetails(product),
            onAddToCart: () => _addToCart(product),
          );
        },
      ),
    );
  }
}
