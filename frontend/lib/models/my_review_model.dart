enum ReviewTargetType { product, shop }

/// One review written by the buyer, shown on [MyReviewsScreen]. Miroir de
/// `MyReviewResponse` (`review/dto/MyReviewResponse.java`).
class MyReviewModel {
  final String targetName;
  final ReviewTargetType type;
  final int rating;
  final String comment;
  final DateTime createdAt;

  const MyReviewModel({
    required this.targetName,
    required this.type,
    required this.rating,
    required this.comment,
    required this.createdAt,
  });

  factory MyReviewModel.fromJson(Map<String, dynamic> json) {
    return MyReviewModel(
      targetName: json['targetName'] as String,
      type: (json['type'] as String) == 'SHOP' ? ReviewTargetType.shop : ReviewTargetType.product,
      rating: json['rating'] as int,
      comment: json['comment'] as String? ?? '',
      createdAt: DateTime.parse(json['createdAt'] as String),
    );
  }
}
