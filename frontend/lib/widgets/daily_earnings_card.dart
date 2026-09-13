import 'package:flutter/material.dart';

import '../theme/app_color_scheme.dart';
import '../utils/formatters.dart';

/// "Gains du jour" card: today's earnings with a progress bar toward the
/// daily goal.
class DailyEarningsCard extends StatelessWidget {
  final num earnings;
  final num goal;

  const DailyEarningsCard({super.key, required this.earnings, required this.goal});

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    final progress = goal > 0 ? (earnings / goal).clamp(0, 1).toDouble() : 0.0;

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
            children: [
              Icon(Icons.account_balance_wallet_rounded, size: 16, color: colors.primary),
              const SizedBox(width: 8),
              Text('GAINS DU JOUR', style: TextStyle(fontSize: 12, fontWeight: FontWeight.w700, color: colors.textMuted)),
            ],
          ),
          const SizedBox(height: 10),
          Text(formatFcfa(earnings), style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: colors.primary)),
          const SizedBox(height: 12),
          ClipRRect(
            borderRadius: BorderRadius.circular(8),
            child: LinearProgressIndicator(
              value: progress,
              minHeight: 8,
              backgroundColor: colors.inputFill,
              valueColor: AlwaysStoppedAnimation(colors.primary),
            ),
          ),
          const SizedBox(height: 8),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text('Objectif: ${formatFcfa(goal)}', style: TextStyle(fontSize: 12, color: colors.textMuted)),
              Text('${(progress * 100).round()}%', style: TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: colors.textDark)),
            ],
          ),
        ],
      ),
    );
  }
}
