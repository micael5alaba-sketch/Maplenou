import 'package:flutter/material.dart';

import '../theme/app_color_scheme.dart';
import '../utils/formatters.dart';

/// KPI card: available balance with a "Retirer" (withdraw) action.
class BalanceCard extends StatelessWidget {
  final num availableBalance;
  final VoidCallback onWithdraw;

  const BalanceCard({
    super.key,
    required this.availableBalance,
    required this.onWithdraw,
  });

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 12, offset: const Offset(0, 4)),
        ],
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              color: colors.primary.withValues(alpha: 0.1),
              borderRadius: BorderRadius.circular(14),
            ),
            child: Icon(Icons.account_balance_wallet_rounded, color: colors.primary),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Solde disponible',
                  style: TextStyle(fontSize: 13, color: colors.textMuted, fontWeight: FontWeight.w500),
                ),
                const SizedBox(height: 6),
                Text(
                  formatFcfa(availableBalance),
                  style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: colors.textDark),
                ),
              ],
            ),
          ),
          const SizedBox(width: 12),
          ElevatedButton(
            onPressed: onWithdraw,
            style: ElevatedButton.styleFrom(
              backgroundColor: colors.primary,
              foregroundColor: Colors.white,
              elevation: 0,
              padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 12),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
            ),
            child: const Text('Retirer', style: TextStyle(fontWeight: FontWeight.w600)),
          ),
        ],
      ),
    );
  }
}
