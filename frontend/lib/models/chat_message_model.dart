/// One message bubble in [SellerConversationScreen]. Maps directly onto
/// the backend's `Message` entity (`content`, `createdAt`, `readAt`);
/// [isMine] is derived client-side by comparing `sender.id` to the
/// logged-in user once auth is wired up.
class ChatMessageModel {
  final String id;
  final String content;
  final DateTime sentAt;
  final bool isMine;

  const ChatMessageModel({
    required this.id,
    required this.content,
    required this.sentAt,
    required this.isMine,
  });
}
