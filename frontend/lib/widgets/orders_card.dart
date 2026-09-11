import 'package:flutter/material.dart';

import '../theme/app_color_scheme.dart';

/// KPI card: how many orders are currently in progress, and how many of
/// those are ready to ship.
class OrdersCard extends StatelessWidget {
  final int ongoingOrdersCount;
  final int readyToShipCount;
  final VoidCallback? onTap;

  const OrdersCard({
    super.key,
    required this.ongoingOrdersCount,
    required this.readyToShipCount,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.all(18),
        decoration: BoxDecoration(
          color: colors.surface,
          borderRadius: BorderRadius.circular(20),
          boxShadow: [
            BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 12, offset: const Offset(0, 4)),
          ],
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: colors.accentOrange.withValues(alpha: 0.12),
                borderRadius: BorderRadius.circular(14),
              ),
              child: Icon(Icons.local_shipping_rounded, color: colors.accentOrange),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Commandes en cours',
                    style: TextStyle(fontSize: 13, color: colors.textMuted, fontWeight: FontWeight.w500),
                  ),
                  const SizedBox(height: 6),
                  Text(
                    '$ongoingOrdersCount',
                    style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: colors.textDark),
                  ),
                  const SizedBox(height: 6),
                  Text(
                    '$readyToShipCount prêtes à expédier',
                    style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: colors.accentOrange),
                  ),
                ],
              ),
            ),
            Icon(Icons.chevron_right_rounded, color: colors.textMuted),
          ],
        ),
      ),
    );
  }
}
