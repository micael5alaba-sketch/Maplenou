import 'package:flutter/material.dart';

import '../models/review_model.dart';
import '../theme/app_color_scheme.dart';
import '../utils/formatters.dart';

/// One customer review row: avatar, name, star rating, comment and date.
class ProductReviewCard extends StatelessWidget {
  final ReviewModel review;

  const ProductReviewCard({super.key, required this.review});

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 10),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          CircleAvatar(
            radius: 20,
            backgroundColor: colors.inputFill,
            backgroundImage: review.avatarUrl != null ? NetworkImage(review.avatarUrl!) : null,
            child: review.avatarUrl == null
                ? Text(
                    review.customerName.isNotEmpty ? review.customerName[0].toUpperCase() : '?',
                    style: TextStyle(fontWeight: FontWeight.bold, color: colors.primary),
                  )
                : null,
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Expanded(
                      child: Text(
                        review.customerName,
                        style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark),
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                    Text(
                      formatRelativeDate(review.date),
                      style: TextStyle(fontSize: 12, color: colors.textMuted),
                    ),
                  ],
                ),
                const SizedBox(height: 4),
                _buildStars(context, review.rating),
                const SizedBox(height: 6),
                Text(review.comment, style: TextStyle(color: colors.textDark, height: 1.35)),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildStars(BuildContext context, double rating) {
    final accentOrange = context.colors.accentOrange;

    return Row(
      mainAxisSize: MainAxisSize.min,
      children: List.generate(5, (index) {
        final filled = index < rating.round();
        return Icon(
          filled ? Icons.star_rounded : Icons.star_border_rounded,
          size: 16,
          color: accentOrange,
        );
      }),
    );
  }
}
