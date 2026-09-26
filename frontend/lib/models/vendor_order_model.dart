/// Status of an order from the seller's point of view — regroupe le cycle
/// fin de `SubOrderStatus` (`order/SubOrderStatus.java`) en 4 catégories
/// pour l'affichage/filtrage.
enum VendorOrderStatus { toPrepare, shipping, delivered, cancelled }

VendorOrderStatus _bucketForRawStatus(String rawStatus) {
  switch (rawStatus) {
    case 'PENDING':
    case 'PREPARING':
      return VendorOrderStatus.toPrepare;
    case 'READY_FOR_PICKUP':
    case 'IN_DELIVERY':
      return VendorOrderStatus.shipping;
    case 'DELIVERED':
      return VendorOrderStatus.delivered;
    default: // CANCELLED, RETURN_REQUESTED, RETURNED
      return VendorOrderStatus.cancelled;
  }
}

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
/// Miroir de `SubOrderResponse` (`order/dto/SubOrderResponse.java`).
class VendorOrderModel {
  final String id;
  final String orderNumber;
  final DateTime dateTime;
  final String customerName;
  final List<String> productNames;
  final String rawStatus;
  final num amount;

  const VendorOrderModel({
    required this.id,
    required this.orderNumber,
    required this.dateTime,
    required this.customerName,
    required this.productNames,
    required this.rawStatus,
    required this.amount,
  });

  VendorOrderStatus get status => _bucketForRawStatus(rawStatus);

  /// Le vendeur ne peut faire avancer une sous-commande que
  /// PENDING → PREPARING → READY_FOR_PICKUP (voir `SubOrderService`) —
  /// au-delà, c'est le livreur qui prend le relais.
  bool get canAdvance => rawStatus == 'PENDING' || rawStatus == 'PREPARING';

  String? get nextRawStatus {
    if (rawStatus == 'PENDING') return 'PREPARING';
    if (rawStatus == 'PREPARING') return 'READY_FOR_PICKUP';
    return null;
  }

  /// e.g. "Sac en cuir" for a single item, "Tissu Wax + 2 autres" for
  /// several — always names the first product rather than just a count,
  /// so the seller sees at a glance what the order is about.
  String get productSummary {
    if (productNames.isEmpty) return '';
    if (productNames.length == 1) return productNames.first;
    final extra = productNames.length - 1;
    return '${productNames.first} + $extra autre${extra > 1 ? 's' : ''}';
  }

  factory VendorOrderModel.fromJson(Map<String, dynamic> json) {
    final items = json['items'] as List<dynamic>? ?? [];
    return VendorOrderModel(
      id: json['id'] as String,
      orderNumber: json['orderNumber'] as String,
      dateTime: DateTime.parse(json['createdAt'] as String),
      customerName: json['buyerName'] as String,
      productNames: items.map((i) => (i as Map<String, dynamic>)['productName'] as String).toList(),
      rawStatus: json['status'] as String,
      amount: json['subtotal'] as num,
    );
  }
}
