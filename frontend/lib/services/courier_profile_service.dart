import '../models/courier_profile_model.dart';
import 'courier_delivery_service.dart';
import 'courier_earnings_service.dart';
import 'courier_review_service.dart';
import 'user_service.dart';

/// `GET /api/users/me` + `CourierDeliveryService` + `CourierEarningsService`
/// + `CourierReviewService` pour l'identité/solde/nombre de livraisons/note
/// réels. `levelLabel` reste un libellé générique et `yearsExperience` reste
/// `null` : aucun champ backend équivalent n'existe pour l'ancienneté d'un
/// compte `DELIVERY_AGENT`.
class CourierProfileService {
  final _userService = UserService();
  final _deliveryService = CourierDeliveryService();
  final _earningsService = CourierEarningsService();
  final _reviewService = CourierReviewService();

  Future<CourierProfileModel> getProfile() async {
    final user = await _userService.getCurrentUser();
    final deliveries = await _deliveryService.getMyDeliveries();
    final availableBalance = await _earningsService.getAvailableBalance();

    double? rating;
    try {
      final summary = await _reviewService.getRatingSummary(user.id);
      rating = summary.reviewCount > 0 ? summary.averageRating : null;
    } catch (_) {
      rating = null;
    }

    return CourierProfileModel(
      fullName: user.fullName,
      levelLabel: 'Livreur',
      isVerified: user.phoneVerified,
      rating: rating,
      deliveriesCount: deliveries.where((d) => d.status == 'DELIVERED').length,
      availableBalance: availableBalance,
    );
  }
}
