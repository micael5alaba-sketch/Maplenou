/// One saved delivery address. Miroir de `AddressResponse`
/// (`user/dto/AddressResponse.java`) côté backend.
class AddressModel {
  final String id;
  final String label;
  final String city;
  final String? district;
  final String? details;
  final double? latitude;
  final double? longitude;
  final bool isDefault;

  const AddressModel({
    required this.id,
    required this.label,
    required this.city,
    this.district,
    this.details,
    this.latitude,
    this.longitude,
    this.isDefault = false,
  });

  bool get hasLocation => latitude != null && longitude != null;

  String get summary {
    final parts = [city, if (district != null && district!.isNotEmpty) district!];
    return parts.join(', ');
  }

  factory AddressModel.fromJson(Map<String, dynamic> json) {
    return AddressModel(
      id: json['id'] as String,
      label: (json['label'] as String?) ?? '',
      city: json['city'] as String,
      district: json['district'] as String?,
      details: json['details'] as String?,
      latitude: (json['latitude'] as num?)?.toDouble(),
      longitude: (json['longitude'] as num?)?.toDouble(),
      isDefault: json['isDefault'] as bool? ?? false,
    );
  }
}
