import '../models/seller_profile_model.dart';

/// Provides the shop identity and settings shown on [SellerProfileScreen].
///
/// Mocked for now — no network call. Replace [getProfile] with a real
/// `GET /api/shops/mine` call once wired up; the return shape maps
/// directly onto `ShopResponse` (`taxId` included).
class SellerProfileService {
  SellerProfileModel getProfile() {
    return const SellerProfileModel(
      shopName: 'Atelier Terre & Racine',
      isCertified: true,
      location: 'Dakar, Sénégal',
      memberSinceYear: 2023,
      contactEmail: 'contact@terreracine.sn',
      taxId: null,
    );
  }
}
