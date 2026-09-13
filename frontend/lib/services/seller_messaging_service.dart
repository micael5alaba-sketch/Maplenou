import '../models/chat_message_model.dart';
import '../models/conversation_model.dart';

/// Provides the seller's conversations and messages shown on
/// [SellerMessagesScreen] / [SellerConversationScreen].
///
/// Mocked for now — no network call. Replace with real calls once auth is
/// wired up: conversations map onto the backend's `Conversation` list
/// (`ConversationType.BUYER_SELLER` for the "Clients" tab,
/// `ConversationType.SUPPORT` for "Support"), messages onto
/// `GET /api/conversations/{id}/messages` and sending onto the matching
/// POST (see `MODELE_DONNEES.md`, module `messaging`).
class SellerMessagingService {
  static final List<ConversationPreviewModel> _conversations = [
    ConversationPreviewModel(
      id: 'c1',
      participantName: 'Amina Diallo',
      lastMessage: 'Parfait, je descends vous ouvrir le portail principal.',
      lastMessageAt: DateTime.now().subtract(const Duration(minutes: 12)),
      unreadCount: 2,
      kind: ConversationKind.client,
    ),
    ConversationPreviewModel(
      id: 'c2',
      participantName: 'Kwame Mensah',
      lastMessage: 'Merci pour la livraison rapide !',
      lastMessageAt: DateTime.now().subtract(const Duration(hours: 20)),
      kind: ConversationKind.client,
    ),
    ConversationPreviewModel(
      id: 'c3',
      participantName: 'Fatou Traoré',
      lastMessage: 'Bonjour, le collier est-il encore disponible en argent ?',
      lastMessageAt: DateTime.now().subtract(const Duration(days: 2)),
      unreadCount: 1,
      kind: ConversationKind.client,
    ),
    ConversationPreviewModel(
      id: 's1',
      participantName: 'Support Maplenou',
      lastMessage: 'Votre demande de retrait a bien été prise en compte.',
      lastMessageAt: DateTime.now().subtract(const Duration(days: 1)),
      kind: ConversationKind.support,
    ),
  ];

  static final Map<String, List<ChatMessageModel>> _messages = {
    'c1': [
      ChatMessageModel(
        id: 'm1',
        content: 'Bonjour ! Votre commande #CMD-8492 est prête, le livreur passe la récupérer sous peu.',
        sentAt: DateTime.now().subtract(const Duration(minutes: 40)),
        isMine: true,
      ),
      ChatMessageModel(
        id: 'm2',
        content: "D'accord, merci pour l'info !",
        sentAt: DateTime.now().subtract(const Duration(minutes: 30)),
        isMine: false,
      ),
      ChatMessageModel(
        id: 'm3',
        content: 'Parfait, je descends vous ouvrir le portail principal.',
        sentAt: DateTime.now().subtract(const Duration(minutes: 12)),
        isMine: false,
      ),
    ],
    'c2': [
      ChatMessageModel(
        id: 'm4',
        content: 'Merci pour la livraison rapide !',
        sentAt: DateTime.now().subtract(const Duration(hours: 20)),
        isMine: false,
      ),
    ],
    'c3': [
      ChatMessageModel(
        id: 'm5',
        content: 'Bonjour, le collier est-il encore disponible en argent ?',
        sentAt: DateTime.now().subtract(const Duration(days: 2)),
        isMine: false,
      ),
    ],
    's1': [
      ChatMessageModel(
        id: 'm6',
        content: 'Votre demande de retrait a bien été prise en compte.',
        sentAt: DateTime.now().subtract(const Duration(days: 1)),
        isMine: false,
      ),
    ],
  };

  List<ConversationPreviewModel> getConversations({required ConversationKind kind}) {
    return _conversations.where((c) => c.kind == kind).toList();
  }

  List<ChatMessageModel> getMessages(String conversationId) {
    return List.unmodifiable(_messages[conversationId] ?? const []);
  }

  /// Mocked send: appends locally so the UI reflects it immediately.
  void sendMessage(String conversationId, String content) {
    final thread = _messages.putIfAbsent(conversationId, () => []);
    thread.add(ChatMessageModel(
      id: 'local-${thread.length}',
      content: content,
      sentAt: DateTime.now(),
      isMine: true,
    ));
  }
}
