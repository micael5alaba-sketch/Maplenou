import '../models/sales_history_model.dart';
import 'api_client.dart';

/// `GET /api/sub-orders` (`order/SubOrderController.java`, rôle SELLER),
/// filtré côté client sur les sous-commandes terminées (DELIVERED, ou
/// CANCELLED/RETURNED) — le backend ne prend pas de paramètre de statut sur
/// cet endpoint, le volume par boutique reste bas.
class SalesHistoryService {
  final _client = ApiClient();

  Future<SalesHistoryModel> getHistory() async {
    final data = await _client.get('/api/sub-orders', query: {'size': '200'}) as Map<String, dynamic>;
    final content = data['content'] as List<dynamic>? ?? [];

    final finished = content
        .cast<Map<String, dynamic>>()
        .where((json) => const ['DELIVERED', 'CANCELLED', 'RETURNED'].contains(json['status'] as String))
        .toList();

    final transactions = finished.map(SaleTransactionModel.fromJson).toList();
    final delivered = transactions.where((t) => t.status == SaleStatus.completed);

    return SalesHistoryModel(
      totalSales: delivered.fold<num>(0, (sum, t) => sum + t.amount),
      ordersProcessed: delivered.length,
      transactions: transactions,
    );
  }
}
