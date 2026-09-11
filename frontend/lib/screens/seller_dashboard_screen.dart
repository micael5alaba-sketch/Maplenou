import 'package:flutter/material.dart';

import '../models/seller_dashboard_model.dart';
import '../services/seller_dashboard_service.dart';
import '../theme/app_color_scheme.dart';
import '../widgets/app_drawer.dart';
import '../widgets/balance_card.dart';
import '../widgets/best_selling_product_widget.dart';
import '../widgets/dashboard_header.dart';
import '../widgets/orders_card.dart';
import '../widgets/revenue_card.dart';
import '../widgets/sales_chart_widget.dart';
import '../widgets/seller_bottom_navigation.dart';
import '../widgets/top_categories_widget.dart';
import 'orders_management_screen.dart';
import 'vendor_catalog_screen.dart';

/// Seller's main dashboard, shown after they log in to their seller space:
/// KPIs (revenue, orders, balance), a sales chart, top categories and the
/// best-selling product, plus the seller-specific bottom navigation.
///
/// Backed by [SellerDashboardService] mocked data for now — no network
/// call yet. Swapping it for real Spring Boot endpoints later only
/// touches that service, not this screen or its widgets.
class SellerDashboardScreen extends StatefulWidget {
  const SellerDashboardScreen({super.key});

  @override
  State<SellerDashboardScreen> createState() => _SellerDashboardScreenState();
}

class _SellerDashboardScreenState extends State<SellerDashboardScreen> {
  final _scaffoldKey = GlobalKey<ScaffoldState>();
  final _dashboard = SellerDashboardService().getDashboard();

  final SellerTab _currentTab = SellerTab.dashboard;

  void _showComingSoon(String feature) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text('$feature bientôt disponible.')));
  }

  void _onTabSelected(SellerTab tab) {
    if (tab == _currentTab) return;
    switch (tab) {
      case SellerTab.dashboard:
        break;
      case SellerTab.orders:
        Navigator.of(context).push(MaterialPageRoute(builder: (_) => const OrdersManagementScreen()));
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
    final dashboard = _dashboard;

    return Scaffold(
      key: _scaffoldKey,
      backgroundColor: context.colors.homeBackground,
      drawer: AppDrawer(onComingSoon: _showComingSoon),
      body: SafeArea(
        child: Column(
          children: [
            DashboardHeader(
              onMenuTap: () => _scaffoldKey.currentState?.openDrawer(),
              onNotificationTap: () => _showComingSoon('Notifications'),
            ),
            Expanded(
              child: SingleChildScrollView(
                padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    _buildGreeting(dashboard),
                    const SizedBox(height: 24),
                    RevenueCard(
                      weeklyRevenue: dashboard.weeklyRevenue,
                      changePercent: dashboard.revenueChangePercent,
                    ),
                    const SizedBox(height: 16),
                    OrdersCard(
                      ongoingOrdersCount: dashboard.ongoingOrdersCount,
                      readyToShipCount: dashboard.readyToShipCount,
                      onTap: () => _showComingSoon('Le détail des commandes'),
                    ),
                    const SizedBox(height: 16),
                    BalanceCard(
                      availableBalance: dashboard.availableBalance,
                      onWithdraw: () => _showComingSoon('Le retrait de solde'),
                    ),
                    const SizedBox(height: 24),
                    SalesChartWidget(salesByRange: dashboard.salesByRange),
                    const SizedBox(height: 24),
                    TopCategoriesWidget(categories: dashboard.topCategories),
                    const SizedBox(height: 24),
                    BestSellingProductWidget(product: dashboard.bestSellingProduct),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
      bottomNavigationBar: SellerBottomNavigation(
        currentTab: _currentTab,
        onTabSelected: _onTabSelected,
      ),
    );
  }

  Widget _buildGreeting(SellerDashboardModel dashboard) {
    final colors = context.colors;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Bonjour, ${dashboard.sellerName}',
          style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: colors.primary),
        ),
        const SizedBox(height: 4),
        Text(
          'Bienvenue dans votre espace vendeur.',
          style: TextStyle(fontSize: 14, color: colors.textMuted),
        ),
      ],
    );
  }
}
