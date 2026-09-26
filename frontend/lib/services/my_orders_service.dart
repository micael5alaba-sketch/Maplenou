import '../models/my_order_summary_model.dart';
import 'order_service.dart';

/// `GET /api/orders` (`order/OrderController.java`), via [OrderService].
class MyOrdersService {
  final _orderService = OrderService();

  Future<List<MyOrderSummaryModel>> getMyOrders() async {
    final orders = await _orderService.listMyOrders();
    return orders.map(MyOrderSummaryModel.fromOrder).toList();
  }
}
