/// One delivery request the courier can accept, shown on
/// [CourierDeliveriesScreen].
class AvailableDeliveryModel {
  final String id;
  final num amount;
  final DateTime postedAt;
  final String pickupLabel;
  final double pickupDistanceKm;
  final String pickupAddress;
  final String dropoffLabel;
  final double totalDistanceKm;
  final String dropoffAddress;

  /// e.g. "Restauration chaude", "Fast Food", "Pharmacie", "Colis volumineux".
  final String category;
  final String itemsSummary;

  const AvailableDeliveryModel({
    required this.id,
    required this.amount,
    required this.postedAt,
    required this.pickupLabel,
    required this.pickupDistanceKm,
    required this.pickupAddress,
    required this.dropoffLabel,
    required this.totalDistanceKm,
    required this.dropoffAddress,
    required this.category,
    required this.itemsSummary,
  });
}
