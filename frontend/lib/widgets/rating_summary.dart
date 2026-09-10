import 'package:flutter/material.dart';

import '../theme/app_color_scheme.dart';

/// Stars + average rating + review count, e.g. "★★★★★ 4.8 (128 avis)".
class RatingSummary extends StatelessWidget {
  final double rating;
  final int reviewCount;
  final double starSize;

  const RatingSummary({
    super.key,
    required this.rating,
    required this.reviewCount,
    this.starSize = 18,
  });

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        ...List.generate(5, (index) {
          final filled = index < rating.round();
          return Icon(
            filled ? Icons.star_rounded : Icons.star_border_rounded,
            size: starSize,
            color: colors.accentOrange,
          );
        }),
        const SizedBox(width: 6),
        Text(
          rating.toStringAsFixed(1),
          style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark),
        ),
        const SizedBox(width: 4),
        Text('($reviewCount avis)', style: TextStyle(color: colors.textMuted)),
      ],
    );
  }
}
