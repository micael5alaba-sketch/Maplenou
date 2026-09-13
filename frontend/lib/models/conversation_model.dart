/// `client`/`support` match the backend's `ConversationType`
/// (`BUYER_SELLER`/`SUPPORT`, see `MODELE_DONNEES.md`). `vendor` is
/// Livreur-only (a courier messaging a shop about a pickup) and has no
/// backend equivalent yet — flagged as a gap alongside the courier's
/// missing earnings/documents/tickets models.
enum ConversationKind { client, vendor, support }

/// One row in [SellerMessagesScreen]'s conversation list.
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
}
