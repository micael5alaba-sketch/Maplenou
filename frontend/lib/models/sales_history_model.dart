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

/// One row in [SalesHistoryScreen]'s transaction list. Miroir d'un
/// `SubOrderResponse` (`order/dto/SubOrderResponse.java`) déjà terminé
/// (DELIVERED, ou CANCELLED/RETURNED).
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

  factory SaleTransactionModel.fromJson(Map<String, dynamic> json) {
    final items = (json['items'] as List<dynamic>? ?? [])
        .map((i) => i as Map<String, dynamic>)
        .toList();
    final itemsSummary = items.isEmpty
        ? ''
        : items.length == 1
            ? '${items.first['quantity']}x ${items.first['productName']}'
            : '${items.first['quantity']}x ${items.first['productName']} + ${items.length - 1} autre${items.length > 2 ? 's' : ''}';

    return SaleTransactionModel(
      orderNumber: json['orderNumber'] as String,
      customerName: json['buyerName'] as String,
      itemsSummary: itemsSummary,
      dateTime: DateTime.parse(json['createdAt'] as String),
      amount: json['subtotal'] as num,
      status: json['status'] == 'DELIVERED' ? SaleStatus.completed : SaleStatus.cancelled,
    );
  }
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
