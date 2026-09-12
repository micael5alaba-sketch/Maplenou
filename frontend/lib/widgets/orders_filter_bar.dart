import 'package:flutter/material.dart';

import '../models/vendor_order_model.dart';
import 'filter_pill.dart';

/// Horizontal, scrollable row of status filter chips — single selection.
/// `null` in [selected]/[onSelected] represents "Toutes" (no filter),
/// shown as the first chip so the seller can always get back to the full
/// list.
class OrdersFilterBar extends StatelessWidget {
  final VendorOrderStatus? selected;
  final ValueChanged<VendorOrderStatus?> onSelected;

  const OrdersFilterBar({
    super.key,
    required this.selected,
    required this.onSelected,
  });

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: Row(
        children: [
          FilterPill(
            label: 'Toutes',
            isActive: selected == null,
            onTap: () => onSelected(null),
          ),
          for (final status in VendorOrderStatus.values) ...[
            const SizedBox(width: 8),
            FilterPill(
              label: status.filterLabel,
              isActive: selected == status,
              onTap: () => onSelected(status),
            ),
          ],
        ],
      ),
    );
  }
}
