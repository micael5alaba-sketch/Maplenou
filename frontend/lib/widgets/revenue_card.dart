import 'package:flutter/material.dart';

import '../theme/app_color_scheme.dart';
import '../utils/formatters.dart';

/// KPI card: weekly revenue with a week-over-week change indicator.
class RevenueCard extends StatelessWidget {
  final num weeklyRevenue;
  final double changePercent;

  const RevenueCard({
    super.key,
    required this.weeklyRevenue,
    required this.changePercent,
  });

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    final isPositive = changePercent >= 0;
    // A decline still uses the brand orange (an alert color here), not red —
    // matches the "orange pour les indicateurs et alertes" brief.
    final changeColor = isPositive ? colors.primary : colors.accentOrange;

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
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              color: colors.primary.withValues(alpha: 0.1),
              borderRadius: BorderRadius.circular(14),
            ),
            child: Icon(Icons.payments_rounded, color: colors.primary),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Chiffre d\'affaires (Semaine)',
                  style: TextStyle(fontSize: 13, color: colors.textMuted, fontWeight: FontWeight.w500),
                ),
                const SizedBox(height: 6),
                Text(
                  formatFcfa(weeklyRevenue),
                  style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: colors.textDark),
                ),
                const SizedBox(height: 6),
                Row(
                  children: [
                    Icon(
                      isPositive ? Icons.arrow_upward_rounded : Icons.arrow_downward_rounded,
                      size: 15,
                      color: changeColor,
                    ),
                    const SizedBox(width: 2),
                    Text(
                      '${isPositive ? '+' : ''}${changePercent.toStringAsFixed(1)}%',
                      style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: changeColor),
                    ),
                    const SizedBox(width: 6),
                    Flexible(
                      child: Text(
                        'vs semaine dernière',
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(fontSize: 12, color: colors.textMuted),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
