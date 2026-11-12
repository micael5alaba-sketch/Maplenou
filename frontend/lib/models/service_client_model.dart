// Modèles du Service client (`ServiceClient/`, API `/api/v1/support`). Distincts de l'ancien module de tickets
// (`support_ticket_model.dart`, Bakend `/api/support-tickets`) qui reste en lecture seule.

String _text(Object? value, [String fallback = '']) => value is String ? value : fallback;

class SupportCategoryModel {
  final String code;
  final String name;

  const SupportCategoryModel({required this.code, required this.name});

  factory SupportCategoryModel.fromJson(Map<String, dynamic> json) =>
      SupportCategoryModel(code: _text(json['code']), name: _text(json['name'], _text(json['code'])));
}

/// Libellés français des statuts d'origine ; un statut ajouté par l'administration s'affiche tel quel.
String supportStatusLabel(String status) => switch (status) {
      'NEW' => 'Nouveau',
      'ASSIGNED' => 'Pris en charge',
      'IN_PROGRESS' => 'En cours',
      'WAITING_FOR_CUSTOMER' => 'En attente de votre réponse',
      'RESOLVED' => 'Résolu',
      'CLOSED' => 'Clôturé',
      _ => status,
    };

class SupportTicketSummary {
  final String id;
  final String ticketNumber;
  final String subject;
  final String status;
  final String categoryName;
  final DateTime createdAt;
  final DateTime updatedAt;

  const SupportTicketSummary({
    required this.id,
    required this.ticketNumber,
    required this.subject,
    required this.status,
    required this.categoryName,
    required this.createdAt,
    required this.updatedAt,
  });

  factory SupportTicketSummary.fromJson(Map<String, dynamic> json) {
    final category = json['category'];
    return SupportTicketSummary(
      id: _text(json['id']),
      ticketNumber: _text(json['ticketNumber']),
      subject: _text(json['subject']),
      status: _text(json['status']),
      categoryName: category is Map<String, dynamic> ? _text(category['name']) : '',
      createdAt: DateTime.parse(_text(json['createdAt'])),
      updatedAt: DateTime.parse(_text(json['updatedAt'])),
    );
  }

  String get statusLabel => supportStatusLabel(status);

  bool get isClosedLike => status == 'RESOLVED' || status == 'CLOSED';
}

class SupportTicketDetail {
  final String id;
  final String ticketNumber;
  final String subject;
  final String description;
  final String status;
  final String categoryName;
  final String? orderRef;
  final String? deliveryRef;
  final List<String> allowedActions;
  final bool reopenPending;
  final int? rating;
  final DateTime createdAt;

  const SupportTicketDetail({
    required this.id,
    required this.ticketNumber,
    required this.subject,
    required this.description,
    required this.status,
    required this.categoryName,
    required this.orderRef,
    required this.deliveryRef,
    required this.allowedActions,
    required this.reopenPending,
    required this.rating,
    required this.createdAt,
  });

  factory SupportTicketDetail.fromJson(Map<String, dynamic> json) {
    final category = json['category'];
    final refs = json['references'];
    final satisfaction = json['satisfaction'];
    return SupportTicketDetail(
      id: _text(json['id']),
      ticketNumber: _text(json['ticketNumber']),
      subject: _text(json['subject']),
      description: _text(json['description']),
      status: _text(json['status']),
      categoryName: category is Map<String, dynamic> ? _text(category['name']) : '',
      orderRef: refs is Map<String, dynamic> ? refs['orderRef'] as String? : null,
      deliveryRef: refs is Map<String, dynamic> ? refs['deliveryRef'] as String? : null,
      allowedActions: (json['allowedActions'] as List<dynamic>? ?? []).map((e) => e.toString()).toList(),
      reopenPending: json['pendingReopenRequest'] != null,
      rating: satisfaction is Map<String, dynamic> ? (satisfaction['rating'] as num?)?.toInt() : null,
      createdAt: DateTime.parse(_text(json['createdAt'])),
    );
  }

  String get statusLabel => supportStatusLabel(status);

  bool can(String action) => allowedActions.contains(action);
}

class SupportMessage {
  final String id;
  final String authorName;
  final bool fromStaff;
  final String body;
  final DateTime createdAt;

  const SupportMessage({
    required this.id,
    required this.authorName,
    required this.fromStaff,
    required this.body,
    required this.createdAt,
  });

  factory SupportMessage.fromJson(Map<String, dynamic> json) {
    final author = json['author'];
    final role = author is Map<String, dynamic> ? author['role'] as String? : null;
    return SupportMessage(
      id: _text(json['id']),
      authorName: author is Map<String, dynamic> ? _text(author['name'], 'Maplenou') : 'Maplenou',
      fromStaff: role == 'SUPPORT_AGENT' || role == 'SUPPORT_MANAGER' || role == 'ADMINISTRATOR',
      body: _text(json['body']),
      createdAt: DateTime.parse(_text(json['createdAt'])),
    );
  }
}

class SupportNotificationModel {
  final String id;
  final String? ticketId;
  final String title;
  final String body;
  final DateTime createdAt;
  final bool read;

  const SupportNotificationModel({
    required this.id,
    required this.ticketId,
    required this.title,
    required this.body,
    required this.createdAt,
    required this.read,
  });

  factory SupportNotificationModel.fromJson(Map<String, dynamic> json) => SupportNotificationModel(
        id: _text(json['id']),
        ticketId: json['ticketId'] as String?,
        title: _text(json['title']),
        body: _text(json['body']),
        createdAt: DateTime.parse(_text(json['createdAt'])),
        read: json['readAt'] != null,
      );
}
