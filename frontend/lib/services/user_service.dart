import '../models/user_model.dart';
import 'api_client.dart';

/// `GET/PUT /api/users/me` (`user/UserController.java`) — profil du compte
/// connecté, source de vérité pour déterminer quelle interface afficher
/// (Acheteur/Vendeur/Livreur, voir `UserModel.isDeliveryAgent`/`isApprovedSeller`).
class UserService {
  final _client = ApiClient();

  Future<UserModel> getCurrentUser() async {
    final data = await _client.get('/api/users/me') as Map<String, dynamic>;
    return UserModel.fromJson(data);
  }

  Future<UserModel> updateProfile({String? fullName, String? email, String? avatarUrl}) async {
    final data = await _client.put(
      '/api/users/me',
      body: {
        if (fullName != null) 'fullName': fullName,
        if (email != null) 'email': email,
        if (avatarUrl != null) 'avatarUrl': avatarUrl,
      },
    ) as Map<String, dynamic>;
    return UserModel.fromJson(data);
  }
}
