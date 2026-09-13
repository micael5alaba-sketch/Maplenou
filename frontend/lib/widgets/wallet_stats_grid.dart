import 'package:flutter/material.dart';

import '../theme/app_color_scheme.dart';
import '../utils/formatters.dart';

/// "Statistiques rapides" 2x2 grid on [WalletScreen]: total earned this
/// month, sales count, pending amount, average rating.
class WalletStatsGrid extends StatelessWidget {
  final num totalEarnedMonth;
  final int salesCountMonth;
  final num pendingAmount;
  final double averageRating;

  const WalletStatsGrid({
    super.key,
    required this.totalEarnedMonth,
    required this.salesCountMonth,
    required this.pendingAmount,
    required this.averageRating,
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
          Text('Statistiques rapides', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: colors.textDark)),
          const SizedBox(height: 14),
          Row(
            children: [
              Expanded(
                child: _buildTile(colors, Icons.trending_up_rounded, 'TOTAL GAGNÉ (MOIS)', formatFcfa(totalEarnedMonth)),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _buildTile(colors, Icons.shopping_bag_outlined, 'VENTES (MOIS)', '$salesCountMonth'),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: _buildTile(colors, Icons.hourglass_bottom_rounded, 'EN ATTENTE', formatFcfa(pendingAmount),
                    valueColor: colors.accentOrange),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _buildTile(colors, Icons.star_rounded, 'NOTE MOY.', averageRating.toStringAsFixed(1)),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildTile(AppColorScheme colors, IconData icon, String label, String value, {Color? valueColor}) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(color: colors.inputFill, borderRadius: BorderRadius.circular(14)),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, size: 18, color: colors.textMuted),
          const SizedBox(height: 8),
          Text(label, style: TextStyle(fontSize: 10, color: colors.textMuted, fontWeight: FontWeight.w600)),
          const SizedBox(height: 4),
          Text(value, style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: valueColor ?? colors.textDark)),
        ],
      ),
    );
  }
}
