/// Une notification, lue depuis l'historique serveur (`GET /api/notifications`,
/// `notification/AppNotification.java` côté backend) — plus de stockage local
/// uniquement : visible après réinstallation de l'app et sur tout appareil connecté
/// au même compte.
class AppNotification {
  final String id;
  final String title;
  final String body;
  final String type;
  final String? referenceId;
  final DateTime receivedAt;
  final bool read;

  const AppNotification({
    required this.id,
    required this.title,
    required this.body,
    required this.type,
    this.referenceId,
    required this.receivedAt,
    required this.read,
  });

  factory AppNotification.fromJson(Map<String, dynamic> json) {
    return AppNotification(
      id: json['id'] as String,
      title: json['title'] as String? ?? 'Maplenou',
      body: json['body'] as String? ?? '',
      type: json['type'] as String? ?? '',
      referenceId: json['referenceId'] as String?,
      receivedAt: DateTime.tryParse(json['createdAt'] as String? ?? '') ?? DateTime.now(),
      read: json['read'] as bool? ?? false,
    );
  }
}
