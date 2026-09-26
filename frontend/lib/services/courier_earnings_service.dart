import '../models/courier_earning_model.dart';
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

  Future<num> getAvailableBalance() async {
    final data = await _client.get('/api/delivery-agents/me/earnings/balance') as Map<String, dynamic>;
    return data['availableBalance'] as num;
  }
}
