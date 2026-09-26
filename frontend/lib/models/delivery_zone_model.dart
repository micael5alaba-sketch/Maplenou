/// Miroir de `DeliveryZoneResponse` (`delivery/dto/DeliveryZoneResponse.java`).
class DeliveryZoneModel {
  final String id;
  final String name;
  final num deliveryFee;
  final int estimatedTimeMinutes;
  final bool active;

  const DeliveryZoneModel({
    required this.id,
    required this.name,
    required this.deliveryFee,
    required this.estimatedTimeMinutes,
    required this.active,
  });

  factory DeliveryZoneModel.fromJson(Map<String, dynamic> json) {
    return DeliveryZoneModel(
      id: json['id'] as String,
      name: json['name'] as String,
      deliveryFee: json['deliveryFee'] as num,
      estimatedTimeMinutes: json['estimatedTimeMinutes'] as int,
      active: json['active'] as bool? ?? true,
    );
  }
}
