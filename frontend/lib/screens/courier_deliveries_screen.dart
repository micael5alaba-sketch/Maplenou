import 'package:flutter/material.dart';

import '../models/available_delivery_model.dart';
import '../services/available_deliveries_service.dart';
import '../theme/app_color_scheme.dart';
import '../widgets/app_drawer.dart';
import '../widgets/app_logo.dart';
import '../widgets/available_delivery_card.dart';
import '../widgets/courier_bottom_navigation.dart';
import 'courier_delivery_detail_screen.dart';
import 'courier_messages_screen.dart';
import 'courier_profile_screen.dart';

/// "Courses" tab: feed of delivery requests the courier can accept.
///
/// Backed by [AvailableDeliveriesService] mocked data for now — no network
/// call, no live/polling feed either (see that service's doc comment).
class CourierDeliveriesScreen extends StatefulWidget {
  const CourierDeliveriesScreen({super.key});

  @override
  State<CourierDeliveriesScreen> createState() => _CourierDeliveriesScreenState();
}

class _CourierDeliveriesScreenState extends State<CourierDeliveriesScreen> {
  final _scaffoldKey = GlobalKey<ScaffoldState>();
  final _service = AvailableDeliveriesService();
  late List<AvailableDeliveryModel> _deliveries;

  @override
  void initState() {
    super.initState();
    _deliveries = _service.getAvailableDeliveries();
  }

  void _showComingSoon(String feature) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text('$feature bientôt disponible.')));
  }

  void _acceptDelivery(AvailableDeliveryModel delivery) {
    setState(() => _deliveries.removeWhere((d) => d.id == delivery.id));
    Navigator.of(context).push(MaterialPageRoute(builder: (_) => const CourierDeliveryDetailScreen()));
  }

  void _onTabSelected(CourierTab tab) {
    if (tab == CourierTab.deliveries) return;
    switch (tab) {
      case CourierTab.dashboard:
        Navigator.of(context).pop();
        break;
      case CourierTab.deliveries:
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

    return Scaffold(
      key: _scaffoldKey,
      backgroundColor: colors.homeBackground,
      drawer: AppDrawer(onComingSoon: _showComingSoon),
      body: SafeArea(
        child: Column(
          children: [
            _buildHeader(colors),
            Expanded(
              child: _deliveries.isEmpty
                  ? _buildEmptyState(colors)
                  : ListView.separated(
                      padding: const EdgeInsets.all(16),
                      itemCount: _deliveries.length + 1,
                      separatorBuilder: (_, _) => const SizedBox(height: 14),
                      itemBuilder: (context, index) {
                        if (index == _deliveries.length) {
                          return Padding(
                            padding: const EdgeInsets.symmetric(vertical: 12),
                            child: Column(
                              children: [
                                SizedBox(
                                  width: 18,
                                  height: 18,
                                  child: CircularProgressIndicator(strokeWidth: 2, color: colors.textMuted),
                                ),
                                const SizedBox(height: 8),
                                Text('Actualisation en direct...', style: TextStyle(fontSize: 12, color: colors.textMuted)),
                              ],
                            ),
                          );
                        }
                        final delivery = _deliveries[index];
                        return AvailableDeliveryCard(delivery: delivery, onAccept: () => _acceptDelivery(delivery));
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
            onPressed: () => _showComingSoon('Notifications'),
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
            Text('Aucune course disponible pour le moment.', style: TextStyle(color: colors.textMuted, fontWeight: FontWeight.w500)),
          ],
        ),
      ),
    );
  }
}
