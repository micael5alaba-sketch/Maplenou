import '../models/available_delivery_model.dart';
import 'courier_delivery_service.dart';

/// Livraisons assignées au livreur, pas encore commencées (statut
/// `ASSIGNED`) — via [CourierDeliveryService] (`GET /api/deliveries/my`
/// + `GET /api/deliveries/{id}/navigation` pour les adresses).
class AvailableDeliveriesService {
  final _deliveryService = CourierDeliveryService();

  Future<List<AvailableDeliveryModel>> getAvailableDeliveries() async {
    final deliveries = await _deliveryService.getMyDeliveries();
    final assigned = deliveries.where((d) => d.status == 'ASSIGNED').toList();

    final results = <AvailableDeliveryModel>[];
    for (final delivery in assigned) {
      final navigation = await _deliveryService.getNavigation(delivery.id);
      results.add(AvailableDeliveryModel(
        id: delivery.id,
        assignedAt: delivery.assignedAt ?? delivery.createdAt,
        pickupLabel: navigation.pickup.label,
        pickupAddress: navigation.pickup.address,
        dropoffLabel: navigation.dropoff.label,
        dropoffAddress: navigation.dropoff.address,
        distanceKm: _deliveryService.straightLineDistanceKm(navigation),
      ));
    }
    return results;
  }
}
