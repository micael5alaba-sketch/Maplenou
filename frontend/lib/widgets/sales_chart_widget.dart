import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';

import '../models/seller_dashboard_model.dart';
import '../theme/app_color_scheme.dart';

/// "Évolution des ventes" card: a range filter (1/2/4 semaines) over a
/// smooth line chart. Manages its own selected range — the caller just
/// hands it the data for every range up front.
class SalesChartWidget extends StatefulWidget {
  final Map<SalesChartRange, List<SalesPoint>> salesByRange;

  const SalesChartWidget({super.key, required this.salesByRange});

  @override
  State<SalesChartWidget> createState() => _SalesChartWidgetState();
}

class _SalesChartWidgetState extends State<SalesChartWidget> {
  SalesChartRange _selectedRange = SalesChartRange.oneWeek;

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    final points = widget.salesByRange[_selectedRange] ?? const [];

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
          Text(
            'Évolution des ventes',
            style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: colors.textDark),
          ),
          const SizedBox(height: 14),
          _buildRangeFilters(colors),
          const SizedBox(height: 20),
          SizedBox(
            height: 200,
            child: points.isEmpty
                ? Center(child: Text('Aucune donnée.', style: TextStyle(color: colors.textMuted)))
                : _buildChart(colors, points),
          ),
        ],
      ),
    );
  }

  Widget _buildRangeFilters(AppColorScheme colors) {
    return Row(
      children: SalesChartRange.values.map((range) {
        final isActive = range == _selectedRange;
        return Padding(
          padding: const EdgeInsets.only(right: 8),
          child: GestureDetector(
            onTap: () => setState(() => _selectedRange = range),
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
              decoration: BoxDecoration(
                color: isActive ? colors.primary : colors.inputFill,
                borderRadius: BorderRadius.circular(20),
              ),
              child: Text(
                range.label,
                style: TextStyle(
                  fontSize: 12,
                  fontWeight: isActive ? FontWeight.w700 : FontWeight.w500,
                  color: isActive ? Colors.white : colors.textMuted,
                ),
              ),
            ),
          ),
        );
      }).toList(),
    );
  }

  Widget _buildChart(AppColorScheme colors, List<SalesPoint> points) {
    final maxY = points.map((p) => p.value).reduce((a, b) => a > b ? a : b);
    // How many labels to skip on the X axis so a dense range (14/28 points)
    // doesn't overlap into unreadable text.
    final labelStep = (points.length / 6).ceil().clamp(1, points.length);

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
              reservedSize: 26,
              getTitlesWidget: (value, meta) {
                final index = value.toInt();
                if (index < 0 || index >= points.length) return const SizedBox.shrink();
                if (index % labelStep != 0) return const SizedBox.shrink();
                return Padding(
                  padding: const EdgeInsets.only(top: 6),
                  child: Text(
                    points[index].label,
                    style: TextStyle(fontSize: 10, color: colors.textMuted),
                  ),
                );
              },
            ),
          ),
        ),
        lineTouchData: LineTouchData(
          touchTooltipData: LineTouchTooltipData(
            getTooltipColor: (touchedSpot) => colors.primary,
            getTooltipItems: (touchedSpots) {
              return touchedSpots.map((spot) {
                final index = spot.x.toInt();
                final label = index >= 0 && index < points.length ? points[index].label : '';
                return LineTooltipItem(
                  '$label\n${spot.y.round()} FCFA',
                  const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 11),
                );
              }).toList();
            },
          ),
        ),
        lineBarsData: [
          LineChartBarData(
            spots: [
              for (var i = 0; i < points.length; i++) FlSpot(i.toDouble(), points[i].value),
            ],
            isCurved: true,
            color: colors.primary,
            barWidth: 3,
            dotData: const FlDotData(show: false),
            belowBarData: BarAreaData(
              show: true,
              gradient: LinearGradient(
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
                colors: [
                  colors.primary.withValues(alpha: 0.22),
                  colors.primary.withValues(alpha: 0.0),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
