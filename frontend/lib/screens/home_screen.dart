import 'package:flutter/material.dart';
import 'package:flutter_staggered_grid_view/flutter_staggered_grid_view.dart';

import '../models/category_model.dart';
import '../models/product_model.dart';
import '../services/cart_service.dart';
import '../services/category_service.dart';
import '../services/product_service.dart';
import '../theme/app_color_scheme.dart';
import '../widgets/app_drawer.dart';
import '../widgets/app_logo.dart';
import '../widgets/category_carousel.dart';
import '../widgets/custom_bottom_navigation.dart';
import '../widgets/product_card.dart';
import '../widgets/search_field.dart';
import 'cart_screen.dart';
import 'categories_screen.dart';
import 'favorites_screen.dart';
import 'product_details_screen.dart';
import 'profile_screen.dart';

/// Marketplace home screen: header, search bar, promo banner, categories
/// and the popular-products grid, plus the fixed bottom navigation.
///
/// Categories and products are fetched from the Spring Boot backend
/// (`GET /api/categories`, `GET /api/products`) via [CategoryService] and
/// [ProductService].
class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  final _scaffoldKey = GlobalKey<ScaffoldState>();
  final _categoryService = CategoryService();
  final _productService = ProductService();

  List<CategoryModel> _categories = [];
  List<ProductModel> _products = [];
  bool _isLoading = true;
  String? _error;

  final HomeTab _currentTab = HomeTab.home;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });
    try {
      final categories = await _categoryService.getCategories();
      final products = await _productService.getPopularProducts();
      if (!mounted) return;
      setState(() {
        _categories = categories;
        _products = products;
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

  void _openProductDetails(ProductModel product) {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => ProductDetailsScreen(product: product, relatedCatalog: _products),
      ),
    );
  }

  void _onTabSelected(HomeTab tab) {
    if (tab == _currentTab) return;
    switch (tab) {
      case HomeTab.home:
        break;
      case HomeTab.categories:
        Navigator.of(context).push(MaterialPageRoute(builder: (_) => const CategoriesScreen()));
        break;
      case HomeTab.cart:
        Navigator.of(context).push(MaterialPageRoute(builder: (_) => const CartScreen()));
        break;
      case HomeTab.profile:
        Navigator.of(context).push(MaterialPageRoute(builder: (_) => const ProfileScreen()));
        break;
    }
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

  void _showComingSoon(String feature) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text('$feature bientôt disponible.')));
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
              child: SearchField(hintText: 'Que recherchez-vous ?'),
            ),
            Expanded(child: _buildBody()),
          ],
        ),
      ),
      bottomNavigationBar: CustomBottomNavigation(
        currentTab: _currentTab,
        onTabSelected: _onTabSelected,
      ),
    );
  }

  Widget _buildBody() {
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
              ElevatedButton(onPressed: _loadData, child: const Text('Réessayer')),
            ],
          ),
        ),
      );
    }

    return SingleChildScrollView(
      padding: const EdgeInsets.only(bottom: 24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const SizedBox(height: 16),
          _buildBanner(),
          const SizedBox(height: 24),
          _buildCategoriesSection(),
          const SizedBox(height: 24),
          _buildProductsSection(),
        ],
      ),
    );
  }

  // ---------------------------------------------------------------------
  // Header: hamburger menu, centered logo + brand name, favorites/notifs.
  // ---------------------------------------------------------------------
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
            onPressed: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const FavoritesScreen())),
          ),
          _buildNotificationsIcon(),
        ],
      ),
    );
  }

  Widget _buildNotificationsIcon() {
    return Stack(
      clipBehavior: Clip.none,
      children: [
        IconButton(
          icon: Icon(Icons.notifications_none_rounded, color: context.colors.textDark),
          onPressed: () => _showComingSoon('Notifications'),
        ),
        Positioned(
          top: 8,
          right: 8,
          child: Container(
            width: 9,
            height: 9,
            decoration: BoxDecoration(color: context.colors.error, shape: BoxShape.circle),
          ),
        ),
      ],
    );
  }

  // ---------------------------------------------------------------------
  // Promotional hero banner. Overlaid on a photo, so its text/button stay
  // white/black regardless of the app's light/dark theme.
  // ---------------------------------------------------------------------
  Widget _buildBanner() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(24),
        child: SizedBox(
          height: 190,
          width: double.infinity,
          child: Stack(
            fit: StackFit.expand,
            children: [
              Image.asset('assets/images/banner_mode.jpg', fit: BoxFit.cover),
              Container(color: context.colors.accentOrange.withValues(alpha: 0.55)),
              Padding(
                padding: const EdgeInsets.all(20),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    const Text(
                      "Jusqu'à\n-30% sur la\nmode",
                      style: TextStyle(
                        color: Colors.white,
                        fontSize: 22,
                        fontWeight: FontWeight.bold,
                        height: 1.2,
                      ),
                    ),
                    const SizedBox(height: 14),
                    ElevatedButton(
                      onPressed: () => _showComingSoon('La promotion mode'),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Colors.black,
                        foregroundColor: Colors.white,
                        elevation: 0,
                        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
                      ),
                      child: const Text('Découvrir', style: TextStyle(fontWeight: FontWeight.w600)),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  // ---------------------------------------------------------------------
  // Horizontal categories list.
  // ---------------------------------------------------------------------
  Widget _buildCategoriesSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16),
          child: Text(
            'Catégories',
            style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: context.colors.textDark),
          ),
        ),
        const SizedBox(height: 14),
        SizedBox(
          height: 100,
          child: CategoryCarousel(
            categories: _categories,
            onCategoryTap: (category) => _showComingSoon('La catégorie ${category.name}'),
          ),
        ),
      ],
    );
  }

  // ---------------------------------------------------------------------
  // Popular products grid (2 columns).
  // ---------------------------------------------------------------------
  Widget _buildProductsSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(
                child: Text(
                  'Produits populaires',
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: context.colors.textDark),
                ),
              ),
              TextButton(
                onPressed: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const CategoriesScreen())),
                style: TextButton.styleFrom(padding: EdgeInsets.zero),
                child: Text(
                  'Voir tout',
                  style: TextStyle(color: context.colors.primary, fontWeight: FontWeight.w600),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16),
          // Staggered grid (à la AliExpress): each product's own photo
          // ratio drives its card height, so the two columns naturally
          // fall out of alignment instead of sitting in a rigid grid.
          child: MasonryGridView.count(
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            crossAxisCount: 2,
            mainAxisSpacing: 16,
            crossAxisSpacing: 16,
            itemCount: _products.length,
            itemBuilder: (context, index) {
              final product = _products[index];
              return ProductCard(
                product: product,
                onTap: () => _openProductDetails(product),
                onAddToCart: () => _addToCart(product),
              );
            },
          ),
        ),
      ],
    );
  }
}
