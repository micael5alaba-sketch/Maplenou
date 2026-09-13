enum ReviewTargetType { product, shop }

/// One review written by the buyer, shown on [MyReviewsScreen]. Maps
/// directly onto the backend's `MyReviewResponse`
/// (`GET /api/users/me/reviews`, see `MODELE_DONNEES.md`) — already built
/// and working, just needs auth to be wired up.
class MyReviewModel {
  final String targetName;
  final ReviewTargetType type;
  final int rating;
  final String comment;
  final DateTime createdAt;

  const MyReviewModel({
    required this.targetName,
    required this.type,
    required this.rating,
    required this.comment,
    required this.createdAt,
  });
}
