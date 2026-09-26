import '../models/address_model.dart';
import 'api_client.dart';

/// `GET/POST/PUT/PATCH/DELETE /api/users/me/addresses`
/// (`user/AddressController.java`).
class AddressService {
  final _client = ApiClient();

  Future<List<AddressModel>> getAddresses() async {
    final data = await _client.get('/api/users/me/addresses') as List<dynamic>;
    return data.map((json) => AddressModel.fromJson(json as Map<String, dynamic>)).toList();
  }

  Future<AddressModel> add({
    required String label,
    required String city,
    String? district,
    String? details,
    double? latitude,
    double? longitude,
    bool isDefault = false,
  }) async {
    final data = await _client.post(
      '/api/users/me/addresses',
      body: {
        'label': label,
        'city': city,
        'district': district,
        'details': details,
        'latitude': latitude,
        'longitude': longitude,
        'isDefault': isDefault,
      },
    ) as Map<String, dynamic>;
    return AddressModel.fromJson(data);
  }

  Future<void> remove(String id) => _client.delete('/api/users/me/addresses/$id');

  Future<void> setDefault(String id) => _client.patch('/api/users/me/addresses/$id/default');
}
