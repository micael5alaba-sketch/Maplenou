import '../models/chat_message_model.dart';
import '../models/conversation_model.dart';
import 'conversation_service.dart';

/// Messagerie livreur — "Clients" (BUYER_COURIER), "Vendeurs" (SELLER_COURIER)
/// et "Support" (SUPPORT), toutes réelles (`messaging/ConversationController.java`).
class CourierMessagingService {
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

  /// Contacter le client d'une livraison en cours (onglet "Clients").
  Future<String> startClientConversation(String deliveryId) {
    return _conversationService.startCourierConversation(deliveryId);
  }

  /// Contacter la boutique d'une sous-commande en livraison (onglet "Vendeurs").
  Future<(String id, String shopName)> startShopConversation(String subOrderId) async {
    final (id, shopName, _) = await _conversationService.startShopCourierConversation(subOrderId);
    return (id, shopName);
  }
}
