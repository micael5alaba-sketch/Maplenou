/// [rating] est `null` tant qu'aucun avis n'a été laissé, sinon la vraie
/// moyenne (`CourierReviewService`). [yearsExperience] reste `null` : aucune
/// notion d'ancienneté n'existe pour un compte `DELIVERY_AGENT` côté backend.
class CourierProfileModel {
  final String fullName;
  final String levelLabel;
  final bool isVerified;
  final double? rating;
  final int deliveriesCount;
  final int? yearsExperience;
  final num availableBalance;

  const CourierProfileModel({
    required this.fullName,
    required this.levelLabel,
    required this.isVerified,
    this.rating,
    required this.deliveriesCount,
    this.yearsExperience,
    required this.availableBalance,
  });
}
