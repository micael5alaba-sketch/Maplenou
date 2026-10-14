/// Une livraison assignée au livreur connecté. Miroir de `DeliveryResponse`
/// (`delivery/dto/DeliveryResponse.java`).
class DeliveryModel {
  final String id;
  final String subOrderId;
  final String orderNumber;
  final String shopName;
  final String status;
  final String? proofUrl;
  final String? notes;
  final DateTime? assignedAt;
  final DateTime? deliveredAt;
  final DateTime createdAt;

  const DeliveryModel({
    required this.id,
    required this.subOrderId,
    required this.orderNumber,
    required this.shopName,
    required this.status,
    this.proofUrl,
    this.notes,
    this.assignedAt,
    this.deliveredAt,
    required this.createdAt,
  });

  factory DeliveryModel.fromJson(Map<String, dynamic> json) {
    return DeliveryModel(
      id: json['id'] as String,
      subOrderId: json['subOrderId'] as String,
      orderNumber: json['orderNumber'] as String,
      shopName: json['shopName'] as String,
      status: json['status'] as String,
      proofUrl: json['proofUrl'] as String?,
      notes: json['notes'] as String?,
      assignedAt: json['assignedAt'] != null ? DateTime.parse(json['assignedAt'] as String) : null,
      deliveredAt: json['deliveredAt'] != null ? DateTime.parse(json['deliveredAt'] as String) : null,
      createdAt: DateTime.parse(json['createdAt'] as String),
    );
  }
}

/// Un point de retrait ou de livraison. Miroir de `NavigationResponse`
/// (`delivery/dto/NavigationResponse.java`).
class NavigationPointModel {
  final String label;
  final String address;
  final double? latitude;
  final double? longitude;

  const NavigationPointModel({
    required this.label,
    required this.address,
    this.latitude,
    this.longitude,
  });

  bool get hasLocation => latitude != null && longitude != null;
}

class DeliveryNavigationModel {
  final NavigationPointModel pickup;
  final NavigationPointModel dropoff;

  /// Gain net estimé pour le livreur (75% du tarif de la zone de livraison,
  /// voir `CourierEarningService`) — affiché dès que la livraison est
  /// assignée, pas seulement une fois terminée.
  final double? estimatedEarning;

  const DeliveryNavigationModel({required this.pickup, required this.dropoff, this.estimatedEarning});

  factory DeliveryNavigationModel.fromJson(Map<String, dynamic> json) {
    final pickup = json['pickup'] as Map<String, dynamic>;
    final dropoff = json['dropoff'] as Map<String, dynamic>;
    return DeliveryNavigationModel(
      pickup: NavigationPointModel(
        label: pickup['shopName'] as String,
        address: pickup['address'] as String,
        latitude: (pickup['latitude'] as num?)?.toDouble(),
        longitude: (pickup['longitude'] as num?)?.toDouble(),
      ),
      dropoff: NavigationPointModel(
        label: dropoff['clientName'] as String,
        address: dropoff['address'] as String,
        latitude: (dropoff['latitude'] as num?)?.toDouble(),
        longitude: (dropoff['longitude'] as num?)?.toDouble(),
      ),
      estimatedEarning: (json['estimatedEarning'] as num?)?.toDouble(),
    );
  }
}
