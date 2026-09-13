import 'package:flutter/material.dart';

import '../models/order_tracking_model.dart';
import '../services/order_tracking_service.dart';
import '../theme/app_color_scheme.dart';

/// "Suivi de commande" — map placeholder, courier info, status timeline,
/// delivery address, and support shortcuts.
///
/// Backed by [OrderTrackingService] mocked data for now — no network call,
/// no live GPS feed (see that service's doc comment).
class OrderTrackingScreen extends StatelessWidget {
  final String orderNumber;

  const OrderTrackingScreen({super.key, required this.orderNumber});

  void _showComingSoon(BuildContext context, String feature) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text('$feature bientôt disponible.')));
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    final tracking = OrderTrackingService().getTracking(orderNumber);

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
                  _buildOrderStatusCard(colors, tracking),
                  const SizedBox(height: 16),
                  _buildMapPlaceholder(colors, tracking.etaLabel),
                  const SizedBox(height: 16),
                  _buildCourierCard(context, colors, tracking),
                  const SizedBox(height: 16),
                  _buildStatusTimeline(colors, tracking),
                  const SizedBox(height: 16),
                  _buildAddressCard(colors, tracking),
                  const SizedBox(height: 16),
                  _buildSupportCard(context, colors),
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
          IconButton(icon: Icon(Icons.arrow_back_rounded, color: colors.textDark), onPressed: () => Navigator.of(context).pop()),
          Expanded(child: Text('Suivi de Livraison', textAlign: TextAlign.center, style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark))),
          const SizedBox(width: 48),
        ],
      ),
    );
  }

  Widget _buildOrderStatusCard(AppColorScheme colors, OrderTrackingModel tracking) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 10, offset: const Offset(0, 3))],
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('COMMANDE', style: TextStyle(fontSize: 11, color: colors.textMuted, fontWeight: FontWeight.w700)),
                Text('#${tracking.orderNumber}', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: colors.textDark)),
                const SizedBox(height: 8),
                Text('Livraison estimée', style: TextStyle(fontSize: 11, color: colors.textMuted)),
                Text(tracking.etaLabel, style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark)),
              ],
            ),
          ),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
            decoration: BoxDecoration(color: colors.primary.withValues(alpha: 0.12), borderRadius: BorderRadius.circular(20)),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(Icons.local_shipping_outlined, size: 14, color: colors.primary),
                const SizedBox(width: 4),
                Text('En cours', style: TextStyle(color: colors.primary, fontSize: 11, fontWeight: FontWeight.w700)),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMapPlaceholder(AppColorScheme colors, String eta) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(20),
      child: Container(
        height: 180,
        color: colors.inputFill,
        child: Center(child: Icon(Icons.map_outlined, size: 48, color: colors.textMuted)),
      ),
    );
  }

  Widget _buildCourierCard(BuildContext context, AppColorScheme colors, OrderTrackingModel tracking) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 10, offset: const Offset(0, 3))],
      ),
      child: Row(
        children: [
          CircleAvatar(radius: 22, backgroundColor: colors.inputFill, child: Text(tracking.courierName[0], style: TextStyle(fontWeight: FontWeight.bold, color: colors.primary))),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Votre livreur', style: TextStyle(fontSize: 11, color: colors.textMuted)),
                Text(tracking.courierName, style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
                Row(
                  children: [
                    const Icon(Icons.star_rounded, color: Colors.amber, size: 14),
                    Text(' ${tracking.courierRating} (${tracking.courierDeliveryCount}+ courses)', style: TextStyle(fontSize: 12, color: colors.textMuted)),
                  ],
                ),
              ],
            ),
          ),
          IconButton(icon: Icon(Icons.chat_bubble_outline_rounded, color: colors.primary), onPressed: () => _showComingSoon(context, 'La messagerie')),
          IconButton(
            icon: Icon(Icons.phone_rounded, color: Colors.white),
            style: IconButton.styleFrom(backgroundColor: colors.primary),
            onPressed: () => _showComingSoon(context, "L'appel"),
          ),
        ],
      ),
    );
  }

  Widget _buildStatusTimeline(AppColorScheme colors, OrderTrackingModel tracking) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 10, offset: const Offset(0, 3))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('État de la commande', style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
          const SizedBox(height: 12),
          for (var i = 0; i < tracking.steps.length; i++)
            _buildStepRow(colors, tracking.steps[i], isLast: i == tracking.steps.length - 1),
        ],
      ),
    );
  }

  Widget _buildStepRow(AppColorScheme colors, OrderTrackingStep step, {required bool isLast}) {
    final color = step.isDone ? colors.primary : (step.isCurrent ? colors.accentOrange : colors.border);

    return IntrinsicHeight(
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Column(
            children: [
              Container(
                width: 16,
                height: 16,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: step.isDone ? colors.primary : Colors.transparent,
                  border: Border.all(color: color, width: 2),
                ),
                child: step.isDone ? const Icon(Icons.check, size: 10, color: Colors.white) : null,
              ),
              if (!isLast) Expanded(child: Container(width: 2, color: colors.border)),
            ],
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Padding(
              padding: const EdgeInsets.only(bottom: 16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    step.label,
                    style: TextStyle(fontWeight: FontWeight.w600, color: step.isCurrent ? colors.accentOrange : colors.textDark),
                  ),
                  if (step.timeLabel.isNotEmpty) Text(step.timeLabel, style: TextStyle(fontSize: 12, color: colors.textMuted)),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildAddressCard(AppColorScheme colors, OrderTrackingModel tracking) {
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
                Text(tracking.deliveryAddress, style: TextStyle(color: colors.textDark, height: 1.3)),
                if (tracking.addressNote != null) ...[
                  const SizedBox(height: 4),
                  Text('Note : ${tracking.addressNote}', style: TextStyle(fontSize: 12, color: colors.textMuted)),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSupportCard(BuildContext context, AppColorScheme colors) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 10, offset: const Offset(0, 3))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text("Besoin d'aide ?", style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
          const SizedBox(height: 12),
          SizedBox(
            width: double.infinity,
            child: ElevatedButton.icon(
              onPressed: () => _showComingSoon(context, 'Le support'),
              style: ElevatedButton.styleFrom(
                backgroundColor: colors.primary,
                foregroundColor: Colors.white,
                elevation: 0,
                padding: const EdgeInsets.symmetric(vertical: 12),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
              ),
              icon: const Icon(Icons.support_agent_rounded, size: 18),
              label: const Text('Contacter le support'),
            ),
          ),
          const SizedBox(height: 8),
          SizedBox(
            width: double.infinity,
            child: OutlinedButton.icon(
              onPressed: () => _showComingSoon(context, 'Le signalement de problème'),
              style: OutlinedButton.styleFrom(
                foregroundColor: colors.textDark,
                side: BorderSide(color: colors.border),
                padding: const EdgeInsets.symmetric(vertical: 12),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
              ),
              icon: const Icon(Icons.report_gmailerrorred_rounded, size: 18),
              label: const Text('Signaler un problème'),
            ),
          ),
        ],
      ),
    );
  }
}
