import 'package:flutter/material.dart';

import '../models/available_delivery_model.dart';
import '../theme/app_color_scheme.dart';
import '../utils/formatters.dart';

/// One assigned delivery in [CourierDeliveriesScreen]'s list: pickup and
/// dropoff points, distance, and a button to open the delivery's detail.
class AvailableDeliveryCard extends StatelessWidget {
  final AvailableDeliveryModel delivery;
  final VoidCallback onOpen;

  const AvailableDeliveryCard({super.key, required this.delivery, required this.onOpen});

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
              Text('Assignée', style: TextStyle(fontSize: 12, fontWeight: FontWeight.w700, color: colors.accentOrange)),
              Text(formatRelativeDate(delivery.assignedAt), style: TextStyle(fontSize: 12, color: colors.textMuted)),
            ],
          ),
          const Divider(height: 20),
          _buildPoint(colors, colors.accentOrange, 'Point de collecte', delivery.pickupLabel, delivery.pickupAddress),
          const SizedBox(height: 10),
          _buildPoint(colors, colors.primary, 'Point de livraison', delivery.dropoffLabel, delivery.dropoffAddress),
          if (delivery.distanceKm != null || delivery.estimatedEarning != null) ...[
            const SizedBox(height: 10),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                if (delivery.distanceKm != null)
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                    decoration: BoxDecoration(color: colors.inputFill, borderRadius: BorderRadius.circular(10)),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(Icons.straighten_rounded, size: 14, color: colors.textMuted),
                        const SizedBox(width: 6),
                        Text('${delivery.distanceKm!.toStringAsFixed(1)} km à vol d\'oiseau', style: TextStyle(fontSize: 12, color: colors.textDark)),
                      ],
                    ),
                  ),
                if (delivery.estimatedEarning != null)
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                    decoration: BoxDecoration(color: colors.primary.withValues(alpha: 0.1), borderRadius: BorderRadius.circular(10)),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(Icons.payments_outlined, size: 14, color: colors.primary),
                        const SizedBox(width: 6),
                        Text(formatFcfa(delivery.estimatedEarning!), style: TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: colors.primary)),
                      ],
                    ),
                  ),
              ],
            ),
          ],
          const SizedBox(height: 14),
          SizedBox(
            width: double.infinity,
            height: 46,
            child: ElevatedButton(
              onPressed: onOpen,
              style: ElevatedButton.styleFrom(
                backgroundColor: colors.primary,
                foregroundColor: Colors.white,
                elevation: 0,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              ),
              child: const Text('Voir les détails', style: TextStyle(fontWeight: FontWeight.w600)),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildPoint(AppColorScheme colors, Color dotColor, String label, String place, String address) {
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
              Text(place, style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark, fontSize: 13)),
              Text(address, style: TextStyle(fontSize: 12, color: colors.textMuted)),
            ],
          ),
        ),
      ],
    );
  }
}
