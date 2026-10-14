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

  /// Anonymisation RGPD du compte (`GdprService.selfDelete`) — pas une
  /// suppression physique (obligation comptable sur les commandes déjà
  /// facturées, voir `CLAUDE.md` §10), mais le compte devient inutilisable :
  /// à appeler uniquement juste avant de déconnecter l'utilisateur.
  Future<void> deleteAccount() => _client.delete('/api/users/me');
}
