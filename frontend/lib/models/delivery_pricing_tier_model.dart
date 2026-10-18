/// Miroir de `DeliveryPricingTierResponse` (`delivery/dto/DeliveryPricingTierResponse.java`)
/// — un palier du barème de livraison par distance : "jusqu'à [maxDistanceKm] km, [price]
/// FCFA". `maxDistanceKm` nul = dernier palier, "au-delà de X km" (illimité).
class DeliveryPricingTierModel {
  final String id;
  final num? maxDistanceKm;
  final num price;

  const DeliveryPricingTierModel({required this.id, this.maxDistanceKm, required this.price});

  bool get isUnlimited => maxDistanceKm == null;

  factory DeliveryPricingTierModel.fromJson(Map<String, dynamic> json) {
    return DeliveryPricingTierModel(
      id: json['id'] as String,
      maxDistanceKm: json['maxDistanceKm'] as num?,
      price: json['price'] as num,
    );
  }
}

/// Miroir de `DeliveryFeeQuote` (`delivery/dto/DeliveryFeeQuote.java`) — prix déterminé
/// par le palier correspondant à la distance réelle, mêmes chiffres que ceux figés côté
/// backend au moment de la commande (`OrderService.placeOrder`).
class DeliveryFeeQuoteModel {
  final num fee;
  final num? distanceKm;

  const DeliveryFeeQuoteModel({required this.fee, this.distanceKm});

  factory DeliveryFeeQuoteModel.fromJson(Map<String, dynamic> json) {
    return DeliveryFeeQuoteModel(
      fee: json['fee'] as num,
      distanceKm: json['distanceKm'] as num?,
    );
  }
}
