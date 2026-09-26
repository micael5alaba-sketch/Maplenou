/// One customer review on a product's detail page. Miroir de
/// `ReviewResponse` (`review/dto/ReviewResponse.java`).
class ReviewModel {
  final String customerName;
  final String? avatarUrl;
  final double rating;
  final String comment;
  final DateTime date;

  const ReviewModel({
    required this.customerName,
    this.avatarUrl,
    required this.rating,
    required this.comment,
    required this.date,
  });

  factory ReviewModel.fromJson(Map<String, dynamic> json) {
    final author = json['author'] as Map<String, dynamic>?;
    return ReviewModel(
      customerName: author?['fullName'] as String? ?? 'Client Maplenou',
      rating: (json['rating'] as num).toDouble(),
      comment: json['comment'] as String? ?? '',
      date: DateTime.parse(json['createdAt'] as String),
    );
  }
}
