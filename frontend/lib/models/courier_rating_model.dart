/// Note moyenne + nombre d'avis d'un livreur. Miroir de `RatingSummaryResponse`
/// (`review/dto/RatingSummaryResponse.java`).
class CourierRatingModel {
  final double averageRating;
  final int reviewCount;

  const CourierRatingModel({required this.averageRating, required this.reviewCount});

  factory CourierRatingModel.fromJson(Map<String, dynamic> json) {
    return CourierRatingModel(
      averageRating: (json['averageRating'] as num).toDouble(),
      reviewCount: (json['reviewCount'] as num).toInt(),
    );
  }
}
