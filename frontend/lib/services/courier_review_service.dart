import '../models/courier_rating_model.dart';
import 'api_client.dart';

/// `review/ReviewController.java` — notation du livreur par l'acheteur
/// (`POST /api/reviews/couriers`) et note moyenne publique
/// (`GET /api/delivery-agents/{agentId}/reviews/summary`).
class CourierReviewService {
  final _client = ApiClient();

  Future<void> submitReview({
    required String deliveryId,
    required int rating,
    String? comment,
  }) {
    return _client.post('/api/reviews/couriers', body: {
      'deliveryId': deliveryId,
      'rating': rating,
      if (comment != null && comment.trim().isNotEmpty) 'comment': comment.trim(),
    });
  }

  Future<CourierRatingModel> getRatingSummary(String agentId) async {
    final data = await _client.get('/api/delivery-agents/$agentId/reviews/summary') as Map<String, dynamic>;
    return CourierRatingModel.fromJson(data);
  }
}
