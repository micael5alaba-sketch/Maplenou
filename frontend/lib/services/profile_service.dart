import '../models/user_profile_model.dart';
import 'address_service.dart';
import 'order_service.dart';
import 'user_service.dart';

/// Assemble le profil affiché sur [ProfileScreen] à partir de trois vrais
/// endpoints : `GET /api/users/me` (identité), `GET /api/orders/summary`
/// (badge "commandes en cours") et `GET /api/users/me/addresses` (adresse
/// par défaut). `paymentMethodsSummary` est un texte indicatif seulement :
/// il n'y a pas de "moyen de paiement enregistré" côté acheteur (FedaPay ne
/// mémorise rien), le vrai choix (pays + T-Money/Flooz/Carte) se fait à
/// l'étape "Paiement" du panier — le sous-titre le dit explicitement pour
/// éviter de laisser croire qu'on peut le configurer depuis le profil.
class ProfileService {
  final _userService = UserService();
  final _orderService = OrderService();
  final _addressService = AddressService();

  Future<UserProfileModel> getCurrentProfile() async {
    final user = await _userService.getCurrentUser();

    int ongoingOrdersCount = 0;
    try {
      ongoingOrdersCount = await _orderService.getOngoingCount();
    } catch (_) {
      // Le profil doit s'afficher même si ce badge échoue à charger.
    }

    String primaryAddressSummary = 'Aucune adresse enregistrée';
    try {
      final addresses = await _addressService.getAddresses();
      if (addresses.isNotEmpty) {
        final defaultAddress = addresses.firstWhere((a) => a.isDefault, orElse: () => addresses.first);
        primaryAddressSummary = defaultAddress.summary;
      }
    } catch (_) {
      // Idem : une adresse illisible ne doit pas bloquer tout le profil.
    }

    return UserProfileModel(
      fullName: user.fullName,
      email: user.email ?? user.phoneNumber,
      avatarUrl: user.avatarUrl,
      isVerified: user.phoneVerified,
      ongoingOrdersCount: ongoingOrdersCount,
      primaryAddressSummary: primaryAddressSummary,
      paymentMethodsSummary: 'Choisi à l\'étape paiement (T-Money, Flooz, Carte)',
      isApprovedSeller: user.isApprovedSeller,
      hasPendingSellerApplication: user.hasPendingSellerApplication,
    );
  }
}
