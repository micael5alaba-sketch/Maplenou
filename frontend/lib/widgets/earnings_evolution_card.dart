import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';

import '../models/seller_dashboard_model.dart' show SalesPoint;
import '../theme/app_color_scheme.dart';
import '../utils/formatters.dart';

/// "Évolution des gains" card on [CourierDashboardScreen]: a single-week
/// area chart plus a weekly total and change indicator. Unlike the
/// seller's [SalesChartWidget], the maquette shows one fixed "Cette
/// semaine" range with no switcher.
class EarningsEvolutionCard extends StatelessWidget {
  final List<SalesPoint> points;
  final num weeklyTotal;
  final double changePercent;

  const EarningsEvolutionCard({
    super.key,
    required this.points,
    required this.weeklyTotal,
    required this.changePercent,
  });

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    final isPositive = changePercent >= 0;

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
              Text('Évolution des gains', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: colors.textDark)),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                decoration: BoxDecoration(color: colors.inputFill, borderRadius: BorderRadius.circular(20)),
                child: Text('Cette semaine', style: TextStyle(fontSize: 12, color: colors.textMuted, fontWeight: FontWeight.w600)),
              ),
            ],
          ),
          const SizedBox(height: 16),
          SizedBox(height: 140, child: _buildChart(colors)),
          const SizedBox(height: 16),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Total semaine', style: TextStyle(fontSize: 12, color: colors.textMuted)),
                  Text(formatFcfa(weeklyTotal), style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
                ],
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                decoration: BoxDecoration(color: colors.primary.withValues(alpha: 0.1), borderRadius: BorderRadius.circular(20)),
                child: Text(
                  '${isPositive ? '+' : ''}${changePercent.toStringAsFixed(0)}% vs sem. dernière',
                  style: TextStyle(fontSize: 11, fontWeight: FontWeight.w600, color: colors.primary),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildChart(AppColorScheme colors) {
    if (points.isEmpty) {
      return Center(child: Text('Aucune donnée.', style: TextStyle(color: colors.textMuted)));
    }
    final maxY = points.map((p) => p.value).reduce((a, b) => a > b ? a : b);

    return LineChart(
      LineChartData(
        minY: 0,
        maxY: maxY * 1.2,
        gridData: const FlGridData(show: false),
        borderData: FlBorderData(show: false),
        titlesData: FlTitlesData(
          leftTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
          topTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
          rightTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
          bottomTitles: AxisTitles(
            sideTitles: SideTitles(
              showTitles: true,
              reservedSize: 22,
              getTitlesWidget: (value, meta) {
                final index = value.toInt();
                if (index < 0 || index >= points.length) return const SizedBox.shrink();
                return Padding(
                  padding: const EdgeInsets.only(top: 6),
                  child: Text(points[index].label, style: TextStyle(fontSize: 10, color: colors.textMuted)),
                );
              },
            ),
          ),
        ),
        lineTouchData: LineTouchData(
          touchTooltipData: LineTouchTooltipData(
            getTooltipColor: (touchedSpot) => colors.primary,
            getTooltipItems: (touchedSpots) => touchedSpots.map((spot) {
              final index = spot.x.toInt();
              final label = index >= 0 && index < points.length ? points[index].label : '';
              return LineTooltipItem(
                '$label\n${spot.y.round()} FCFA',
                const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 11),
              );
            }).toList(),
          ),
        ),
        lineBarsData: [
          LineChartBarData(
            spots: [for (var i = 0; i < points.length; i++) FlSpot(i.toDouble(), points[i].value)],
            isCurved: true,
            color: colors.primary,
            barWidth: 3,
            dotData: const FlDotData(show: false),
            belowBarData: BarAreaData(
              show: true,
              gradient: LinearGradient(
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
                colors: [colors.primary.withValues(alpha: 0.22), colors.primary.withValues(alpha: 0.0)],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
