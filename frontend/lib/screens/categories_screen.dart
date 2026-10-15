import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_staggered_grid_view/flutter_staggered_grid_view.dart';

import '../models/category_model.dart';
import '../models/product_model.dart';
import '../services/api_client.dart';
import '../services/cart_service.dart';
import '../services/category_service.dart';
import '../services/product_service.dart';
import '../theme/app_color_scheme.dart';
import '../widgets/app_drawer.dart';
import '../widgets/app_logo.dart';
import '../widgets/custom_bottom_navigation.dart';
import '../widgets/filter_pill.dart';
import '../widgets/product_card.dart';
import '../widgets/search_field.dart';
import 'cart_screen.dart';
import 'favorites_screen.dart';
import 'home_screen.dart';
import 'product_details_screen.dart';
import 'profile_screen.dart';

enum _SortOption { recent, priceAsc, priceDesc }

/// "Catégories" tab: search + filters over a staggered results grid.
///
/// Reached from [HomeScreen]'s bottom navigation. Products are fetched
/// from the backend catalog (`GET /api/products`) via [ProductService],
/// with real category/prix/recherche filters — pas de "bientôt disponible"
/// ici, tous les filtres appellent le backend (recherche = full-text
/// tsvector, voir `ProductController`/`ProductRepository`).
class CategoriesScreen extends StatefulWidget {
  final String? initialSearch;
  final CategoryModel? initialCategory;

  const CategoriesScreen({super.key, this.initialSearch, this.initialCategory});

  @override
  State<CategoriesScreen> createState() => _CategoriesScreenState();
}

class _CategoriesScreenState extends State<CategoriesScreen> {
  final _scaffoldKey = GlobalKey<ScaffoldState>();
  final _productService = ProductService();
  final _categoryService = CategoryService();
  late final _searchController = TextEditingController(text: widget.initialSearch ?? '');

  List<ProductModel> _results = [];
  bool _isLoading = true;
  String? _error;
  Timer? _debounce;

  late CategoryModel? _selectedCategory = widget.initialCategory;
  num? _minPrice;
  num? _maxPrice;
  _SortOption _sort = _SortOption.recent;

  @override
  void initState() {
    super.initState();
    _loadResults();
  }

  @override
  void dispose() {
    _debounce?.cancel();
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _loadResults() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });
    try {
      final results = await _productService.getCategoryResults(
        categoryId: _selectedCategory?.id,
        minPrice: _minPrice,
        maxPrice: _maxPrice,
        search: _searchController.text,
      );
      if (!mounted) return;
      setState(() {
        _results = _sortResults(results);
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

  List<ProductModel> _sortResults(List<ProductModel> input) {
    final list = [...input];
    switch (_sort) {
      case _SortOption.recent:
        break;
      case _SortOption.priceAsc:
        list.sort((a, b) => a.price.compareTo(b.price));
      case _SortOption.priceDesc:
        list.sort((a, b) => b.price.compareTo(a.price));
    }
    return list;
  }

  void _onSearchChanged(String value) {
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 400), _loadResults);
  }

  Future<void> _pickCategory() async {
    List<CategoryModel> categories;
    try {
      categories = await _categoryService.getCategories();
    } on ApiException catch (e) {
      _showMessage(e.message);
      return;
    }
    if (!mounted) return;
    final colors = context.colors;
    final picked = await showModalBottomSheet<CategoryModel?>(
      context: context,
      backgroundColor: colors.surface,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (sheetContext) {
        return SafeArea(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const SizedBox(height: 12),
              Text('Catégorie', style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
              const SizedBox(height: 8),
              ListTile(
                title: Text('Toutes les catégories', style: TextStyle(color: colors.textDark)),
                trailing: _selectedCategory == null ? Icon(Icons.check_rounded, color: colors.primary) : null,
                onTap: () => Navigator.of(sheetContext).pop(),
              ),
              for (final category in categories)
                ListTile(
                  title: Text(category.name, style: TextStyle(color: colors.textDark)),
                  trailing: _selectedCategory?.id == category.id ? Icon(Icons.check_rounded, color: colors.primary) : null,
                  onTap: () => Navigator.of(sheetContext).pop(category),
                ),
              const SizedBox(height: 8),
            ],
          ),
        );
      },
    );
    if (!mounted) return;
    setState(() => _selectedCategory = picked);
    _loadResults();
  }

  Future<void> _pickPriceRange() async {
    final minController = TextEditingController(text: _minPrice?.toStringAsFixed(0) ?? '');
    final maxController = TextEditingController(text: _maxPrice?.toStringAsFixed(0) ?? '');
    final colors = context.colors;
    final applied = await showModalBottomSheet<bool>(
      context: context,
      backgroundColor: colors.surface,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (sheetContext) {
        return Padding(
          padding: EdgeInsets.only(bottom: MediaQuery.of(sheetContext).viewInsets.bottom),
          child: SafeArea(
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Fourchette de prix (FCFA)', style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
                  const SizedBox(height: 16),
                  Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: minController,
                          keyboardType: TextInputType.number,
                          decoration: const InputDecoration(labelText: 'Min'),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: TextField(
                          controller: maxController,
                          keyboardType: TextInputType.number,
                          decoration: const InputDecoration(labelText: 'Max'),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 20),
                  Row(
                    children: [
                      Expanded(
                        child: OutlinedButton(
                          onPressed: () => Navigator.of(sheetContext).pop(false),
                          child: const Text('Annuler'),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: ElevatedButton(
                          style: ElevatedButton.styleFrom(backgroundColor: colors.primary, foregroundColor: Colors.white),
                          onPressed: () => Navigator.of(sheetContext).pop(true),
                          child: const Text('Appliquer'),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
        );
      },
    );
    if (applied != true || !mounted) return;
    setState(() {
      _minPrice = num.tryParse(minController.text.trim());
      _maxPrice = num.tryParse(maxController.text.trim());
    });
    _loadResults();
  }

  void _pickSort() {
    final colors = context.colors;
    showModalBottomSheet<void>(
      context: context,
      backgroundColor: colors.surface,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (sheetContext) {
        const labels = {
          _SortOption.recent: 'Plus récent',
          _SortOption.priceAsc: 'Prix croissant',
          _SortOption.priceDesc: 'Prix décroissant',
        };
        return SafeArea(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const SizedBox(height: 12),
              Text('Trier par', style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
              for (final option in _SortOption.values)
                ListTile(
                  title: Text(labels[option]!, style: TextStyle(color: colors.textDark)),
                  trailing: _sort == option ? Icon(Icons.check_rounded, color: colors.primary) : null,
                  onTap: () {
                    Navigator.of(sheetContext).pop();
                    setState(() {
                      _sort = option;
                      _results = _sortResults(_results);
                    });
                  },
                ),
              const SizedBox(height: 8),
            ],
          ),
        );
      },
    );
  }

  void _clearFilters() {
    setState(() {
      _selectedCategory = null;
      _minPrice = null;
      _maxPrice = null;
      _searchController.clear();
    });
    _loadResults();
  }

  void _showMessage(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(message)));
  }

  void _openProductDetails(ProductModel product) {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => ProductDetailsScreen(product: product, relatedCatalog: _results),
      ),
    );
  }

  // `pushReplacement` dans tous les cas, voir home_screen.dart.
  void _onTabSelected(HomeTab tab) {
    if (tab == HomeTab.categories) return;
    if (tab == HomeTab.home) {
      Navigator.of(context).pushReplacement(MaterialPageRoute(builder: (_) => const HomeScreen()));
      return;
    }
    if (tab == HomeTab.profile) {
      Navigator.of(context).pushReplacement(MaterialPageRoute(builder: (_) => const ProfileScreen()));
      return;
    }
    Navigator.of(context).pushReplacement(MaterialPageRoute(builder: (_) => const CartScreen()));
  }

  Future<void> _addToCart(ProductModel product) async {
    try {
      await CartService().addProductQuickly(product);
    } on ApiException catch (e) {
      _showMessage(e.message);
      return;
    }
    if (!mounted) return;
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
      drawer: AppDrawer(onComingSoon: _showMessage),
      body: SafeArea(
        child: Column(
          children: [
            _buildHeader(),
            const SizedBox(height: 12),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: SearchField(
                hintText: 'Rechercher des produits, artisans...',
                controller: _searchController,
                onChanged: _onSearchChanged,
              ),
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
            onPressed: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const FavoritesScreen())),
          ),
        ],
      ),
    );
  }

  Widget _buildFilters() {
    final hasFilters = _selectedCategory != null || _minPrice != null || _maxPrice != null || _searchController.text.isNotEmpty;

    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: Row(
        children: [
          FilterPill(label: 'Tout voir', isActive: !hasFilters, onTap: hasFilters ? _clearFilters : null),
          const SizedBox(width: 8),
          FilterPill(
            label: _selectedCategory?.name ?? 'Catégorie',
            isActive: _selectedCategory != null,
            showChevron: true,
            onTap: _pickCategory,
          ),
          const SizedBox(width: 8),
          FilterPill(
            label: (_minPrice != null || _maxPrice != null) ? 'Prix ✓' : 'Prix',
            isActive: _minPrice != null || _maxPrice != null,
            showChevron: true,
            onTap: _pickPriceRange,
          ),
        ],
      ),
    );
  }

  Widget _buildResultsBar() {
    final colors = context.colors;
    const sortLabels = {
      _SortOption.recent: 'Plus récent',
      _SortOption.priceAsc: 'Prix croissant',
      _SortOption.priceDesc: 'Prix décroissant',
    };

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
              onTap: _pickSort,
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Flexible(
                    child: Text.rich(
                      TextSpan(
                        children: [
                          TextSpan(text: 'Trier par: ', style: TextStyle(fontSize: 13, color: colors.textMuted)),
                          TextSpan(
                            text: sortLabels[_sort],
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
    if (_results.isEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 32),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.search_off_rounded, size: 42, color: context.colors.textMuted),
              const SizedBox(height: 12),
              Text('Aucun résultat pour ces critères.', style: TextStyle(color: context.colors.textMuted)),
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
