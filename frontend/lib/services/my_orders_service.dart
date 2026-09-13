import '../models/my_order_summary_model.dart';

/// Provides the buyer's order list shown on [MyOrdersScreen].
///
/// Mocked for now — no network call. Maps directly onto the real, already
/// working `GET /api/orders` once auth is wired up.
class MyOrdersService {
  List<MyOrderSummaryModel> getMyOrders() {
    return [
      MyOrderSummaryModel(
        orderNumber: 'MPL-9928',
        dateTime: DateTime.now().subtract(const Duration(hours: 2)),
        total: 204000,
        status: MyOrderStatus.inProgress,
        itemCount: 3,
      ),
      MyOrderSummaryModel(
        orderNumber: 'MPL-9871',
        dateTime: DateTime.now().subtract(const Duration(days: 6)),
        total: 45000,
        status: MyOrderStatus.delivered,
        itemCount: 1,
      ),
      MyOrderSummaryModel(
        orderNumber: 'MPL-9740',
        dateTime: DateTime.now().subtract(const Duration(days: 14)),
        total: 32000,
        status: MyOrderStatus.cancelled,
        itemCount: 1,
      ),
    ];
  }
}
