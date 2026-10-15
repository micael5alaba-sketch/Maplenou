/// Miroir de `DeliveryZoneResponse` (`delivery/dto/DeliveryZoneResponse.java`).
class DeliveryZoneModel {
  final String id;
  final String name;
  final num deliveryFee;
  final num pricePerKm;
  final int estimatedTimeMinutes;
  final bool active;

  const DeliveryZoneModel({
    required this.id,
    required this.name,
    required this.deliveryFee,
    required this.pricePerKm,
    required this.estimatedTimeMinutes,
    required this.active,
  });

  factory DeliveryZoneModel.fromJson(Map<String, dynamic> json) {
    return DeliveryZoneModel(
      id: json['id'] as String,
      name: json['name'] as String,
      deliveryFee: json['deliveryFee'] as num,
      pricePerKm: json['pricePerKm'] as num? ?? 0,
      estimatedTimeMinutes: json['estimatedTimeMinutes'] as int,
      active: json['active'] as bool? ?? true,
    );
  }
}

/// Miroir de `DeliveryFeeQuote` (`delivery/dto/DeliveryFeeQuote.java`) — tarif
/// de base + distance réelle, mêmes chiffres que ceux figés côté backend au
/// moment de la commande (`OrderService.placeOrder`).
class DeliveryFeeQuoteModel {
  final num fee;
  final num? distanceKm;
  final String? zoneName;

  const DeliveryFeeQuoteModel({required this.fee, this.distanceKm, this.zoneName});

  factory DeliveryFeeQuoteModel.fromJson(Map<String, dynamic> json) {
    return DeliveryFeeQuoteModel(
      fee: json['fee'] as num,
      distanceKm: json['distanceKm'] as num?,
      zoneName: json['zoneName'] as String?,
    );
  }
}
