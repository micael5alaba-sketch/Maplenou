import '../models/vendor_order_model.dart';
import 'api_client.dart';

/// `GET/PATCH /api/sub-orders` (`order/SubOrderController.java`, rôle SELLER).
///
/// Le filtrage par statut n'existe pas côté backend sur cet endpoint (juste
/// la pagination) — on récupère donc tout (volume borné à une seule
/// boutique, pas le catalogue public) puis on filtre/pagine côté client,
/// pour garder la pagination par page de 6 déjà en place côté écran.
class VendorOrdersService {
  static const int pageSize = 6;
  static const int _fetchSize = 200;

  final _client = ApiClient();

  Future<List<VendorOrderModel>> getOrders({
    VendorOrderStatus? status,
    required int page,
  }) async {
    final data = await _client.get(
      '/api/sub-orders',
      query: {'size': '$_fetchSize'},
    ) as Map<String, dynamic>;
    final content = data['content'] as List<dynamic>? ?? [];
    final allOrders = content.map((json) => VendorOrderModel.fromJson(json as Map<String, dynamic>)).toList();

    final filtered = status == null ? allOrders : allOrders.where((o) => o.status == status).toList();

    final start = page * pageSize;
    if (start >= filtered.length) return [];
    final end = (start + pageSize).clamp(0, filtered.length);
    return filtered.sublist(start, end);
  }

  /// Détail complet d'une sous-commande (`SubOrderResponse`, avec le détail
  /// des articles) — pour [OrderDetailScreen].
  Future<Map<String, dynamic>> getSubOrderDetail(String subOrderId) async {
    return await _client.get('/api/sub-orders/$subOrderId') as Map<String, dynamic>;
  }

  Future<VendorOrderModel> advanceStatus(VendorOrderModel order) async {
    final nextStatus = order.nextRawStatus;
    if (nextStatus == null) {
      throw StateError('Cette commande ne peut plus être avancée par le vendeur.');
    }
    final data = await _client.patch(
      '/api/sub-orders/${order.id}/status',
      body: {'status': nextStatus},
    ) as Map<String, dynamic>;
    return VendorOrderModel.fromJson(data);
  }
}
