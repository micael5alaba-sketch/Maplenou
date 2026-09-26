/// Un gain de livraison. Miroir de `CourierEarningResponse`
/// (`delivery/dto/CourierEarningResponse.java`).
class CourierEarningModel {
  final String id;
  final String deliveryId;
  final num amount;
  final String status;
  final DateTime? paidAt;
  final DateTime createdAt;

  const CourierEarningModel({
    required this.id,
    required this.deliveryId,
    required this.amount,
    required this.status,
    this.paidAt,
    required this.createdAt,
  });

  factory CourierEarningModel.fromJson(Map<String, dynamic> json) {
    return CourierEarningModel(
      id: json['id'] as String,
      deliveryId: json['deliveryId'] as String,
      amount: json['amount'] as num,
      status: json['status'] as String,
      paidAt: json['paidAt'] != null ? DateTime.parse(json['paidAt'] as String) : null,
      createdAt: DateTime.parse(json['createdAt'] as String),
    );
  }
}
