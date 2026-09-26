/// Miroir du `ConversationType` backend (`messaging/ConversationType.java`) :
/// `client` = BUYER_SELLER, `courierClient` = BUYER_COURIER,
/// `courierShop` = SELLER_COURIER, `support` = SUPPORT.
enum ConversationKind { client, courierClient, courierShop, support }

/// One row in a conversation list (Vendeur/Livreur "Messagerie"). Miroir de
/// `ConversationResponse` (`messaging/dto/ConversationResponse.java`) — pas
/// d'aperçu du dernier message ni de compteur de non-lus exposés par cet
/// endpoint pour l'instant (juste l'horodatage).
class ConversationPreviewModel {
  final String id;
  final String participantName;
  final String? participantAvatarUrl;
  final String lastMessage;
  final DateTime lastMessageAt;
  final int unreadCount;
  final ConversationKind kind;

  const ConversationPreviewModel({
    required this.id,
    required this.participantName,
    this.participantAvatarUrl,
    required this.lastMessage,
    required this.lastMessageAt,
    this.unreadCount = 0,
    required this.kind,
  });

  /// [viewerId] est l'id du compte connecté — sert à déterminer, pour les
  /// types à deux participants nommés, lequel des deux est "l'autre".
  factory ConversationPreviewModel.fromJson(Map<String, dynamic> json, {required String viewerId}) {
    final type = json['type'] as String;

    final kind = switch (type) {
      'SUPPORT' => ConversationKind.support,
      'BUYER_COURIER' => ConversationKind.courierClient,
      'SELLER_COURIER' => ConversationKind.courierShop,
      _ => ConversationKind.client, // BUYER_SELLER
    };

    final participantName = switch (type) {
      'SUPPORT' => 'Support Maplenou',
      'BUYER_SELLER' => viewerId == json['buyerId']
          ? (json['shopName'] as String? ?? 'Boutique')
          : (json['buyerName'] as String? ?? 'Client'),
      'BUYER_COURIER' => viewerId == json['buyerId']
          ? (json['courierName'] as String? ?? 'Livreur')
          : (json['buyerName'] as String? ?? 'Client'),
      'SELLER_COURIER' => viewerId == json['sellerId']
          ? (json['courierName'] as String? ?? 'Livreur')
          : (json['shopName'] as String? ?? 'Boutique'),
      _ => 'Conversation',
    };

    return ConversationPreviewModel(
      id: json['id'] as String,
      participantName: participantName,
      lastMessage: '',
      lastMessageAt: DateTime.parse((json['lastMessageAt'] ?? json['createdAt']) as String),
      kind: kind,
    );
  }
}
