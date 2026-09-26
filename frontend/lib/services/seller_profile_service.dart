import '../models/seller_profile_model.dart';
import 'shop_service.dart';
import 'user_service.dart';

/// `GET /api/shops/mine` (`catalog/ShopController.java`), plus l'email du
/// compte (`GET /api/users/me`) — `Shop` n'a pas de champ email de contact
/// dédié, on affiche celui du compte vendeur.
class SellerProfileService {
  final _shopService = ShopService();
  final _userService = UserService();

  Future<SellerProfileModel> getProfile() async {
    final shop = await _shopService.getMine();
    final user = await _userService.getCurrentUser();

    if (shop == null) {
      throw StateError('Profil vendeur consulté sans boutique créée — ne devrait pas arriver ici.');
    }

    return SellerProfileModel(
      shopName: shop.name,
      logoUrl: shop.logoUrl,
      isCertified: shop.isApproved,
      location: shop.location,
      memberSinceYear: shop.createdAt.year,
      contactEmail: user.email ?? user.phoneNumber,
      taxId: shop.taxId,
    );
  }
}
