import 'package:flutter/material.dart';

import '../models/my_order_summary_model.dart';
import '../services/api_client.dart';
import '../services/my_orders_service.dart';
import '../theme/app_color_scheme.dart';
import '../widgets/my_order_card.dart';
import 'order_tracking_screen.dart';

/// "Mes Commandes" — reached from [ProfileScreen]'s activity section.
///
/// Backed by [MyOrdersService] (`GET /api/orders`).
class MyOrdersScreen extends StatefulWidget {
  const MyOrdersScreen({super.key});

  @override
  State<MyOrdersScreen> createState() => _MyOrdersScreenState();
}

class _MyOrdersScreenState extends State<MyOrdersScreen> {
  final _service = MyOrdersService();
  late Future<List<MyOrderSummaryModel>> _ordersFuture;

  @override
  void initState() {
    super.initState();
    _ordersFuture = _service.getMyOrders();
  }

  void _reload() {
    setState(() => _ordersFuture = _service.getMyOrders());
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return Scaffold(
      backgroundColor: colors.homeBackground,
      body: SafeArea(
        child: Column(
          children: [
            _buildHeader(context, colors),
            Expanded(
              child: FutureBuilder<List<MyOrderSummaryModel>>(
                future: _ordersFuture,
                builder: (context, snapshot) {
                  if (snapshot.connectionState == ConnectionState.waiting) {
                    return const Center(child: CircularProgressIndicator());
                  }
                  if (snapshot.hasError) {
                    return _buildErrorState(colors, snapshot.error);
                  }
                  final orders = snapshot.data ?? const [];
                  if (orders.isEmpty) return _buildEmptyState(colors);
                  return ListView.separated(
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
                            MaterialPageRoute(builder: (_) => OrderTrackingScreen(orderId: order.orderId)),
                          );
                        },
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
              error is ApiException ? error.message : 'Impossible de charger vos commandes.',
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
