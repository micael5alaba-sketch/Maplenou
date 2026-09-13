/// Status of a [SaleTransactionModel] — a completed sale's history entry
/// only ever ends up "completed" or "cancelled" (unlike the live
/// [VendorOrderStatus] which still has in-progress states).
enum SaleStatus { completed, cancelled }

extension SaleStatusLabel on SaleStatus {
  String get label {
    switch (this) {
      case SaleStatus.completed:
        return 'Terminé';
      case SaleStatus.cancelled:
        return 'Annulé';
    }
  }
}

/// One row in [SalesHistoryScreen]'s transaction list.
class SaleTransactionModel {
  final String orderNumber;
  final String customerName;
  final String itemsSummary;
  final DateTime dateTime;
  final num amount;
  final SaleStatus status;

  const SaleTransactionModel({
    required this.orderNumber,
    required this.customerName,
    required this.itemsSummary,
    required this.dateTime,
    required this.amount,
    required this.status,
  });
}

/// Everything shown on [SalesHistoryScreen].
class SalesHistoryModel {
  final num totalSales;
  final int ordersProcessed;
  final List<SaleTransactionModel> transactions;

  const SalesHistoryModel({
    required this.totalSales,
    required this.ordersProcessed,
    required this.transactions,
  });
}
