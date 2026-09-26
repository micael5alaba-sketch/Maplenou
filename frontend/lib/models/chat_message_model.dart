/// One message bubble in [SellerConversationScreen]. Miroir de
/// `MessageResponse` (`messaging/dto/MessageResponse.java`) ; [isMine] est
/// dérivé côté client en comparant `senderId` au compte connecté.
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

  factory ChatMessageModel.fromJson(Map<String, dynamic> json, {required String currentUserId}) {
    return ChatMessageModel(
      id: json['id'] as String,
      content: json['content'] as String,
      sentAt: DateTime.parse(json['createdAt'] as String),
      isMine: json['senderId'] == currentUserId,
    );
  }
}
