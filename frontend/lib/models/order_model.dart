/// Un article au sein d'une [SubOrderSummaryModel]. Miroir de
/// `SubOrderItemResponse` (`order/dto/SubOrderItemResponse.java`) —
/// uniquement présent sur le détail d'une commande (`GET /api/orders/{id}`),
/// pas sur la liste, pour garder "Mes commandes" léger.
class OrderItemModel {
  final String productName;
  final String variantLabel;
  final int quantity;
  final num unitPrice;
  final num lineTotal;

  const OrderItemModel({
    required this.productName,
    required this.variantLabel,
    required this.quantity,
    required this.unitPrice,
    required this.lineTotal,
  });

  factory OrderItemModel.fromJson(Map<String, dynamic> json) {
    return OrderItemModel(
      productName: json['productName'] as String,
      variantLabel: json['variantLabel'] as String,
      quantity: json['quantity'] as int,
      unitPrice: json['unitPrice'] as num,
      lineTotal: json['lineTotal'] as num,
    );
  }
}

/// Une sous-commande au sein d'une [OrderModel] (une par boutique). Miroir
/// de `SubOrderSummary` (`order/dto/SubOrderSummary.java`).
class SubOrderSummaryModel {
  final String id;
  final String shopId;
  final String shopName;
  final String status;
  final num subtotal;
  final List<OrderItemModel> items;
  final DateTime createdAt;

  const SubOrderSummaryModel({
    required this.id,
    required this.shopId,
    required this.shopName,
    required this.status,
    required this.subtotal,
    this.items = const [],
    required this.createdAt,
  });

  factory SubOrderSummaryModel.fromJson(Map<String, dynamic> json) {
    final items = json['items'] as List<dynamic>?;
    return SubOrderSummaryModel(
      id: json['id'] as String,
      shopId: json['shopId'] as String,
      shopName: json['shopName'] as String,
      status: json['status'] as String,
      subtotal: json['subtotal'] as num,
      items: items == null ? const [] : items.map((i) => OrderItemModel.fromJson(i as Map<String, dynamic>)).toList(),
      createdAt: DateTime.parse(json['createdAt'] as String),
    );
  }
}

/// Adresse de livraison figée à la commande. Miroir de `DeliveryAddressDto`
/// (`order/dto/OrderResponse.java`) — absent sur `OrderSummaryResponse`
/// (liste), présent seulement sur le détail d'une commande.
class OrderDeliveryAddressModel {
  final String? label;
  final String city;
  final String? district;
  final String? details;
  final double? latitude;
  final double? longitude;

  const OrderDeliveryAddressModel({
    this.label,
    required this.city,
    this.district,
    this.details,
    this.latitude,
    this.longitude,
  });

  String get summary {
    final parts = [city, if (district != null && district!.isNotEmpty) district!];
    return parts.join(', ');
  }

  factory OrderDeliveryAddressModel.fromJson(Map<String, dynamic> json) {
    return OrderDeliveryAddressModel(
      label: json['label'] as String?,
      city: json['city'] as String,
      district: json['district'] as String?,
      details: json['details'] as String?,
      latitude: (json['latitude'] as num?)?.toDouble(),
      longitude: (json['longitude'] as num?)?.toDouble(),
    );
  }
}

/// Miroir de `OrderResponse` (`order/dto/OrderResponse.java`) — sert aussi
/// à parser `OrderSummaryResponse` (liste `GET /api/orders`), plus léger
/// (pas de détail par sous-commande ni d'adresse, juste [subOrderCount]).
class OrderModel {
  final String id;
  final String status;
  final num totalAmount;
  final num discountAmount;
  final String? paymentReference;
  final OrderDeliveryAddressModel? deliveryAddress;
  final List<SubOrderSummaryModel> subOrders;
  final int subOrderCount;
  final DateTime createdAt;

  final String orderNumber;

  /// URL de paiement FedaPay à ouvrir juste après la création de la commande
  /// (voir `OrderService.placeOrder` côté backend) — null si FedaPay est
  /// désactivé (`app.fedapay.enabled=false`, cas par défaut hors production)
  /// ou sur toute relecture ultérieure de la commande (pas régénéré).
  final String? paymentUrl;

  const OrderModel({
    required this.id,
    required this.orderNumber,
    required this.status,
    required this.totalAmount,
    required this.discountAmount,
    this.paymentReference,
    this.deliveryAddress,
    required this.subOrders,
    required this.subOrderCount,
    required this.createdAt,
    this.paymentUrl,
  });

  factory OrderModel.fromJson(Map<String, dynamic> json) {
    final subOrders = (json['subOrders'] as List<dynamic>? ?? [])
        .map((s) => SubOrderSummaryModel.fromJson(s as Map<String, dynamic>))
        .toList();
    final addressJson = json['deliveryAddress'] as Map<String, dynamic>?;
    return OrderModel(
      id: json['id'] as String,
      orderNumber: json['orderNumber'] as String,
      status: json['status'] as String,
      totalAmount: json['totalAmount'] as num,
      discountAmount: json['discountAmount'] as num? ?? 0,
      paymentReference: json['paymentReference'] as String?,
      deliveryAddress: addressJson != null ? OrderDeliveryAddressModel.fromJson(addressJson) : null,
      subOrders: subOrders,
      subOrderCount: json['subOrderCount'] as int? ?? subOrders.length,
      createdAt: DateTime.parse(json['createdAt'] as String),
      paymentUrl: json['paymentUrl'] as String?,
    );
  }
}
