import 'package:flutter/material.dart';

import '../models/my_review_model.dart';
import '../services/my_reviews_service.dart';
import '../theme/app_color_scheme.dart';
import '../utils/formatters.dart';

/// "Mes Avis" — reviews the buyer has written, on products or shops.
///
/// Backed by [MyReviewsService] mocked data for now — maps directly onto
/// the real, already working `GET /api/users/me/reviews` once auth is
/// wired up.
class MyReviewsScreen extends StatelessWidget {
  const MyReviewsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    final reviews = MyReviewsService().getMyReviews();

    return Scaffold(
      backgroundColor: colors.homeBackground,
      body: SafeArea(
        child: Column(
          children: [
            _buildHeader(context, colors),
            Expanded(
              child: reviews.isEmpty
                  ? _buildEmptyState(colors)
                  : ListView.separated(
                      padding: const EdgeInsets.all(16),
                      itemCount: reviews.length,
                      separatorBuilder: (_, _) => const SizedBox(height: 12),
                      itemBuilder: (context, index) => _buildReviewCard(colors, reviews[index]),
                    ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader(BuildContext context, AppColorScheme colors) {
    return Container(
      color: colors.surface,
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
      child: Row(
        children: [
          IconButton(icon: Icon(Icons.arrow_back_rounded, color: colors.textDark), onPressed: () => Navigator.of(context).pop()),
          Expanded(child: Text('Mes Avis', textAlign: TextAlign.center, style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark))),
          const SizedBox(width: 48),
        ],
      ),
    );
  }

  Widget _buildReviewCard(AppColorScheme colors, MyReviewModel review) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 10, offset: const Offset(0, 3))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(review.type == ReviewTargetType.product ? Icons.inventory_2_outlined : Icons.storefront_outlined, size: 16, color: colors.textMuted),
              const SizedBox(width: 6),
              Expanded(child: Text(review.targetName, style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark))),
              Text(formatRelativeDate(review.createdAt), style: TextStyle(fontSize: 11, color: colors.textMuted)),
            ],
          ),
          const SizedBox(height: 6),
          Row(children: List.generate(5, (i) => Icon(i < review.rating ? Icons.star_rounded : Icons.star_border_rounded, size: 16, color: Colors.amber))),
          const SizedBox(height: 6),
          Text(review.comment, style: TextStyle(color: colors.textDark, height: 1.3)),
        ],
      ),
    );
  }

  Widget _buildEmptyState(AppColorScheme colors) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.rate_review_outlined, size: 48, color: colors.textMuted),
            const SizedBox(height: 12),
            Text("Vous n'avez pas encore laissé d'avis.", style: TextStyle(color: colors.textMuted, fontWeight: FontWeight.w500)),
          ],
        ),
      ),
    );
  }
}
