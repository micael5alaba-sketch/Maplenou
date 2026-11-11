import 'package:geolocator/geolocator.dart';

import '../models/delivery_model.dart';
import 'api_client.dart';
import 'location_service.dart';

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

  /// Refuse une course qui vient d'être affectée (avant de la démarrer) : elle est aussitôt
  /// proposée au livreur suivant et ne lui est plus reproposée.
  Future<void> declineDelivery(String deliveryId) {
    return _client.patch('/api/deliveries/$deliveryId/decline');
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
  /// Distance à vol d'oiseau entre la position actuelle du livreur et la boutique (premier
  /// tronçon : livreur -> boutique, avant le tronçon boutique -> client). `null` si la position
  /// du livreur ou celle de la boutique est indisponible (permission refusée, GPS coupé...) —
  /// jamais bloquant pour l'affichage de la course.
  Future<double?> courierToShopKm(DeliveryNavigationModel navigation) async {
    final pickup = navigation.pickup;
    if (!pickup.hasLocation) return null;
    try {
      final position = await LocationService().getCurrentPosition().timeout(const Duration(seconds: 6));
      return Geolocator.distanceBetween(
            position.latitude,
            position.longitude,
            pickup.latitude!,
            pickup.longitude!,
          ) /
          1000;
    } catch (_) {
      return null;
    }
  }

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
