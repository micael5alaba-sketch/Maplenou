import 'package:flutter/material.dart';

import '../models/vendor_product_model.dart';
import '../services/vendor_catalog_service.dart';
import '../theme/app_color_scheme.dart';
import '../widgets/app_drawer.dart';
import '../widgets/filter_chips.dart';
import '../widgets/search_field.dart';
import '../widgets/seller_bottom_navigation.dart';
import '../widgets/vendor_product_card.dart';
import 'orders_management_screen.dart';

/// Seller's product catalog: search, filter by stock status, and manage
/// each product (edit/view/duplicate/delete) from a 3-dot menu.
///
/// Reuses [SearchField] (buyer catalog's search bar — same rounded,
/// shadowed white input) and [SellerBottomNavigation] rather than
/// building near-duplicates of either.
///
/// Backed by [VendorCatalogService] mocked data for now — no network call
/// yet. Swapping it for a real `GET /api/shops/mine/products` later only
/// touches that service, not this screen or its widgets.
class VendorCatalogScreen extends StatefulWidget {
  const VendorCatalogScreen({super.key});

  @override
  State<VendorCatalogScreen> createState() => _VendorCatalogScreenState();
}

class _VendorCatalogScreenState extends State<VendorCatalogScreen> {
  final _scaffoldKey = GlobalKey<ScaffoldState>();
  final _searchController = TextEditingController();

  late List<VendorProductModel> _products;
  ProductStockStatus? _selectedStatus;
  String _query = '';

  @override
  void initState() {
    super.initState();
    _products = VendorCatalogService().getProducts();
    _searchController.addListener(() {
      setState(() => _query = _searchController.text.trim().toLowerCase());
    });
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  List<VendorProductModel> get _filteredProducts {
    return _products.where((product) {
      final matchesStatus = _selectedStatus == null || product.status == _selectedStatus;
      final matchesQuery = _query.isEmpty || product.name.toLowerCase().contains(_query);
      return matchesStatus && matchesQuery;
    }).toList();
  }

  void _showComingSoon(String feature) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text('$feature bientôt disponible.')));
  }

  Future<void> _confirmDelete(VendorProductModel product) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Supprimer ce produit ?'),
        content: Text('"${product.name}" sera retiré de votre catalogue.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(false),
            child: const Text('Annuler'),
          ),
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(true),
            child: Text('Supprimer', style: TextStyle(color: context.colors.error)),
          ),
        ],
      ),
    );

    if (confirmed != true || !mounted) return;
    setState(() => _products.removeWhere((p) => p.id == product.id));
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text('"${product.name}" supprimé.')));
  }

  void _onTabSelected(SellerTab tab) {
    if (tab == SellerTab.products) return;
    switch (tab) {
      case SellerTab.dashboard:
        Navigator.of(context).pop();
        break;
      case SellerTab.orders:
        Navigator.of(context).push(MaterialPageRoute(builder: (_) => const OrdersManagementScreen()));
        break;
      case SellerTab.products:
        break;
      case SellerTab.profile:
        _showComingSoon('Le profil vendeur');
        break;
    }
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    final filtered = _filteredProducts;

    return Scaffold(
      key: _scaffoldKey,
      backgroundColor: colors.homeBackground,
      drawer: AppDrawer(onComingSoon: _showComingSoon),
      floatingActionButton: FloatingActionButton(
        backgroundColor: colors.primary,
        foregroundColor: Colors.white,
        onPressed: () => _showComingSoon('Ajouter un produit'),
        child: const Icon(Icons.add_rounded),
      ),
      body: SafeArea(
        child: Column(
          children: [
            _buildHeader(),
            Expanded(
              child: ListView(
                padding: const EdgeInsets.only(bottom: 90),
                children: [
                  _buildTitleSection(),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                    child: SearchField(hintText: 'Rechercher un produit...'),
                  ),
                  const SizedBox(height: 14),
                  _buildFilterChips(),
                  const SizedBox(height: 16),
                  if (filtered.isEmpty)
                    _buildEmptyState()
                  else
                    Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 16),
                      child: Column(
                        children: [
                          for (final product in filtered) ...[
                            VendorProductCard(
                              product: product,
                              onEdit: () => _showComingSoon('La modification de "${product.name}"'),
                              onView: () => _showComingSoon('La fiche de "${product.name}"'),
                              onDuplicate: () => _showComingSoon('La duplication de "${product.name}"'),
                              onDelete: () => _confirmDelete(product),
                            ),
                            const SizedBox(height: 14),
                          ],
                        ],
                      ),
                    ),
                ],
              ),
            ),
          ],
        ),
      ),
      bottomNavigationBar: SellerBottomNavigation(
        currentTab: SellerTab.products,
        onTabSelected: _onTabSelected,
      ),
    );
  }

  Widget _buildHeader() {
    final colors = context.colors;

    return Container(
      color: colors.surface,
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      child: Row(
        children: [
          IconButton(
            icon: Icon(Icons.menu_rounded, color: colors.textDark),
            onPressed: () => _scaffoldKey.currentState?.openDrawer(),
          ),
          Expanded(
            child: Text(
              'Maplenou Vendor',
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 17, fontWeight: FontWeight.bold, color: colors.textDark),
            ),
          ),
          Padding(
            padding: const EdgeInsets.only(right: 8),
            child: CircleAvatar(
              radius: 16,
              backgroundColor: colors.primary.withValues(alpha: 0.12),
              child: Text(
                'K',
                style: TextStyle(fontWeight: FontWeight.bold, color: colors.primary),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTitleSection() {
    final colors = context.colors;

    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Catalogue de Produits',
            style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: colors.textDark),
          ),
          const SizedBox(height: 4),
          Text(
            'Gérez votre inventaire et ajoutez de nouvelles créations.',
            style: TextStyle(fontSize: 13, color: colors.textMuted),
          ),
          const SizedBox(height: 16),
        ],
      ),
    );
  }

  Widget _buildFilterChips() {
    int countFor(ProductStockStatus? status) =>
        status == null ? _products.length : _products.where((p) => p.status == status).length;

    return FilterChips<ProductStockStatus?>(
      selected: _selectedStatus,
      onSelected: (status) => setState(() => _selectedStatus = status),
      items: [
        FilterChipItem(label: 'Tous', count: countFor(null), value: null),
        FilterChipItem(
          label: ProductStockStatus.inStock.filterLabel,
          count: countFor(ProductStockStatus.inStock),
          value: ProductStockStatus.inStock,
        ),
        FilterChipItem(
          label: ProductStockStatus.outOfStock.filterLabel,
          count: countFor(ProductStockStatus.outOfStock),
          value: ProductStockStatus.outOfStock,
        ),
        FilterChipItem(
          label: ProductStockStatus.lowStock.filterLabel,
          count: countFor(ProductStockStatus.lowStock),
          value: ProductStockStatus.lowStock,
        ),
      ],
    );
  }

  Widget _buildEmptyState() {
    final colors = context.colors;

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 48),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.inventory_2_outlined, size: 48, color: colors.textMuted),
          const SizedBox(height: 12),
          Text('Aucun produit ne correspond.', style: TextStyle(color: colors.textMuted, fontWeight: FontWeight.w500)),
        ],
      ),
    );
  }
}
