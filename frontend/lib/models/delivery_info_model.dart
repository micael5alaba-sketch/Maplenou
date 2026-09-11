/// Delivery and returns info shown on a product's detail page.
class DeliveryInfoModel {
  final String standardDelay;
  final String expressDelay;
  final num deliveryFee;
  final String returnPolicy;

  const DeliveryInfoModel({
    required this.standardDelay,
    required this.expressDelay,
    required this.deliveryFee,
    required this.returnPolicy,
  });
}
