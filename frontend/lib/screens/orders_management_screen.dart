import 'package:flutter/material.dart';

import '../models/vendor_order_model.dart';
import '../services/vendor_orders_service.dart';
import '../theme/app_color_scheme.dart';
import '../widgets/app_drawer.dart';
import '../widgets/app_logo.dart';
import '../widgets/empty_orders_widget.dart';
import '../widgets/order_card.dart';
import '../widgets/orders_filter_bar.dart';
import '../widgets/seller_bottom_navigation.dart';
import 'vendor_catalog_screen.dart';

/// Seller's order list: filter by status, scroll through pages of orders,
/// and move each one to its next status ("Prêt pour livraison" →
/// "Marquer comme livrée").
///
/// Reuses [SellerBottomNavigation] rather than a separate
/// "VendorBottomNavigation" — same 4 tabs (Dashboard, Commandes, Produits,
/// Profil), no need for a second near-identical widget.
///
/// Backed by [VendorOrdersService] mocked data for now — shaped like a
/// real paginated, filterable endpoint so wiring the real one later only
/// touches that service.
class OrdersManagementScreen extends StatefulWidget {
  const OrdersManagementScreen({super.key});

  @override
  State<OrdersManagementScreen> createState() => _OrdersManagementScreenState();
}

class _OrdersManagementScreenState extends State<OrdersManagementScreen> {
  final _scaffoldKey = GlobalKey<ScaffoldState>();
  final _ordersService = VendorOrdersService();
  final _scrollController = ScrollController();

  VendorOrderStatus? _selectedFilter;
  final List<VendorOrderModel> _orders = [];
  int _page = 0;
  bool _isLoading = true;
  bool _isLoadingMore = false;
  bool _hasMore = true;

  @override
  void initState() {
    super.initState();
    _scrollController.addListener(_onScroll);
    _loadPage(reset: true);
  }

  @override
  void dispose() {
    _scrollController.removeListener(_onScroll);
    _scrollController.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (!_hasMore || _isLoadingMore || _isLoading) return;
    final threshold = _scrollController.position.maxScrollExtent - 200;
    if (_scrollController.position.pixels >= threshold) {
      _loadPage(reset: false);
    }
  }

  Future<void> _loadPage({required bool reset}) async {
    setState(() {
      if (reset) {
        _isLoading = true;
        _page = 0;
        _orders.clear();
        _hasMore = true;
      } else {
        _isLoadingMore = true;
      }
    });

    final results = await _ordersService.getOrders(status: _selectedFilter, page: _page);
    if (!mounted) return;

    setState(() {
      _orders.addAll(results);
      _hasMore = results.length == VendorOrdersService.pageSize;
      _page++;
      _isLoading = false;
      _isLoadingMore = false;
    });
  }

  void _onFilterSelected(VendorOrderStatus? status) {
    if (status == _selectedFilter) return;
    setState(() => _selectedFilter = status);
    _loadPage(reset: true);
  }

  void _handlePrimaryAction(VendorOrderModel order) {
    final nextStatus = order.status == VendorOrderStatus.toPrepare
        ? VendorOrderStatus.shipping
        : VendorOrderStatus.delivered;

    setState(() {
      final index = _orders.indexWhere((o) => o.id == order.id);
      if (index == -1) return;

      final updated = VendorOrderModel(
        id: order.id,
        orderNumber: order.orderNumber,
        dateTime: order.dateTime,
        customerName: order.customerName,
        productNames: order.productNames,
        status: nextStatus,
        amount: order.amount,
      );

      // Le filtre actif ne correspond peut-être plus à ce nouveau statut —
      // dans ce cas la commande sort simplement de la liste affichée.
      if (_selectedFilter != null && _selectedFilter != nextStatus) {
        _orders.removeAt(index);
      } else {
        _orders[index] = updated;
      }
    });

    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text('${order.orderNumber} → ${nextStatus.badgeLabel}')));
  }

  void _showComingSoon(String feature) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text('$feature bientôt disponible.')));
  }

  void _onTabSelected(SellerTab tab) {
    if (tab == SellerTab.orders) return;
    switch (tab) {
      case SellerTab.dashboard:
        Navigator.of(context).pop();
        break;
      case SellerTab.orders:
        break;
      case SellerTab.products:
        Navigator.of(context).push(MaterialPageRoute(builder: (_) => const VendorCatalogScreen()));
        break;
      case SellerTab.profile:
        _showComingSoon('Le profil vendeur');
        break;
    }
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return Scaffold(
      key: _scaffoldKey,
      backgroundColor: colors.homeBackground,
      drawer: AppDrawer(onComingSoon: _showComingSoon),
      body: SafeArea(
        child: Column(
          children: [
            _buildHeader(),
            Expanded(
              child: RefreshIndicator(
                onRefresh: () => _loadPage(reset: true),
                color: colors.primary,
                child: CustomScrollView(
                  controller: _scrollController,
                  slivers: [
                    SliverToBoxAdapter(child: _buildTitleSection()),
                    SliverToBoxAdapter(
                      child: Padding(
                        padding: const EdgeInsets.only(bottom: 14),
                        child: OrdersFilterBar(selected: _selectedFilter, onSelected: _onFilterSelected),
                      ),
                    ),
                    _buildOrdersSliver(),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
      bottomNavigationBar: SellerBottomNavigation(
        currentTab: SellerTab.orders,
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
            padding: const EdgeInsets.only(right: 12),
            child: ClipOval(
              child: Container(
                width: 32,
                height: 32,
                color: colors.inputFill,
                padding: const EdgeInsets.all(6),
                child: const AppLogo(variant: AppLogoVariant.iconColor, size: 20),
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
            'Gestion des Commandes',
            style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: colors.textDark),
          ),
          const SizedBox(height: 4),
          Text(
            "Gérez et suivez l'état de vos commandes récentes.",
            style: TextStyle(fontSize: 13, color: colors.textMuted),
          ),
        ],
      ),
    );
  }

  Widget _buildOrdersSliver() {
    if (_isLoading) {
      return SliverFillRemaining(
        hasScrollBody: false,
        child: Center(child: CircularProgressIndicator(color: context.colors.primary)),
      );
    }

    if (_orders.isEmpty) {
      return const SliverFillRemaining(hasScrollBody: false, child: EmptyOrdersWidget());
    }

    return SliverPadding(
      padding: const EdgeInsets.fromLTRB(16, 0, 16, 24),
      sliver: SliverList.separated(
        itemCount: _orders.length + (_hasMore ? 1 : 0),
        separatorBuilder: (context, index) => const SizedBox(height: 14),
        itemBuilder: (context, index) {
          if (index >= _orders.length) {
            return Padding(
              padding: const EdgeInsets.symmetric(vertical: 16),
              child: Center(child: CircularProgressIndicator(color: context.colors.primary)),
            );
          }

          final order = _orders[index];
          return OrderCard(
            order: order,
            onViewDetails: () => _showComingSoon('Le détail de la commande ${order.orderNumber}'),
            onPrimaryAction: order.status == VendorOrderStatus.toPrepare ||
                    order.status == VendorOrderStatus.shipping
                ? () => _handlePrimaryAction(order)
                : null,
          );
        },
      ),
    );
  }
}
