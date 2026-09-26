import '../models/my_review_model.dart';
import 'review_service.dart';

/// `GET /api/users/me/reviews`, via [ReviewService].
class MyReviewsService {
  final _reviewService = ReviewService();

  Future<List<MyReviewModel>> getMyReviews() => _reviewService.getMyReviews();
}
