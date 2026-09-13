import '../models/my_review_model.dart';

/// Provides the buyer's own reviews shown on [MyReviewsScreen].
///
/// Mocked for now — no network call. Maps directly onto the real, already
/// working `GET /api/users/me/reviews` once auth is wired up. Writing a
/// new review isn't built here: it needs a delivered order/product to
/// attach to, which this mocked layer doesn't model — out of scope for
/// this pass (see `MyOrdersScreen` for the equivalent read-only choice).
class MyReviewsService {
  List<MyReviewModel> getMyReviews() {
    return [
      MyReviewModel(
        targetName: 'Sac Cabas Tressé',
        type: ReviewTargetType.product,
        rating: 5,
        comment: 'Très bonne qualité, exactement comme sur les photos.',
        createdAt: DateTime.now().subtract(const Duration(days: 6)),
      ),
      MyReviewModel(
        targetName: 'Atelier Terre & Racine',
        type: ReviewTargetType.shop,
        rating: 4,
        comment: 'Vendeur réactif, livraison rapide.',
        createdAt: DateTime.now().subtract(const Duration(days: 20)),
      ),
    ];
  }
}
