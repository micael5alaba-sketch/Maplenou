import 'package:flutter/material.dart';

import '../models/active_delivery_detail_model.dart';
import '../services/active_delivery_service.dart';
import '../theme/app_color_scheme.dart';
import '../utils/formatters.dart';
import 'courier_delivery_confirmation_screen.dart';

/// Detail of the delivery currently in progress: a map placeholder (no
/// live GPS feed — see [ActiveDeliveryService]), the client's info,
/// delivery address and order content, plus a call/message shortcut.
///
/// Reached from [CourierDashboardScreen] / [CourierDeliveriesScreen].
class CourierDeliveryDetailScreen extends StatelessWidget {
  const CourierDeliveryDetailScreen({super.key});

  void _showComingSoon(BuildContext context, String feature) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text('$feature bientôt disponible.')));
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    final delivery = ActiveDeliveryService().getActiveDelivery('current');

    return Scaffold(
      backgroundColor: colors.homeBackground,
      body: SafeArea(
        child: Column(
          children: [
            _buildHeader(context, colors),
            Expanded(
              child: ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  _buildMapPlaceholder(colors, delivery.etaMinutes),
                  const SizedBox(height: 16),
                  _buildClientCard(context, colors, delivery),
                  const SizedBox(height: 16),
                  _buildAddressCard(colors, delivery),
                  const SizedBox(height: 16),
                  _buildOrderCard(colors, delivery),
                  const SizedBox(height: 20),
                  SizedBox(
                    width: double.infinity,
                    height: 54,
                    child: ElevatedButton.icon(
                      onPressed: () => Navigator.of(context).pushReplacement(
                        MaterialPageRoute(builder: (_) => const CourierDeliveryConfirmationScreen()),
                      ),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: colors.primary,
                        foregroundColor: Colors.white,
                        elevation: 0,
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28)),
                      ),
                      icon: const Icon(Icons.check_circle_outline_rounded),
                      label: const Text('Confirmer la livraison', style: TextStyle(fontWeight: FontWeight.w600)),
                    ),
                  ),
                ],
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
          IconButton(
            icon: Icon(Icons.arrow_back_rounded, color: colors.textDark),
            onPressed: () => Navigator.of(context).pop(),
          ),
          Expanded(
            child: Text('Livraison en cours', style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
          ),
          IconButton(
            icon: Icon(Icons.more_vert_rounded, color: colors.textDark),
            onPressed: () => _showComingSoon(context, 'Plus d\'options'),
          ),
        ],
      ),
    );
  }

  Widget _buildMapPlaceholder(AppColorScheme colors, int etaMinutes) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(20),
      child: Container(
        height: 200,
        color: colors.inputFill,
        child: Stack(
          children: [
            Center(child: Icon(Icons.map_outlined, size: 48, color: colors.textMuted)),
            Positioned(
              top: 12,
              left: 0,
              right: 0,
              child: Center(
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                  decoration: BoxDecoration(color: colors.surface, borderRadius: BorderRadius.circular(20)),
                  child: Text(
                    'En route - $etaMinutes min',
                    style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark, fontSize: 13),
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildClientCard(BuildContext context, AppColorScheme colors, ActiveDeliveryDetailModel delivery) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 10, offset: const Offset(0, 3))],
      ),
      child: Row(
        children: [
          CircleAvatar(
            radius: 22,
            backgroundColor: colors.inputFill,
            child: Text(delivery.clientName[0], style: TextStyle(fontWeight: FontWeight.bold, color: colors.primary)),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(delivery.clientName, style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
                Text(
                  'Client • ${delivery.clientOrdersCount} commandes',
                  style: TextStyle(fontSize: 12, color: colors.textMuted),
                ),
              ],
            ),
          ),
          IconButton(
            icon: Icon(Icons.phone_rounded, color: colors.primary),
            onPressed: () => _showComingSoon(context, "L'appel"),
          ),
        ],
      ),
    );
  }

  Widget _buildAddressCard(AppColorScheme colors, ActiveDeliveryDetailModel delivery) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 10, offset: const Offset(0, 3))],
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.location_on_outlined, color: colors.primary),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('ADRESSE DE LIVRAISON', style: TextStyle(fontSize: 11, fontWeight: FontWeight.w700, color: colors.textMuted)),
                const SizedBox(height: 4),
                Text(delivery.deliveryAddress, style: TextStyle(color: colors.textDark, height: 1.3)),
                if (delivery.doorCode != null) ...[
                  const SizedBox(height: 4),
                  Text('Code porte : ${delivery.doorCode}', style: TextStyle(color: colors.accentOrange, fontWeight: FontWeight.w600, fontSize: 12)),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildOrderCard(AppColorScheme colors, ActiveDeliveryDetailModel delivery) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 10, offset: const Offset(0, 3))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(Icons.receipt_long_outlined, size: 16, color: colors.textMuted),
              const SizedBox(width: 6),
              Text('Détails de la commande (${delivery.orderNumber})', style: TextStyle(fontSize: 12, color: colors.textMuted)),
            ],
          ),
          const SizedBox(height: 10),
          for (final item in delivery.items) ...[
            Container(
              margin: const EdgeInsets.only(bottom: 8),
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
              decoration: BoxDecoration(color: colors.inputFill, borderRadius: BorderRadius.circular(10)),
              child: Text(item, style: TextStyle(color: colors.textDark)),
            ),
          ],
          const Divider(),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text('Montant à encaisser', style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark)),
              Text(formatFcfa(delivery.amountToCollect), style: TextStyle(fontWeight: FontWeight.bold, color: colors.primary)),
            ],
          ),
        ],
      ),
    );
  }
}
