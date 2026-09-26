import '../models/wallet_model.dart';
import 'api_client.dart';
import 'payout_service.dart';
import 'review_service.dart';
import 'shop_service.dart';

/// Assemble le portefeuille vendeur depuis trois sources réelles : solde
/// disponible (`GET /api/sellers/me/kpis`), reversements + moyens de
/// paiement (`PayoutService`), et note moyenne boutique (`ReviewService`).
class WalletService {
  final _client = ApiClient();
  final _payoutService = PayoutService();
  final _shopService = ShopService();
  final _reviewService = ReviewService();

  Future<WalletModel> getWallet() async {
    final shop = await _shopService.getMine();
    if (shop == null) {
      throw StateError('Portefeuille consulté sans boutique créée — ne devrait pas arriver ici.');
    }

    final kpiData = await _client.get('/api/sellers/me/kpis') as Map<String, dynamic>;
    final withdrawals = await _payoutService.listMyPayouts();
    final paymentMethods = await _payoutService.getPayoutMethods();

    double averageRating = 0;
    try {
      averageRating = await _reviewService.getShopAverageRating(shop.id);
    } catch (_) {
      // Le portefeuille doit s'afficher même si la note échoue à charger.
    }

    final paid = withdrawals.where((w) => w.status == WithdrawalStatus.completed);
    final pending = withdrawals.where((w) => w.status == WithdrawalStatus.pending);

    return WalletModel(
      availableBalance: kpiData['availableBalance'] as num,
      paymentMethods: paymentMethods,
      totalPaidOut: paid.fold<num>(0, (sum, w) => sum + w.amount),
      paidPayoutsCount: paid.length,
      pendingAmount: pending.fold<num>(0, (sum, w) => sum + w.amount),
      averageRating: averageRating,
      withdrawals: withdrawals,
    );
  }
}
