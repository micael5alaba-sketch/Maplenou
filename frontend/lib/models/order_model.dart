/// Une sous-commande au sein d'une [OrderModel] (une par boutique). Miroir
/// de `SubOrderSummary` (`order/dto/SubOrderSummary.java`).
class SubOrderSummaryModel {
  final String id;
  final String shopId;
  final String shopName;
  final String status;
  final num subtotal;
  final DateTime createdAt;

  const SubOrderSummaryModel({
    required this.id,
    required this.shopId,
    required this.shopName,
    required this.status,
    required this.subtotal,
    required this.createdAt,
  });

  factory SubOrderSummaryModel.fromJson(Map<String, dynamic> json) {
    return SubOrderSummaryModel(
      id: json['id'] as String,
      shopId: json['shopId'] as String,
      shopName: json['shopName'] as String,
      status: json['status'] as String,
      subtotal: json['subtotal'] as num,
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

  const OrderDeliveryAddressModel({this.label, required this.city, this.district, this.details});

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
    );
  }
}
