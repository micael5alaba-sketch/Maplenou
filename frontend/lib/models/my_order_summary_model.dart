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

/// One row in [MyOrdersScreen]. Maps onto the backend's
/// `OrderSummaryResponse` (see `MODELE_DONNEES.md`).
class MyOrderSummaryModel {
  final String orderNumber;
  final DateTime dateTime;
  final num total;
  final MyOrderStatus status;
  final int itemCount;

  const MyOrderSummaryModel({
    required this.orderNumber,
    required this.dateTime,
    required this.total,
    required this.status,
    required this.itemCount,
  });
}
