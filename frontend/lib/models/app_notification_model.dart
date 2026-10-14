import 'dart:convert';

/// Une notification push reçue sur cet appareil, conservée localement
/// (voir `NotificationHistoryService`) — le backend (`FcmService`) envoie en
/// fire-and-forget, sans historique côté serveur.
class AppNotification {
  final String title;
  final String body;
  final Map<String, dynamic> data;
  final DateTime receivedAt;
  bool read;

  AppNotification({
    required this.title,
    required this.body,
    required this.data,
    required this.receivedAt,
    this.read = false,
  });

  Map<String, dynamic> toJson() => {
        'title': title,
        'body': body,
        'data': data,
        'receivedAt': receivedAt.toIso8601String(),
        'read': read,
      };

  String encode() => jsonEncode(toJson());

  factory AppNotification.fromJson(Map<String, dynamic> json) {
    return AppNotification(
      title: json['title'] as String? ?? 'Maplenou',
      body: json['body'] as String? ?? '',
      data: (json['data'] as Map?)?.cast<String, dynamic>() ?? const {},
      receivedAt: DateTime.tryParse(json['receivedAt'] as String? ?? '') ?? DateTime.now(),
      read: json['read'] as bool? ?? false,
    );
  }

  factory AppNotification.decode(String raw) =>
      AppNotification.fromJson(jsonDecode(raw) as Map<String, dynamic>);
}
