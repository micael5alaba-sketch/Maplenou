/// Miroir de `ShopResponse` (`catalog/dto/ShopResponse.java`).
class ShopModel {
  final String id;
  final String name;
  final String slug;
  final String? description;
  final String? logoUrl;
  final String? coverUrl;
  final String status;
  final String city;
  final String? district;
  final String? taxId;
  final DateTime createdAt;

  const ShopModel({
    required this.id,
    required this.name,
    required this.slug,
    this.description,
    this.logoUrl,
    this.coverUrl,
    required this.status,
    required this.city,
    this.district,
    this.taxId,
    required this.createdAt,
  });

  bool get isApproved => status == 'APPROVED';

  String get location {
    final parts = [city, if (district != null && district!.isNotEmpty) district!];
    return parts.join(', ');
  }

  factory ShopModel.fromJson(Map<String, dynamic> json) {
    return ShopModel(
      id: json['id'] as String,
      name: json['name'] as String,
      slug: json['slug'] as String,
      description: json['description'] as String?,
      logoUrl: json['logoUrl'] as String?,
      coverUrl: json['coverUrl'] as String?,
      status: json['status'] as String,
      city: json['city'] as String,
      district: json['district'] as String?,
      taxId: json['taxId'] as String?,
      createdAt: DateTime.parse(json['createdAt'] as String),
    );
  }
}
