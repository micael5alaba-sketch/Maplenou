import 'api_client.dart';

/// `POST /api/sellers/apply` (`seller/SellerProfileController.java`) — un
/// compte déjà connecté active son profil vendeur, sur ce même compte (voir
/// `CLAUDE.md` §3 : pas de nouvelle inscription séparée).
class SellerApplicationService {
  final _client = ApiClient();

  Future<void> apply({required String shopName}) {
    return _client.post('/api/sellers/apply', body: {'shopName': shopName});
  }
}
