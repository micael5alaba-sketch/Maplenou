/// One delivery assigned to the courier, not yet started, shown on
/// [CourierDeliveriesScreen]. Assemblé depuis `DeliveryResponse`
/// (liste, `GET /api/deliveries/my`) et `NavigationResponse` (adresses,
/// `GET /api/deliveries/{id}/navigation`) — l'affectation est faite par un
/// admin (`CLAUDE.md` §3), pas une file de courses à accepter soi-même :
/// pas de montant ni de catégorie à afficher tant que la livraison n'est
/// pas terminée (voir `CourierEarning`, calculé après coup).
class AvailableDeliveryModel {
  final String id;
  final DateTime assignedAt;
  final String pickupLabel;
  final String pickupAddress;
  final String dropoffLabel;
  final String dropoffAddress;
  final double? distanceKm;

  const AvailableDeliveryModel({
    required this.id,
    required this.assignedAt,
    required this.pickupLabel,
    required this.pickupAddress,
    required this.dropoffLabel,
    required this.dropoffAddress,
    this.distanceKm,
  });
}
