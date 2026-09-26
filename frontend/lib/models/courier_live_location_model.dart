/// Position GPS temps réel du livreur, publiée sur Firebase Realtime
/// Database (voir `notification/FirebaseRealtimeDbStructure.java` côté
/// backend pour la structure exacte : `deliveries/{deliveryId}/location`).
class CourierLiveLocationModel {
  final double lat;
  final double lng;
  final double? heading;
  final DateTime updatedAt;

  const CourierLiveLocationModel({
    required this.lat,
    required this.lng,
    this.heading,
    required this.updatedAt,
  });

  factory CourierLiveLocationModel.fromMap(Map<dynamic, dynamic> map) {
    return CourierLiveLocationModel(
      lat: (map['lat'] as num).toDouble(),
      lng: (map['lng'] as num).toDouble(),
      heading: (map['heading'] as num?)?.toDouble(),
      updatedAt: DateTime.fromMillisecondsSinceEpoch((map['updatedAt'] as num).toInt()),
    );
  }
}
