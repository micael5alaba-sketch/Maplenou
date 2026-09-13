import 'package:flutter/material.dart';

import '../models/my_order_summary_model.dart';
import '../services/my_orders_service.dart';
import '../theme/app_color_scheme.dart';
import '../widgets/my_order_card.dart';
import 'order_tracking_screen.dart';

/// "Mes Commandes" — reached from [ProfileScreen]'s activity section.
///
/// Backed by [MyOrdersService] mocked data for now — maps directly onto
/// the real, already working `GET /api/orders` once auth is wired up.
class MyOrdersScreen extends StatelessWidget {
  const MyOrdersScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    final orders = MyOrdersService().getMyOrders();

    return Scaffold(
      backgroundColor: colors.homeBackground,
      body: SafeArea(
        child: Column(
          children: [
            _buildHeader(context, colors),
            Expanded(
              child: orders.isEmpty
                  ? _buildEmptyState(colors)
                  : ListView.separated(
                      padding: const EdgeInsets.all(16),
                      itemCount: orders.length,
                      separatorBuilder: (_, _) => const SizedBox(height: 12),
                      itemBuilder: (context, index) {
                        final order = orders[index];
                        return MyOrderCard(
                          order: order,
                          onTap: () {
                            if (order.status != MyOrderStatus.inProgress) return;
                            Navigator.of(context).push(
                              MaterialPageRoute(builder: (_) => OrderTrackingScreen(orderNumber: order.orderNumber)),
                            );
                          },
                        );
                      },
                    ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader(BuildContext context, AppColorScheme colors) {
    return Container(
      color: colors.surface,
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
      child: Row(
        children: [
          IconButton(icon: Icon(Icons.arrow_back_rounded, color: colors.textDark), onPressed: () => Navigator.of(context).pop()),
          Expanded(child: Text('Mes Commandes', textAlign: TextAlign.center, style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark))),
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
            Icon(Icons.inventory_2_outlined, size: 48, color: colors.textMuted),
            const SizedBox(height: 12),
            Text("Vous n'avez pas encore de commande.", style: TextStyle(color: colors.textMuted, fontWeight: FontWeight.w500)),
          ],
        ),
      ),
    );
  }
}
