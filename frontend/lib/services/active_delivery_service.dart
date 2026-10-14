import '../models/active_delivery_detail_model.dart';
import 'courier_delivery_service.dart';

/// Détail d'une livraison (`CourierDeliveryService.getMyDeliveries` +
/// `getNavigation`) — utilisé pour la livraison en cours (`IN_TRANSIT`)
/// comme pour une livraison encore `ASSIGNED`.
class ActiveDeliveryService {
  final _deliveryService = CourierDeliveryService();

  Future<ActiveDeliveryDetailModel> getActiveDelivery(String deliveryId) async {
    final deliveries = await _deliveryService.getMyDeliveries();
    final delivery = deliveries.firstWhere((d) => d.id == deliveryId);
    final navigation = await _deliveryService.getNavigation(deliveryId);

    return ActiveDeliveryDetailModel(
      id: delivery.id,
      orderNumber: delivery.orderNumber,
      status: delivery.status,
      shopName: navigation.pickup.label,
      pickupAddress: navigation.pickup.address,
      clientName: navigation.dropoff.label,
      deliveryAddress: navigation.dropoff.address,
      distanceKm: _deliveryService.straightLineDistanceKm(navigation),
      pickupLatitude: navigation.pickup.latitude,
      pickupLongitude: navigation.pickup.longitude,
      dropoffLatitude: navigation.dropoff.latitude,
      dropoffLongitude: navigation.dropoff.longitude,
      estimatedEarning: navigation.estimatedEarning,
    );
  }

  /// La livraison actuellement en route (`IN_TRANSIT`), s'il y en a une —
  /// pour la bannière du dashboard. `null` si le livreur n'a rien en cours.
  Future<ActiveDeliveryDetailModel?> getCurrentInTransitDelivery() async {
    final deliveries = await _deliveryService.getMyDeliveries();
    final inTransit = deliveries.where((d) => d.status == 'IN_TRANSIT').toList();
    if (inTransit.isEmpty) return null;
    return getActiveDelivery(inTransit.first.id);
  }
}
