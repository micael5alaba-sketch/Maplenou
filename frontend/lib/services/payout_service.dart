import '../models/wallet_model.dart';
import 'api_client.dart';

/// `payout/PayoutController.java` + `PayoutMethodController.java` —
/// reversements et moyens de paiement du vendeur connecté.
class PayoutService {
  final _client = ApiClient();

  Future<List<WithdrawalModel>> listMyPayouts({int size = 50}) async {
    final data = await _client.get('/api/seller/payouts', query: {'size': '$size'}) as Map<String, dynamic>;
    final content = data['content'] as List<dynamic>? ?? [];
    return content.map((json) => WithdrawalModel.fromJson(json as Map<String, dynamic>)).toList();
  }

  Future<List<PaymentMethodModel>> getPayoutMethods() async {
    final data = await _client.get('/api/seller/payout-methods') as List<dynamic>;
    return data.map((json) => PaymentMethodModel.fromJson(json as Map<String, dynamic>)).toList();
  }

  Future<void> addPayoutMethod({required String type, required String accountNumber}) {
    return _client.post('/api/seller/payout-methods', body: {'type': type, 'accountNumber': accountNumber});
  }

  Future<void> setDefaultPayoutMethod(String id) => _client.patch('/api/seller/payout-methods/$id/default');

  Future<void> deletePayoutMethod(String id) => _client.delete('/api/seller/payout-methods/$id');
}
