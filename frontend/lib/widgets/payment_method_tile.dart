import 'package:flutter/material.dart';

import '../models/wallet_model.dart';
import '../theme/app_color_scheme.dart';

/// One registered payout method on [WalletScreen] ("T-Money ••• 89",
/// "Flooz ••• 42"...), with a "Défaut" badge on the default one.
class PaymentMethodTile extends StatelessWidget {
  final PaymentMethodModel method;

  const PaymentMethodTile({super.key, required this.method});

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      decoration: BoxDecoration(
        color: method.isDefault ? colors.primary.withValues(alpha: 0.06) : colors.inputFill,
        borderRadius: BorderRadius.circular(14),
        border: method.isDefault ? Border.all(color: colors.primary.withValues(alpha: 0.3)) : null,
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(color: colors.surface, borderRadius: BorderRadius.circular(10)),
            child: Icon(Icons.phone_iphone_rounded, size: 18, color: colors.primary),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(method.name, style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark)),
                const SizedBox(height: 2),
                Text(method.maskedNumber, style: TextStyle(fontSize: 12, color: colors.textMuted)),
              ],
            ),
          ),
          if (method.isDefault)
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
              decoration: BoxDecoration(color: colors.primary, borderRadius: BorderRadius.circular(20)),
              child: const Text('Défaut', style: TextStyle(color: Colors.white, fontSize: 11, fontWeight: FontWeight.w600)),
            ),
        ],
      ),
    );
  }
}
