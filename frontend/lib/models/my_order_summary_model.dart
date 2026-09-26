import 'order_model.dart';

enum MyOrderStatus { inProgress, delivered, cancelled }

extension MyOrderStatusLabel on MyOrderStatus {
  String get label {
    switch (this) {
      case MyOrderStatus.inProgress:
        return 'En cours';
      case MyOrderStatus.delivered:
        return 'Livrée';
      case MyOrderStatus.cancelled:
        return 'Annulée';
    }
  }
}

/// One row in [MyOrdersScreen] — vue simplifiée d'un [OrderModel] réel pour
/// l'affichage en liste (le statut fin par sous-commande vit dans
/// `OrderModel.subOrders`, consulté sur l'écran de suivi).
class MyOrderSummaryModel {
  final String orderId;
  final String orderNumber;
  final DateTime dateTime;
  final num total;
  final MyOrderStatus status;
  final int shopCount;

  const MyOrderSummaryModel({
    required this.orderId,
    required this.orderNumber,
    required this.dateTime,
    required this.total,
    required this.status,
    required this.shopCount,
  });

  factory MyOrderSummaryModel.fromOrder(OrderModel order) {
    final status = switch (order.status) {
      'CANCELLED' || 'PAYMENT_FAILED' || 'REFUNDED' => MyOrderStatus.cancelled,
      'CLOSED' => MyOrderStatus.delivered,
      _ => MyOrderStatus.inProgress,
    };
    return MyOrderSummaryModel(
      orderId: order.id,
      orderNumber: order.orderNumber,
      dateTime: order.createdAt,
      total: order.totalAmount,
      status: status,
      shopCount: order.subOrderCount,
    );
  }
}
