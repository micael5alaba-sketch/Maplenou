import '../models/courier_earning_model.dart';
import '../models/courier_withdrawal_model.dart';
import 'api_client.dart';

/// `delivery/CourierEarningController.java` — gains du livreur connecté
/// (`GET /api/delivery-agents/me/earnings[/balance]`).
class CourierEarningsService {
  final _client = ApiClient();

  Future<List<CourierEarningModel>> getEarnings({int size = 100}) async {
    final data =
        await _client.get('/api/delivery-agents/me/earnings', query: {'size': '$size'}) as Map<String, dynamic>;
    final content = data['content'] as List<dynamic>? ?? [];
    return content.map((json) => CourierEarningModel.fromJson(json as Map<String, dynamic>)).toList();
  }

  /// Demande le retrait de tous les gains disponibles (`POST /api/delivery-agents/me/withdrawals`).
  /// Le retrait suit ensuite le circuit de l'équipe : vérification, soumission, approbation du
  /// Super Admin, paiement.
  Future<void> requestWithdrawal(String paymentDetails) async {
    await _client.post('/api/delivery-agents/me/withdrawals', body: {'paymentDetails': paymentDetails});
  }

  Future<List<CourierWithdrawalModel>> getWithdrawals({int size = 20}) async {
    final data =
        await _client.get('/api/delivery-agents/me/withdrawals', query: {'size': '$size'}) as Map<String, dynamic>;
    final content = data['content'] as List<dynamic>? ?? [];
    return content.map((json) => CourierWithdrawalModel.fromJson(json as Map<String, dynamic>)).toList();
  }

  Future<num> getAvailableBalance() async {
    final data = await _client.get('/api/delivery-agents/me/earnings/balance') as Map<String, dynamic>;
    return data['availableBalance'] as num;
  }
}
