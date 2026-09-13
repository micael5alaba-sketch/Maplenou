/// Matches the backend's `ConversationType` (see `MODELE_DONNEES.md`):
/// a seller only ever sees conversations with buyers or with support —
/// never with other sellers.
enum ConversationKind { client, support }

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
