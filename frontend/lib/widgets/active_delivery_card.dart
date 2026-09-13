import 'package:flutter/material.dart';

import '../models/courier_dashboard_model.dart';
import '../theme/app_color_scheme.dart';
import '../utils/formatters.dart';

/// One "Courses Actives" card on [CourierDashboardScreen]: either "À
/// collecter" (heading to the shop) or "En route" (heading to the client,
/// cash to collect + a confirm button).
class ActiveDeliveryCard extends StatelessWidget {
  final ActiveDeliveryModel delivery;
  final VoidCallback onTap;
  final VoidCallback? onConfirm;

  const ActiveDeliveryCard({super.key, required this.delivery, required this.onTap, this.onConfirm});

  bool get _isEnRoute => delivery.status == ActiveDeliveryStatus.enRoute;

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    final statusColor = _isEnRoute ? colors.primary : colors.accentOrange;

    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(18),
      child: Container(
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
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                  decoration: BoxDecoration(color: statusColor.withValues(alpha: 0.12), borderRadius: BorderRadius.circular(20)),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(_isEnRoute ? Icons.pedal_bike_rounded : Icons.storefront_rounded, size: 14, color: statusColor),
                      const SizedBox(width: 4),
                      Text(
                        _isEnRoute ? 'En route' : 'À collecter',
                        style: TextStyle(color: statusColor, fontSize: 11, fontWeight: FontWeight.w700),
                      ),
                    ],
                  ),
                ),
                if (_isEnRoute && delivery.amountToCollect != null)
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: [
                      Text('À encaisser', style: TextStyle(fontSize: 11, color: colors.textMuted)),
                      Text(
                        '${formatFcfa(delivery.amountToCollect!)} (Espèces)',
                        style: TextStyle(fontSize: 12, fontWeight: FontWeight.w700, color: colors.accentOrange),
                      ),
                    ],
                  )
                else
                  Text('${delivery.distanceKm} km • ${delivery.etaMinutes} min',
                      style: TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: colors.textDark)),
              ],
            ),
            const SizedBox(height: 10),
            Row(
              children: [
                Icon(_isEnRoute ? Icons.person_outline_rounded : Icons.location_on_outlined, size: 18, color: colors.textMuted),
                const SizedBox(width: 6),
                Expanded(
                  child: Text(delivery.pickupOrClientLabel, style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark)),
                ),
              ],
            ),
            const SizedBox(height: 2),
            Text(delivery.addressLine, style: TextStyle(fontSize: 12, color: colors.textMuted)),
            const SizedBox(height: 12),
            SizedBox(
              width: double.infinity,
              height: 42,
              child: _isEnRoute
                  ? ElevatedButton.icon(
                      onPressed: onConfirm ?? onTap,
                      style: ElevatedButton.styleFrom(
                        backgroundColor: colors.primary,
                        foregroundColor: Colors.white,
                        elevation: 0,
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                      ),
                      icon: const Icon(Icons.check_circle_outline_rounded, size: 18),
                      label: const Text('Confirmer la livraison', style: TextStyle(fontWeight: FontWeight.w600)),
                    )
                  : OutlinedButton.icon(
                      onPressed: onTap,
                      style: OutlinedButton.styleFrom(
                        foregroundColor: colors.primary,
                        side: BorderSide(color: colors.primary),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                      ),
                      icon: const Icon(Icons.directions_rounded, size: 18),
                      label: const Text('Itinéraire', style: TextStyle(fontWeight: FontWeight.w600)),
                    ),
            ),
          ],
        ),
      ),
    );
  }
}
