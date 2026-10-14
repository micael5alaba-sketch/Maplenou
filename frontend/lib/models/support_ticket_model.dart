/// A support ticket (courier or buyer side). Miroir de `SupportTicketResponse`
/// (`support/dto/SupportTicketResponse.java`).
enum SupportTicketStatus { open, inProgress, resolved }

extension SupportTicketStatusX on SupportTicketStatus {
  String get label {
    switch (this) {
      case SupportTicketStatus.open:
        return 'Ouvert';
      case SupportTicketStatus.inProgress:
        return 'En cours';
      case SupportTicketStatus.resolved:
        return 'Résolu';
    }
  }

  static SupportTicketStatus fromApiValue(String value) {
    switch (value) {
      case 'IN_PROGRESS':
        return SupportTicketStatus.inProgress;
      case 'RESOLVED':
        return SupportTicketStatus.resolved;
      default:
        return SupportTicketStatus.open;
    }
  }
}

class SupportTicketModel {
  final String id;
  final String ticketNumber;
  final String category;
  final String subject;
  final String description;
  final String? relatedDeliveryId;
  final String? photoUrl;
  final SupportTicketStatus status;
  final String? resolutionNotes;
  final DateTime createdAt;

  const SupportTicketModel({
    required this.id,
    required this.ticketNumber,
    required this.category,
    required this.subject,
    required this.description,
    this.relatedDeliveryId,
    this.photoUrl,
    required this.status,
    this.resolutionNotes,
    required this.createdAt,
  });

  factory SupportTicketModel.fromJson(Map<String, dynamic> json) {
    return SupportTicketModel(
      id: json['id'] as String,
      ticketNumber: json['ticketNumber'] as String,
      category: json['category'] as String,
      subject: json['subject'] as String,
      description: json['description'] as String,
      relatedDeliveryId: json['relatedDeliveryId'] as String?,
      photoUrl: json['photoUrl'] as String?,
      status: SupportTicketStatusX.fromApiValue(json['status'] as String),
      resolutionNotes: json['resolutionNotes'] as String?,
      createdAt: DateTime.parse(json['createdAt'] as String),
    );
  }
}
