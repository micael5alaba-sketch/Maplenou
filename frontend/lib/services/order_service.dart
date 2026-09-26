import '../models/order_model.dart';
import 'api_client.dart';

/// `order/OrderController.java` — création et consultation des commandes
/// acheteur (`/api/orders`).
class OrderService {
  final _client = ApiClient();

  /// Passe une commande à partir du panier courant. Le paiement réel
  /// (FedaPay/Stripe) est explicitement reporté (`CLAUDE.md` §7) : la
  /// commande est créée avec le statut `CREATED`, pas encore `PAID`.
  Future<OrderModel> placeOrder({required String addressId}) async {
    final data = await _client.post(
      '/api/orders',
      body: {'addressId': addressId},
    ) as Map<String, dynamic>;
    return OrderModel.fromJson(data);
  }

  Future<OrderModel> getOrder(String orderId) async {
    final data = await _client.get('/api/orders/$orderId') as Map<String, dynamic>;
    return OrderModel.fromJson(data);
  }

  /// Mes commandes (`GET /api/orders`, paginé — `OrderSummaryResponse`,
  /// pas de détail par sous-commande, voir `OrderModel.fromJson`).
  Future<List<OrderModel>> listMyOrders({int size = 20}) async {
    final data = await _client.get('/api/orders', query: {'size': '$size'}) as Map<String, dynamic>;
    final content = data['content'] as List<dynamic>? ?? [];
    return content.map((json) => OrderModel.fromJson(json as Map<String, dynamic>)).toList();
  }

  /// Nombre de commandes en cours (`GET /api/orders/summary`) — utilisé
  /// pour le badge "N en cours" sur "Mes commandes" du profil.
  Future<int> getOngoingCount() async {
    final data = await _client.get('/api/orders/summary') as Map<String, dynamic>;
    return data['ongoing'] as int? ?? 0;
  }
}
