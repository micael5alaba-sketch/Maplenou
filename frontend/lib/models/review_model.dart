/// One customer review on a product's detail page.
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
}
