import '../models/seller_fee_model.dart';
import 'api_client.dart';

/// `sellerfee/SellerFeeController.java` — frais mensuel d'activité vendeur
/// (catégorie, montant dû, déductions) et demande de changement de catégorie.
class SellerFeeService {
  final _client = ApiClient();

  Future<SellerFeeSummaryModel> getSummary() async {
    final data = await _client.get('/api/seller/fees/summary') as Map<String, dynamic>;
    return SellerFeeSummaryModel.fromJson(data);
  }

  Future<List<SellerMonthlyFeeModel>> getFeeHistory({int size = 20}) async {
    final data = await _client.get('/api/seller/fees/history', query: {'size': '$size'}) as Map<String, dynamic>;
    final content = data['content'] as List<dynamic>? ?? [];
    return content.map((json) => SellerMonthlyFeeModel.fromJson(json as Map<String, dynamic>)).toList();
  }

  Future<void> requestCategoryChange(String requestedCategory) {
    return _client.post('/api/seller/fees/category-change', body: {'requestedCategory': requestedCategory});
  }
}
