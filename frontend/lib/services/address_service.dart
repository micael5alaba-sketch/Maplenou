import '../models/address_model.dart';

/// In-memory saved addresses shared across the app session (same pattern
/// as [CartService]/[FavoritesService]).
///
/// Mocked for now — no network call, no persistence across app restarts.
/// Maps onto the real, already working `AddressController`
/// (`GET/POST/PATCH/DELETE /api/users/me/addresses`) once auth is wired up.
class AddressService {
  AddressService._internal();
  static final AddressService _instance = AddressService._internal();
  factory AddressService() => _instance;

  final List<AddressModel> _addresses = [
    const AddressModel(
      id: 'a1',
      label: 'Maison',
      city: 'Lomé',
      district: 'Adidogomé',
      details: 'Rue des Étoiles, portail vert',
      isDefault: true,
    ),
  ];

  List<AddressModel> get addresses => List.unmodifiable(_addresses);

  void add({required String label, required String city, String? district, String? details}) {
    final isFirst = _addresses.isEmpty;
    _addresses.add(AddressModel(
      id: 'a${_addresses.length + 1}_${DateTime.now().millisecondsSinceEpoch}',
      label: label,
      city: city,
      district: district,
      details: details,
      isDefault: isFirst,
    ));
  }

  void remove(String id) {
    _addresses.removeWhere((a) => a.id == id);
  }

  void setDefault(String id) {
    for (var i = 0; i < _addresses.length; i++) {
      _addresses[i] = _addresses[i].copyWith(isDefault: _addresses[i].id == id);
    }
  }
}
