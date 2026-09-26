import '../models/chat_message_model.dart';
import '../models/conversation_model.dart';
import 'conversation_service.dart';

/// Messagerie vendeur, via [ConversationService] (`ConversationController.java`).
class SellerMessagingService {
  final _conversationService = ConversationService();

  Future<List<ConversationPreviewModel>> getConversations({required ConversationKind kind}) {
    return _conversationService.getConversations(kind: kind);
  }

  Future<List<ChatMessageModel>> getMessages(String conversationId) {
    return _conversationService.getMessages(conversationId);
  }

  Future<void> sendMessage(String conversationId, String content) {
    return _conversationService.sendMessage(conversationId, content);
  }

  /// Contacter le livreur assigné à la livraison d'une sous-commande (onglet "Livreur").
  Future<(String id, String courierName)> startCourierConversation(String subOrderId) async {
    final (id, _, courierName) = await _conversationService.startShopCourierConversation(subOrderId);
    return (id, courierName);
  }
}
