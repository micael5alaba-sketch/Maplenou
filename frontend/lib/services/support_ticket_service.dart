import 'conversation_service.dart';

/// Envoie un "ticket" support via la messagerie déjà existante
/// (`POST /api/conversations/support` + premier message) — le backend n'a
/// pas d'entité "ticket" dédiée (catégorie, commande liée, numéro de
/// suivi), seulement une `Conversation` de type `SUPPORT` générique. La
/// référence retournée est donc générée côté app, pas un vrai numéro de
/// ticket backend.
class SupportTicketService {
  final _conversationService = ConversationService();

  Future<String> submitTicket({
    required String category,
    String? relatedOrder,
    required String subject,
    required String description,
  }) async {
    final conversationId = await _conversationService.startSupportConversation();

    final messageParts = [
      'Catégorie : $category',
      if (relatedOrder != null && relatedOrder.isNotEmpty) 'Commande concernée : $relatedOrder',
      'Sujet : $subject',
      '',
      description,
    ];
    await _conversationService.sendMessage(conversationId, messageParts.join('\n'));

    final year = DateTime.now().year;
    final sequence = (DateTime.now().millisecondsSinceEpoch % 9000) + 1000;
    return 'TKT-$year-$sequence';
  }
}
