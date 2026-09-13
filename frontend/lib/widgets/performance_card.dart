import 'package:flutter/material.dart';

import '../theme/app_color_scheme.dart';

/// "Performance" card: overall rating and validated-deliveries count.
class PerformanceCard extends StatelessWidget {
  final double rating;
  final int validatedDeliveries;
  final int totalDeliveries;

  const PerformanceCard({
    super.key,
    required this.rating,
    required this.validatedDeliveries,
    required this.totalDeliveries,
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
            children: [
              Icon(Icons.speed_rounded, size: 16, color: colors.accentOrange),
              const SizedBox(width: 8),
              Text('PERFORMANCE', style: TextStyle(fontSize: 12, fontWeight: FontWeight.w700, color: colors.textMuted)),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(
                children: [
                  const Icon(Icons.star_rounded, color: Colors.amber, size: 22),
                  const SizedBox(width: 6),
                  Text('$rating', style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: colors.textDark)),
                ],
              ),
              Text(
                '$validatedDeliveries/$totalDeliveries',
                style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: colors.textDark),
              ),
            ],
          ),
          const SizedBox(height: 4),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text('Note globale', style: TextStyle(fontSize: 12, color: colors.textMuted)),
              Text('Courses validées', style: TextStyle(fontSize: 12, color: colors.textMuted)),
            ],
          ),
        ],
      ),
    );
  }
}
