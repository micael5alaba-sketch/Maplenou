/// One delivery assigned to the courier, not yet started, shown on
/// [CourierDeliveriesScreen]. Assemblé depuis `DeliveryResponse`
/// (liste, `GET /api/deliveries/my`) et `NavigationResponse` (adresses +
/// gain estimé, `GET /api/deliveries/{id}/navigation`) — l'affectation est
/// faite par un admin (`CLAUDE.md` §3.bis), pas une file de courses à
/// accepter soi-même, mais le livreur voit tout de même le gain estimé dès
/// l'affectation (75% du tarif de zone, voir `CourierEarningService`), pas
/// seulement une fois la livraison terminée.
class AvailableDeliveryModel {
  final String id;
  final DateTime assignedAt;
  final String pickupLabel;
  final String pickupAddress;
  final String dropoffLabel;
  final String dropoffAddress;
  final double? distanceKm;
  final double? estimatedEarning;

  const AvailableDeliveryModel({
    required this.id,
    required this.assignedAt,
    required this.pickupLabel,
    required this.pickupAddress,
    required this.dropoffLabel,
    required this.dropoffAddress,
    this.distanceKm,
    this.estimatedEarning,
  });
}
