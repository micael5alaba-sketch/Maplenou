import 'package:flutter/material.dart';

import '../models/wallet_model.dart';
import '../theme/app_color_scheme.dart';
import '../utils/formatters.dart';

/// "Historique des retraits" card on [WalletScreen]: a title, a "Tout voir"
/// shortcut, and one row per withdrawal (date, amount, method, status dot).
class WithdrawalHistorySection extends StatelessWidget {
  final List<WithdrawalModel> withdrawals;
  final VoidCallback onSeeAll;

  const WithdrawalHistorySection({
    super.key,
    required this.withdrawals,
    required this.onSeeAll,
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
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text('Historique des retraits', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: colors.textDark)),
              GestureDetector(
                onTap: onSeeAll,
                child: Text('Tout voir', style: TextStyle(color: colors.primary, fontWeight: FontWeight.w600, fontSize: 13)),
              ),
            ],
          ),
          const SizedBox(height: 12),
          for (var i = 0; i < withdrawals.length; i++) ...[
            _buildRow(colors, withdrawals[i]),
            if (i != withdrawals.length - 1) Divider(height: 20, color: colors.border),
          ],
        ],
      ),
    );
  }

  Widget _buildRow(AppColorScheme colors, WithdrawalModel withdrawal) {
    final isPending = withdrawal.status == WithdrawalStatus.pending;

    return Row(
      children: [
        Expanded(
          child: Text(
            formatOrderDateTime(withdrawal.date).split(' • ').first,
            style: TextStyle(fontSize: 13, color: colors.textMuted),
          ),
        ),
        Expanded(
          flex: 2,
          child: Text(
            formatFcfa(withdrawal.amount),
            style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark),
          ),
        ),
        Expanded(
          flex: 2,
          child: Text(
            withdrawal.method,
            overflow: TextOverflow.ellipsis,
            style: TextStyle(fontSize: 13, color: colors.textMuted),
          ),
        ),
        Icon(
          isPending ? Icons.access_time_rounded : Icons.check_circle_rounded,
          size: 18,
          color: isPending ? colors.accentOrange : colors.primary,
        ),
      ],
    );
  }
}
