/// Retrait des gains du livreur (`delivery/dto/CourierWithdrawalResponse.java`). Suit le circuit
/// de l'équipe : reçu, préparé, soumis, approuvé par le Super Admin, payé (ou rejeté / à corriger).
class CourierWithdrawalModel {
  final String id;
  final num amount;
  final String status;
  final String paymentDetails;
  final DateTime createdAt;

  const CourierWithdrawalModel({
    required this.id,
    required this.amount,
    required this.status,
    required this.paymentDetails,
    required this.createdAt,
  });

  factory CourierWithdrawalModel.fromJson(Map<String, dynamic> json) {
    return CourierWithdrawalModel(
      id: json['id'] as String,
      amount: json['amount'] as num,
      status: json['status'] as String,
      paymentDetails: json['paymentDetails'] as String? ?? '',
      createdAt: DateTime.parse(json['createdAt'] as String),
    );
  }

  String get statusLabel => switch (status) {
        'PENDING' => 'Reçu',
        'PREPARED' => 'En vérification',
        'SUBMITTED' => "En attente d'approbation",
        'NEEDS_CORRECTION' => 'À corriger',
        'APPROVED' => 'Approuvé',
        'PROCESSING' => 'Virement en cours',
        'PAID' => 'Payé',
        'REJECTED' => 'Rejeté',
        'FAILED' => 'Échec du virement',
        _ => status,
      };
}
