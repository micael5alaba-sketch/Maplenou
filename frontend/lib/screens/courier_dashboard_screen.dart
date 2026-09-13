import 'package:flutter/material.dart';

import '../models/courier_dashboard_model.dart';
import '../services/courier_dashboard_service.dart';
import '../theme/app_color_scheme.dart';
import '../widgets/active_delivery_card.dart';
import '../widgets/app_drawer.dart';
import '../widgets/app_logo.dart';
import '../widgets/courier_bottom_navigation.dart';
import '../widgets/daily_earnings_card.dart';
import '../widgets/earnings_evolution_card.dart';
import '../widgets/online_status_toggle.dart';
import '../widgets/performance_card.dart';
import 'courier_delivery_confirmation_screen.dart';
import 'courier_delivery_detail_screen.dart';
import 'courier_messages_screen.dart';
import 'courier_profile_screen.dart';
import 'courier_deliveries_screen.dart';

/// Courier's main dashboard: online/offline toggle, earnings evolution,
/// daily goal, performance, and active deliveries.
///
/// Backed by [CourierDashboardService] mocked data for now — no network
/// call yet. The earnings/goal/rating figures have no backend model to
/// map onto (see that service's doc comment).
class CourierDashboardScreen extends StatefulWidget {
  const CourierDashboardScreen({super.key});

  @override
  State<CourierDashboardScreen> createState() => _CourierDashboardScreenState();
}

class _CourierDashboardScreenState extends State<CourierDashboardScreen> {
  final _scaffoldKey = GlobalKey<ScaffoldState>();
  final _service = CourierDashboardService();
  late CourierDashboardModel _dashboard;

  final CourierTab _currentTab = CourierTab.dashboard;

  @override
  void initState() {
    super.initState();
    _dashboard = _service.getDashboard(isOnline: false);
  }

  void _setOnline(bool isOnline) {
    setState(() => _dashboard = _service.getDashboard(isOnline: isOnline));
  }

  void _showComingSoon(String feature) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text('$feature bientôt disponible.')));
  }

  void _openDeliveryDetail(ActiveDeliveryModel delivery) {
    Navigator.of(context).push(MaterialPageRoute(builder: (_) => const CourierDeliveryDetailScreen()));
  }

  void _openConfirmation(ActiveDeliveryModel delivery) {
    Navigator.of(context).push(MaterialPageRoute(builder: (_) => const CourierDeliveryConfirmationScreen()));
  }

  void _onTabSelected(CourierTab tab) {
    if (tab == _currentTab) return;
    switch (tab) {
      case CourierTab.dashboard:
        break;
      case CourierTab.deliveries:
        Navigator.of(context).push(MaterialPageRoute(builder: (_) => const CourierDeliveriesScreen()));
        break;
      case CourierTab.messages:
        Navigator.of(context).push(MaterialPageRoute(builder: (_) => const CourierMessagesScreen()));
        break;
      case CourierTab.profile:
        Navigator.of(context).push(MaterialPageRoute(builder: (_) => const CourierProfileScreen()));
        break;
    }
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    final dashboard = _dashboard;

    return Scaffold(
      key: _scaffoldKey,
      backgroundColor: colors.homeBackground,
      drawer: AppDrawer(onComingSoon: _showComingSoon),
      body: SafeArea(
        child: Column(
          children: [
            _buildHeader(colors),
            Expanded(
              child: SingleChildScrollView(
                padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Bonjour, ${dashboard.courierName}',
                      style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: colors.textDark),
                    ),
                    const SizedBox(height: 4),
                    Text('Prêt pour votre journée ?', style: TextStyle(fontSize: 14, color: colors.textMuted)),
                    const SizedBox(height: 16),
                    OnlineStatusToggle(isOnline: dashboard.isOnline, onChanged: _setOnline),
                    const SizedBox(height: 20),
                    EarningsEvolutionCard(
                      points: dashboard.weeklyEarningsSeries,
                      weeklyTotal: dashboard.weeklyTotal,
                      changePercent: dashboard.weeklyChangePercent,
                    ),
                    const SizedBox(height: 16),
                    DailyEarningsCard(earnings: dashboard.dailyEarnings, goal: dashboard.dailyGoal),
                    const SizedBox(height: 16),
                    PerformanceCard(
                      rating: dashboard.rating,
                      validatedDeliveries: dashboard.validatedDeliveries,
                      totalDeliveries: dashboard.totalDeliveriesToday,
                    ),
                    const SizedBox(height: 20),
                    SizedBox(
                      width: double.infinity,
                      height: 54,
                      child: ElevatedButton.icon(
                        onPressed: dashboard.isOnline
                            ? () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const CourierDeliveriesScreen()))
                            : () => _setOnline(true),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: colors.accentOrange,
                          foregroundColor: Colors.white,
                          elevation: 0,
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28)),
                        ),
                        icon: Icon(dashboard.isOnline ? Icons.local_shipping_rounded : Icons.toggle_on_rounded),
                        label: Text(
                          dashboard.isOnline ? 'Prêt pour une nouvelle course' : 'Passer en ligne',
                          style: const TextStyle(fontWeight: FontWeight.w600),
                        ),
                      ),
                    ),
                    const SizedBox(height: 20),
                    if (dashboard.isOnline && dashboard.activeDeliveries.isNotEmpty) ...[
                      Text('Courses Actives', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: colors.textDark)),
                      const SizedBox(height: 12),
                      for (final delivery in dashboard.activeDeliveries) ...[
                        ActiveDeliveryCard(
                          delivery: delivery,
                          onTap: () => _openDeliveryDetail(delivery),
                          onConfirm: () => _openConfirmation(delivery),
                        ),
                        const SizedBox(height: 12),
                      ],
                    ] else
                      _buildOfflineState(colors, dashboard.isOnline),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
      bottomNavigationBar: CourierBottomNavigation(currentTab: _currentTab, onTabSelected: _onTabSelected),
    );
  }

  Widget _buildHeader(AppColorScheme colors) {
    return Container(
      color: colors.surface,
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      child: Row(
        children: [
          IconButton(
            icon: Icon(Icons.menu_rounded, color: colors.textDark),
            onPressed: () => _scaffoldKey.currentState?.openDrawer(),
          ),
          const Expanded(child: Center(child: AppLogo(variant: AppLogoVariant.horizontalColor, size: 34))),
          IconButton(
            icon: Icon(Icons.notifications_none_rounded, color: colors.textDark),
            onPressed: () => _showComingSoon('Notifications'),
          ),
        ],
      ),
    );
  }

  Widget _buildOfflineState(AppColorScheme colors, bool isOnline) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 12, offset: const Offset(0, 4)),
        ],
      ),
      child: Column(
        children: [
          Icon(Icons.cloud_off_rounded, size: 40, color: colors.textMuted),
          const SizedBox(height: 12),
          Text(
            isOnline ? 'Aucune course active pour le moment.' : 'Vous êtes hors ligne',
            style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark),
          ),
          const SizedBox(height: 6),
          Text(
            isOnline
                ? "Consultez l'onglet Courses pour en accepter une."
                : 'Passez en ligne pour commencer à recevoir des courses et augmenter vos gains.',
            textAlign: TextAlign.center,
            style: TextStyle(fontSize: 13, color: colors.textMuted, height: 1.4),
          ),
        ],
      ),
    );
  }
}
