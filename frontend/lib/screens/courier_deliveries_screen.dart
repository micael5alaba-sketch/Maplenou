import 'package:flutter/material.dart';

import '../models/available_delivery_model.dart';
import '../services/api_client.dart';
import '../services/available_deliveries_service.dart';
import '../theme/app_color_scheme.dart';
import '../widgets/app_drawer.dart';
import '../widgets/app_logo.dart';
import '../widgets/available_delivery_card.dart';
import '../widgets/courier_bottom_navigation.dart';
import 'courier_dashboard_screen.dart';
import 'courier_delivery_detail_screen.dart';
import 'courier_messages_screen.dart';
import 'courier_profile_screen.dart';
import 'notifications_screen.dart';

/// "Courses" tab: deliveries assigned to this courier by an admin, not yet
/// started (`ASSIGNED`) — voir `CLAUDE.md` §3 : un admin affecte le
/// livreur, ce n'est pas une file de courses à accepter soi-même.
///
/// Backed by [AvailableDeliveriesService] (`GET /api/deliveries/my`).
class CourierDeliveriesScreen extends StatefulWidget {
  const CourierDeliveriesScreen({super.key});

  @override
  State<CourierDeliveriesScreen> createState() => _CourierDeliveriesScreenState();
}

class _CourierDeliveriesScreenState extends State<CourierDeliveriesScreen> {
  final _scaffoldKey = GlobalKey<ScaffoldState>();
  final _service = AvailableDeliveriesService();
  late Future<List<AvailableDeliveryModel>> _deliveriesFuture;

  @override
  void initState() {
    super.initState();
    _deliveriesFuture = _service.getAvailableDeliveries();
  }

  void _reload() {
    setState(() => _deliveriesFuture = _service.getAvailableDeliveries());
  }

  void _showComingSoon(String feature) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text('$feature bientôt disponible.')));
  }

  void _openDelivery(AvailableDeliveryModel delivery) {
    Navigator.of(context)
        .push(MaterialPageRoute(builder: (_) => CourierDeliveryDetailScreen(deliveryId: delivery.id)))
        .then((_) => _reload());
  }

  // `pushReplacement` dans tous les cas, voir courier_dashboard_screen.dart.
  void _onTabSelected(CourierTab tab) {
    if (tab == CourierTab.deliveries) return;
    switch (tab) {
      case CourierTab.dashboard:
        Navigator.of(context).pushReplacement(MaterialPageRoute(builder: (_) => const CourierDashboardScreen()));
        break;
      case CourierTab.deliveries:
        break;
      case CourierTab.messages:
        Navigator.of(context).pushReplacement(MaterialPageRoute(builder: (_) => const CourierMessagesScreen()));
        break;
      case CourierTab.profile:
        Navigator.of(context).pushReplacement(MaterialPageRoute(builder: (_) => const CourierProfileScreen()));
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
            _buildHeader(colors),
            Expanded(
              child: FutureBuilder<List<AvailableDeliveryModel>>(
                future: _deliveriesFuture,
                builder: (context, snapshot) {
                  if (snapshot.connectionState == ConnectionState.waiting) {
                    return const Center(child: CircularProgressIndicator());
                  }
                  if (snapshot.hasError) {
                    return _buildErrorState(colors, snapshot.error);
                  }
                  final deliveries = snapshot.data ?? const [];
                  if (deliveries.isEmpty) return _buildEmptyState(colors);
                  return RefreshIndicator(
                    onRefresh: () async => _reload(),
                    child: ListView.separated(
                      padding: const EdgeInsets.all(16),
                      itemCount: deliveries.length,
                      separatorBuilder: (_, _) => const SizedBox(height: 14),
                      itemBuilder: (context, index) {
                        final delivery = deliveries[index];
                        return AvailableDeliveryCard(delivery: delivery, onOpen: () => _openDelivery(delivery));
                      },
                    ),
                  );
                },
              ),
            ),
          ],
        ),
      ),
      bottomNavigationBar: CourierBottomNavigation(currentTab: CourierTab.deliveries, onTabSelected: _onTabSelected),
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
            onPressed: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const NotificationsScreen())),
          ),
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
            Icon(Icons.local_shipping_outlined, size: 48, color: colors.textMuted),
            const SizedBox(height: 12),
            Text('Aucune livraison assignée pour le moment.', style: TextStyle(color: colors.textMuted, fontWeight: FontWeight.w500)),
          ],
        ),
      ),
    );
  }

  Widget _buildErrorState(AppColorScheme colors, Object? error) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.error_outline_rounded, size: 48, color: colors.error),
            const SizedBox(height: 12),
            Text(
              error is ApiException ? error.message : 'Impossible de charger les livraisons.',
              textAlign: TextAlign.center,
              style: TextStyle(color: colors.textMuted),
            ),
            const SizedBox(height: 12),
            TextButton(onPressed: _reload, child: const Text('Réessayer')),
          ],
        ),
      ),
    );
  }
}
