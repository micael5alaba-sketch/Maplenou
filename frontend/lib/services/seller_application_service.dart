import 'api_client.dart';

/// `POST /api/sellers/apply` (`seller/SellerProfileController.java`) — un
/// compte déjà connecté active son profil vendeur, sur ce même compte (voir
/// `CLAUDE.md` §3 : pas de nouvelle inscription séparée).
///
/// [feeCategory] : 'STANDARD' (500-10 000 FCFA, 500 FCFA/mois) ou
/// 'HIGH_PRICE' (produit > 10 000 FCFA, 2000 FCFA/mois) — voir
/// `com.maplenou.backend.seller.SellerFeeCategory`. Frais d'activité
/// appliqué seulement à partir de la 4ᵉ vente éligible (3 gratuites).
class SellerApplicationService {
  final _client = ApiClient();

  Future<void> apply({required String shopName, required String feeCategory}) {
    return _client.post('/api/sellers/apply', body: {'shopName': shopName, 'feeCategory': feeCategory});
  }
}
