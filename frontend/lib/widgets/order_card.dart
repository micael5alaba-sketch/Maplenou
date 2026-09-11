import 'package:flutter/material.dart';

import '../models/vendor_order_model.dart';
import '../theme/app_color_scheme.dart';
import '../utils/formatters.dart';
import 'order_status_chip.dart';

/// One order in the seller's order list: date/number, status badge,
/// customer, product summary, amount, and the actions relevant to its
/// current status.
class OrderCard extends StatelessWidget {
  final VendorOrderModel order;
  final VoidCallback onViewDetails;

  /// Called when the seller taps the primary action button — only
  /// relevant for [VendorOrderStatus.toPrepare] (mark ready to ship) and
  /// [VendorOrderStatus.shipping] (mark delivered). Null for statuses that
  /// have no further action.
  final VoidCallback? onPrimaryAction;

  const OrderCard({
    super.key,
    required this.order,
    required this.onViewDetails,
    this.onPrimaryAction,
  });

  String? get _primaryActionLabel {
    switch (order.status) {
      case VendorOrderStatus.toPrepare:
        return 'Prêt pour livraison';
      case VendorOrderStatus.shipping:
        return 'Marquer comme livrée';
      case VendorOrderStatus.delivered:
      case VendorOrderStatus.cancelled:
        // Commande terminée : plus aucune action, seul "Détails" a du sens.
        return null;
    }
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    final hasPrimaryAction = _primaryActionLabel != null;

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
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      formatOrderDateTime(order.dateTime),
                      style: TextStyle(fontSize: 12, color: colors.textMuted),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      order.orderNumber,
                      style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark),
                    ),
                  ],
                ),
              ),
              OrderStatusChip(status: order.status),
            ],
          ),
          const SizedBox(height: 14),
          Row(
            children: [
              Icon(Icons.person_outline_rounded, size: 18, color: colors.textMuted),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  order.customerName,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(color: colors.textDark, fontWeight: FontWeight.w500),
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              Icon(Icons.inventory_2_outlined, size: 18, color: colors.textMuted),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  order.productSummary,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(color: colors.textDark, fontWeight: FontWeight.w500),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Text(
            formatFcfa(order.amount),
            style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: colors.primary),
          ),
          const SizedBox(height: 14),
          Row(
            children: [
              Expanded(
                child: SizedBox(
                  height: 44,
                  child: OutlinedButton(
                    onPressed: onViewDetails,
                    style: OutlinedButton.styleFrom(
                      foregroundColor: colors.primary,
                      side: BorderSide(color: colors.primary, width: 1.3),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                    ),
                    child: const Text('Détails', style: TextStyle(fontWeight: FontWeight.w600)),
                  ),
                ),
              ),
              if (hasPrimaryAction) ...[
                const SizedBox(width: 10),
                Expanded(
                  child: SizedBox(
                    height: 44,
                    child: ElevatedButton(
                      onPressed: onPrimaryAction,
                      style: ElevatedButton.styleFrom(
                        backgroundColor: colors.accentOrange,
                        foregroundColor: Colors.white,
                        elevation: 0,
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                      ),
                      child: Text(
                        _primaryActionLabel!,
                        textAlign: TextAlign.center,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13),
                      ),
                    ),
                  ),
                ),
              ],
            ],
          ),
        ],
      ),
    );
  }
}
