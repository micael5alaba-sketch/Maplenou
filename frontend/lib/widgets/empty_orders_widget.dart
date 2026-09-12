import 'package:flutter/material.dart';

import '../theme/app_color_scheme.dart';

/// Shown when the current filter has no matching orders.
class EmptyOrdersWidget extends StatelessWidget {
  const EmptyOrdersWidget({super.key});

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 48),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.receipt_long_outlined, size: 48, color: colors.textMuted),
          const SizedBox(height: 12),
          Text(
            "Vous n'avez aucune commande.",
            style: TextStyle(color: colors.textMuted, fontWeight: FontWeight.w500),
          ),
        ],
      ),
    );
  }
}
