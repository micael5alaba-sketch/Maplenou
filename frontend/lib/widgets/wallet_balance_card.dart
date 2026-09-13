import 'package:flutter/material.dart';

import '../theme/app_color_scheme.dart';
import '../utils/formatters.dart';

/// Hero balance card at the top of [WalletScreen]: centered wallet icon,
/// available balance, a month-over-month change pill, and the "Demander un
/// retrait" call to action.
///
/// Deliberately a distinct layout from [BalanceCard] (centered/stacked
/// rather than a horizontal KPI row) to match this screen's hero treatment
/// in the design.
class WalletBalanceCard extends StatelessWidget {
  final num availableBalance;
  final double changePercent;
  final VoidCallback onWithdraw;

  const WalletBalanceCard({
    super.key,
    required this.availableBalance,
    required this.changePercent,
    required this.onWithdraw,
  });

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    final isPositive = changePercent >= 0;

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 12, offset: const Offset(0, 4)),
        ],
      ),
      child: Column(
        children: [
          Icon(Icons.account_balance_wallet_rounded, size: 30, color: colors.primary),
          const SizedBox(height: 10),
          Text('Solde disponible', style: TextStyle(fontSize: 13, color: colors.textMuted, fontWeight: FontWeight.w500)),
          const SizedBox(height: 6),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.baseline,
            textBaseline: TextBaseline.alphabetic,
            children: [
              Text(
                formatFcfa(availableBalance),
                style: TextStyle(fontSize: 26, fontWeight: FontWeight.bold, color: colors.textDark),
              ),
            ],
          ),
          const SizedBox(height: 10),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 5),
            decoration: BoxDecoration(
              color: colors.primary.withValues(alpha: 0.1),
              borderRadius: BorderRadius.circular(20),
            ),
            child: Text(
              '${isPositive ? '+' : ''}${changePercent.toStringAsFixed(1)}% ce mois-ci',
              style: TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: colors.primary),
            ),
          ),
          const SizedBox(height: 18),
          SizedBox(
            width: double.infinity,
            child: ElevatedButton.icon(
              onPressed: onWithdraw,
              style: ElevatedButton.styleFrom(
                backgroundColor: colors.primary,
                foregroundColor: Colors.white,
                elevation: 0,
                padding: const EdgeInsets.symmetric(vertical: 14),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
              ),
              icon: const Icon(Icons.account_balance_outlined),
              label: const Text('Demander un retrait', style: TextStyle(fontWeight: FontWeight.w600)),
            ),
          ),
        ],
      ),
    );
  }
}
