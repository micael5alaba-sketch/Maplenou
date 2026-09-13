/// Full detail of the delivery the courier is currently handling, shown on
/// [CourierDeliveryDetailScreen] and carried into
/// [CourierDeliveryConfirmationScreen].
class ActiveDeliveryDetailModel {
  final String orderNumber;
  final String clientName;
  final int clientOrdersCount;
  final double clientRating;
  final String deliveryAddress;
  final String? doorCode;
  final int etaMinutes;
  final List<String> items;
  final num amountToCollect;

  const ActiveDeliveryDetailModel({
    required this.orderNumber,
    required this.clientName,
    required this.clientOrdersCount,
    required this.clientRating,
    required this.deliveryAddress,
    this.doorCode,
    required this.etaMinutes,
    required this.items,
    required this.amountToCollect,
  });
}
