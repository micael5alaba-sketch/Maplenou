import 'package:flutter/material.dart';

import '../models/vendor_order_model.dart';
import '../theme/app_color_scheme.dart';

/// Colored status badge on an [OrderCard] ("À préparer", "En livraison",
/// "Livrée", "Annulée"). Colors are semantic (like traffic lights) so they
/// stay recognizable regardless of light/dark theme, except where they do
/// come straight from the brand palette (green/orange).
class OrderStatusChip extends StatelessWidget {
  final VendorOrderStatus status;

  const OrderStatusChip({super.key, required this.status});

  Color _color(BuildContext context) {
    final colors = context.colors;
    switch (status) {
      case VendorOrderStatus.toPrepare:
        return colors.accentOrange;
      case VendorOrderStatus.shipping:
        return const Color(0xFF2E6F9E);
      case VendorOrderStatus.delivered:
        return colors.primary;
      case VendorOrderStatus.cancelled:
        return colors.error;
    }
  }

  @override
  Widget build(BuildContext context) {
    final color = _color(context);

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Text(
        status.badgeLabel,
        style: TextStyle(color: color, fontWeight: FontWeight.w600, fontSize: 11),
      ),
    );
  }
}
