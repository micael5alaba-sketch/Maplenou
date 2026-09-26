import '../models/chat_message_model.dart';
import '../models/conversation_model.dart';
import 'api_client.dart';
import 'user_service.dart';

/// `messaging/ConversationController.java` — messagerie
/// acheteur↔boutique/support, partagée entre les vues Acheteur, Vendeur et
/// Livreur (chacun ne voit que ses propres conversations).
class ConversationService {
  final _client = ApiClient();
  final _userService = UserService();

  Future<List<ConversationPreviewModel>> getConversations({required ConversationKind kind}) async {
    final currentUser = await _userService.getCurrentUser();
    final data = await _client.get('/api/conversations', query: {'size': '50'}) as Map<String, dynamic>;
    final content = data['content'] as List<dynamic>? ?? [];
    final all = content
        .map((json) => ConversationPreviewModel.fromJson(json as Map<String, dynamic>, viewerId: currentUser.id))
        .toList();
    return all.where((c) => c.kind == kind).toList();
  }

  Future<List<ChatMessageModel>> getMessages(String conversationId) async {
    final currentUser = await _userService.getCurrentUser();
    final data =
        await _client.get('/api/conversations/$conversationId/messages', query: {'size': '100'}) as Map<String, dynamic>;
    final content = data['content'] as List<dynamic>? ?? [];
    return content
        .map((json) => ChatMessageModel.fromJson(json as Map<String, dynamic>, currentUserId: currentUser.id))
        .toList();
  }

  Future<void> sendMessage(String conversationId, String content) {
    return _client.post('/api/conversations/$conversationId/messages', body: {'content': content});
  }

  Future<void> markAsRead(String conversationId) => _client.patch('/api/conversations/$conversationId/read');

  Future<String> startConversationWithShop(String shopId) async {
    final data = await _client.post('/api/conversations/shops/$shopId') as Map<String, dynamic>;
    return data['id'] as String;
  }

  Future<String> startSupportConversation() async {
    final data = await _client.post('/api/conversations/support') as Map<String, dynamic>;
    return data['id'] as String;
  }

  /// Client ou livreur : conversation partagée pour une livraison en cours.
  Future<String> startCourierConversation(String deliveryId) async {
    final data = await _client.post('/api/conversations/deliveries/$deliveryId/courier') as Map<String, dynamic>;
    return data['id'] as String;
  }

  /// Boutique ou livreur : conversation partagée pour la livraison d'une sous-commande.
  /// Renvoie aussi les deux noms (boutique et livreur) : l'appelant ne connaît d'avance que
  /// son propre nom, pas celui de l'autre partie.
  Future<(String id, String shopName, String courierName)> startShopCourierConversation(String subOrderId) async {
    final data = await _client.post('/api/conversations/sub-orders/$subOrderId/courier') as Map<String, dynamic>;
    return (
      data['id'] as String,
      data['shopName'] as String? ?? 'Boutique',
      data['courierName'] as String? ?? 'Livreur',
    );
  }
}
