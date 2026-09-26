/// Statut de livraison d'une sous-commande, vu par l'acheteur. Miroir de
/// `DeliveryResponse` (`delivery/dto/DeliveryResponse.java`) — juste les
/// champs utiles côté suivi de commande (pas ceux réservés au livreur/admin).
class DeliveryStatusModel {
  final String id;
  final String status;
  final String? agentId;
  final String? agentName;
  final String? proofUrl;
  final DateTime? deliveredAt;

  const DeliveryStatusModel({
    required this.id,
    required this.status,
    this.agentId,
    this.agentName,
    this.proofUrl,
    this.deliveredAt,
  });

  factory DeliveryStatusModel.fromJson(Map<String, dynamic> json) {
    return DeliveryStatusModel(
      id: json['id'] as String,
      status: json['status'] as String,
      agentId: json['agentId'] as String?,
      agentName: json['agentName'] as String?,
      proofUrl: json['proofUrl'] as String?,
      deliveredAt: json['deliveredAt'] != null ? DateTime.parse(json['deliveredAt'] as String) : null,
    );
  }
}
