/// One saved delivery address. Maps onto the backend's `Address` entity
/// (see `MODELE_DONNEES.md`).
class AddressModel {
  final String id;
  final String label;
  final String city;
  final String? district;
  final String? details;
  final bool isDefault;

  const AddressModel({
    required this.id,
    required this.label,
    required this.city,
    this.district,
    this.details,
    this.isDefault = false,
  });

  String get summary {
    final parts = [city, if (district != null && district!.isNotEmpty) district!];
    return parts.join(', ');
  }

  AddressModel copyWith({bool? isDefault}) {
    return AddressModel(
      id: id,
      label: label,
      city: city,
      district: district,
      details: details,
      isDefault: isDefault ?? this.isDefault,
    );
  }
}
