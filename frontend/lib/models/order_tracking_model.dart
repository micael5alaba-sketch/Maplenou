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

class OrderTrackingModel {
  final String orderNumber;
  final String etaLabel;
  final String courierName;
  final double courierRating;
  final int courierDeliveryCount;
  final String deliveryAddress;
  final String? addressNote;
  final List<OrderTrackingStep> steps;

  const OrderTrackingModel({
    required this.orderNumber,
    required this.etaLabel,
    required this.courierName,
    required this.courierRating,
    required this.courierDeliveryCount,
    required this.deliveryAddress,
    this.addressNote,
    required this.steps,
  });
}
