import '../models/delivery_status_model.dart';
import 'api_client.dart';

/// `GET /api/deliveries/sub-orders/{subOrderId}` (`delivery/DeliveryController.java`)
/// — statut de livraison d'une sous-commande, côté acheteur.
class DeliveryStatusService {
  final _client = ApiClient();

  /// Renvoie `null` tant qu'aucune livraison n'a été créée pour cette
  /// sous-commande (404 — sous-commande encore PENDING/PREPARING/
  /// READY_FOR_PICKUP), ce qui n'est pas une erreur à afficher.
  Future<DeliveryStatusModel?> getForSubOrder(String subOrderId) async {
    try {
      final data = await _client.get('/api/deliveries/sub-orders/$subOrderId') as Map<String, dynamic>;
      return DeliveryStatusModel.fromJson(data);
    } on ApiException catch (e) {
      if (e.statusCode == 404) return null;
      rethrow;
    }
  }
}
