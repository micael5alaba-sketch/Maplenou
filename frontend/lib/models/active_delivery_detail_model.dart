/// Full detail of one delivery, shown on [CourierDeliveryDetailScreen] and
/// carried into [CourierDeliveryConfirmationScreen]. Assemblé depuis
/// `DeliveryModel` + `DeliveryNavigationModel` (voir `CourierDeliveryService`).
class ActiveDeliveryDetailModel {
  final String id;
  final String orderNumber;
  final String status;
  final String shopName;
  final String pickupAddress;
  final String clientName;
  final String deliveryAddress;
  final double? distanceKm;

  const ActiveDeliveryDetailModel({
    required this.id,
    required this.orderNumber,
    required this.status,
    required this.shopName,
    required this.pickupAddress,
    required this.clientName,
    required this.deliveryAddress,
    this.distanceKm,
  });

  bool get canStart => status == 'ASSIGNED';
  bool get canConfirm => status == 'IN_TRANSIT';
}
