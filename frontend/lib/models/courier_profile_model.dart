class CourierProfileModel {
  final String fullName;
  final String levelLabel;
  final bool isVerified;
  final double rating;
  final int deliveriesCount;
  final int yearsExperience;
  final num availableBalance;

  const CourierProfileModel({
    required this.fullName,
    required this.levelLabel,
    required this.isVerified,
    required this.rating,
    required this.deliveriesCount,
    required this.yearsExperience,
    required this.availableBalance,
  });
}
