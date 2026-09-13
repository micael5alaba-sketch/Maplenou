import 'package:flutter/material.dart';

import '../models/my_order_summary_model.dart';
import '../theme/app_color_scheme.dart';
import '../utils/formatters.dart';

/// One row in [MyOrdersScreen].
class MyOrderCard extends StatelessWidget {
  final MyOrderSummaryModel order;
  final VoidCallback onTap;

  const MyOrderCard({super.key, required this.order, required this.onTap});

  Color _statusColor(AppColorScheme colors) {
    switch (order.status) {
      case MyOrderStatus.inProgress:
        return colors.accentOrange;
      case MyOrderStatus.delivered:
        return colors.primary;
      case MyOrderStatus.cancelled:
        return colors.error;
    }
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    final statusColor = _statusColor(colors);

    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(16),
      child: Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: colors.surface,
          borderRadius: BorderRadius.circular(16),
          boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 10, offset: const Offset(0, 3))],
        ),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Text('#${order.orderNumber}', style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
                      const SizedBox(width: 8),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                        decoration: BoxDecoration(color: statusColor.withValues(alpha: 0.12), borderRadius: BorderRadius.circular(20)),
                        child: Text(order.status.label, style: TextStyle(color: statusColor, fontSize: 10, fontWeight: FontWeight.w700)),
                      ),
                    ],
                  ),
                  const SizedBox(height: 4),
                  Text(
                    '${order.itemCount} article${order.itemCount > 1 ? 's' : ''} • ${formatOrderDateTime(order.dateTime)}',
                    style: TextStyle(fontSize: 12, color: colors.textMuted),
                  ),
                ],
              ),
            ),
            Text(formatFcfa(order.total), style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
            Icon(Icons.chevron_right_rounded, color: colors.textMuted),
          ],
        ),
      ),
    );
  }
}
