/// Status of an order from the seller's point of view.
enum VendorOrderStatus { toPrepare, shipping, delivered, cancelled }

extension VendorOrderStatusLabels on VendorOrderStatus {
  /// Shown on the order card's status badge.
  String get badgeLabel {
    switch (this) {
      case VendorOrderStatus.toPrepare:
        return 'À préparer';
      case VendorOrderStatus.shipping:
        return 'En livraison';
      case VendorOrderStatus.delivered:
        return 'Livrée';
      case VendorOrderStatus.cancelled:
        return 'Annulée';
    }
  }

  /// Shown on the filter chip — a slightly different wording than the
  /// badge (e.g. "Terminées" groups delivered orders).
  String get filterLabel {
    switch (this) {
      case VendorOrderStatus.toPrepare:
        return 'À préparer';
      case VendorOrderStatus.shipping:
        return 'En cours de livraison';
      case VendorOrderStatus.delivered:
        return 'Terminées';
      case VendorOrderStatus.cancelled:
        return 'Annulées';
    }
  }
}

/// One order received by the seller, as shown on [OrdersManagementScreen].
class VendorOrderModel {
  final String id;
  final String orderNumber;
  final DateTime dateTime;
  final String customerName;
  final List<String> productNames;
  final VendorOrderStatus status;
  final num amount;

  const VendorOrderModel({
    required this.id,
    required this.orderNumber,
    required this.dateTime,
    required this.customerName,
    required this.productNames,
    required this.status,
    required this.amount,
  });

  /// e.g. "Sac en cuir" for a single item, "Tissu Wax + 2 autres" for
  /// several — always names the first product rather than just a count,
  /// so the seller sees at a glance what the order is about.
  String get productSummary {
    if (productNames.isEmpty) return '';
    if (productNames.length == 1) return productNames.first;
    final extra = productNames.length - 1;
    return '${productNames.first} + $extra autre${extra > 1 ? 's' : ''}';
  }
}
