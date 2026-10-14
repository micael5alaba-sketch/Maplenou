/// One step in [OrderTrackingScreen]'s "État de la commande" timeline.
class OrderTrackingStep {
  final String label;
  final String timeLabel;
  final bool isDone;
  final bool isCurrent;

  const OrderTrackingStep({
    required this.label,
    required this.timeLabel,
    this.isDone = false,
    this.isCurrent = false,
  });
}

/// Construit à partir du vrai statut `SubOrderStatus` (voir
/// `OrderTrackingService`) — pas d'ETA ni de note/nombre de courses livreur
/// affichés : aucun champ backend équivalent n'existe pour l'instant.
class OrderTrackingModel {
  final String orderNumber;
  final String? deliveryId;
  final String? courierName;
  final String? courierPhone;
  final String deliveryAddress;
  final String? addressNote;
  final double? deliveryLatitude;
  final double? deliveryLongitude;
  final List<OrderTrackingStep> steps;
  final bool isDelivered;

  const OrderTrackingModel({
    required this.orderNumber,
    this.deliveryId,
    this.courierName,
    this.courierPhone,
    required this.deliveryAddress,
    this.addressNote,
    this.deliveryLatitude,
    this.deliveryLongitude,
    required this.steps,
    this.isDelivered = false,
  });
}
