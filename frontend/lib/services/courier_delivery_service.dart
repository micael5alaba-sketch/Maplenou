import 'package:geolocator/geolocator.dart';

import '../models/delivery_model.dart';
import 'api_client.dart';

/// `delivery/DeliveryController.java` — livraisons du livreur connecté
/// (`GET/PATCH /api/deliveries/...`).
class CourierDeliveryService {
  final _client = ApiClient();

  Future<List<DeliveryModel>> getMyDeliveries({int size = 50}) async {
    final data = await _client.get('/api/deliveries/my', query: {'size': '$size'}) as Map<String, dynamic>;
    final content = data['content'] as List<dynamic>? ?? [];
    return content.map((json) => DeliveryModel.fromJson(json as Map<String, dynamic>)).toList();
  }

  Future<DeliveryNavigationModel> getNavigation(String deliveryId) async {
    final data = await _client.get('/api/deliveries/$deliveryId/navigation') as Map<String, dynamic>;
    return DeliveryNavigationModel.fromJson(data);
  }

  Future<void> startDelivery(String deliveryId) {
    return _client.patch('/api/deliveries/$deliveryId/status', body: {'status': 'IN_TRANSIT'});
  }

  Future<void> confirmDelivery(String deliveryId, {required String proofUrl}) {
    return _client.patch(
      '/api/deliveries/$deliveryId/status',
      body: {'status': 'DELIVERED', 'proofType': 'PHOTO', 'proofUrl': proofUrl},
    );
  }

  Future<void> reportFailure(String deliveryId, {required String reason}) {
    return _client.patch(
      '/api/deliveries/$deliveryId/status',
      body: {'status': 'FAILED', 'notes': reason},
    );
  }

  /// Distance à vol d'oiseau entre les deux points de [navigation], en km —
  /// pas un itinéraire routier réel (pas d'intégration Google Directions),
  /// mais une vraie mesure géographique à partir de coordonnées réelles.
  double? straightLineDistanceKm(DeliveryNavigationModel navigation) {
    final pickup = navigation.pickup;
    final dropoff = navigation.dropoff;
    if (!pickup.hasLocation || !dropoff.hasLocation) return null;
    final meters = Geolocator.distanceBetween(
      pickup.latitude!,
      pickup.longitude!,
      dropoff.latitude!,
      dropoff.longitude!,
    );
    return meters / 1000;
  }
}
