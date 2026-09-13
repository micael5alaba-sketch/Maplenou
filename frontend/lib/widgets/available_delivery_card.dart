import 'package:flutter/material.dart';

import '../models/available_delivery_model.dart';
import '../theme/app_color_scheme.dart';
import '../utils/formatters.dart';

/// One request in [CourierDeliveriesScreen]'s feed: payout amount, pickup
/// and dropoff points, category, and an "Accepter la course" button.
class AvailableDeliveryCard extends StatelessWidget {
  final AvailableDeliveryModel delivery;
  final VoidCallback onAccept;

  const AvailableDeliveryCard({super.key, required this.delivery, required this.onAccept});

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(18),
        boxShadow: [
          BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 12, offset: const Offset(0, 4)),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(formatFcfa(delivery.amount), style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: colors.primary)),
              Text(formatRelativeDate(delivery.postedAt), style: TextStyle(fontSize: 12, color: colors.textMuted)),
            ],
          ),
          const Divider(height: 20),
          _buildPoint(colors, colors.accentOrange, 'Point de collecte', delivery.pickupLabel, delivery.pickupDistanceKm, delivery.pickupAddress),
          const SizedBox(height: 10),
          _buildPoint(colors, colors.primary, 'Point de livraison', delivery.dropoffLabel, delivery.totalDistanceKm, delivery.dropoffAddress, isTotal: true),
          const SizedBox(height: 12),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
            decoration: BoxDecoration(color: colors.inputFill, borderRadius: BorderRadius.circular(10)),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(Icons.inventory_2_outlined, size: 14, color: colors.textMuted),
                const SizedBox(width: 6),
                Text('${delivery.category} • ${delivery.itemsSummary}', style: TextStyle(fontSize: 12, color: colors.textDark)),
              ],
            ),
          ),
          const SizedBox(height: 14),
          SizedBox(
            width: double.infinity,
            height: 46,
            child: ElevatedButton(
              onPressed: onAccept,
              style: ElevatedButton.styleFrom(
                backgroundColor: colors.primary,
                foregroundColor: Colors.white,
                elevation: 0,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              ),
              child: const Text('Accepter la course', style: TextStyle(fontWeight: FontWeight.w600)),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildPoint(AppColorScheme colors, Color dotColor, String label, String place, double distanceKm, String address, {bool isTotal = false}) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(top: 5),
          child: Container(width: 8, height: 8, decoration: BoxDecoration(color: dotColor, shape: BoxShape.circle)),
        ),
        const SizedBox(width: 10),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(label, style: TextStyle(fontSize: 11, color: colors.textMuted)),
              Text(
                '$place • ${distanceKm.toStringAsFixed(1)} km${isTotal ? ' total' : ''}',
                style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark, fontSize: 13),
              ),
              Text(address, style: TextStyle(fontSize: 12, color: colors.textMuted)),
            ],
          ),
        ),
      ],
    );
  }
}
