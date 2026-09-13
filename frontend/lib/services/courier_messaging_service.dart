import '../models/chat_message_model.dart';
import '../models/conversation_model.dart';

/// Provides the courier's conversations and messages shown on
/// [CourierMessagesScreen] / on the shared [SellerConversationScreen] (the
/// thread view is generic enough to be reused as-is).
///
/// Mocked for now — no network call. Unlike the seller's two tabs, the
/// "Vendeurs" tab here has no backend `ConversationType` to map onto yet
/// (see [ConversationKind]).
class CourierMessagingService {
  static final List<ConversationPreviewModel> _conversations = [
    ConversationPreviewModel(
      id: 'lc1',
      participantName: 'Aisha Diallo',
      lastMessage: 'Entendu, à tout de suite.',
      lastMessageAt: DateTime.now().subtract(const Duration(minutes: 3)),
      kind: ConversationKind.client,
    ),
    ConversationPreviewModel(
      id: 'lc2',
      participantName: 'Amina D.',
      lastMessage: 'Parfait, je descends vous ouvrir le portail principal.',
      lastMessageAt: DateTime.now().subtract(const Duration(hours: 2)),
      unreadCount: 1,
      kind: ConversationKind.client,
    ),
    ConversationPreviewModel(
      id: 'lv1',
      participantName: 'Le Maquis Doré',
      lastMessage: 'La commande est prête, vous pouvez venir la récupérer.',
      lastMessageAt: DateTime.now().subtract(const Duration(minutes: 20)),
      kind: ConversationKind.vendor,
    ),
    ConversationPreviewModel(
      id: 'ls1',
      participantName: 'Support Maplenou',
      lastMessage: 'Votre document a bien été mis à jour.',
      lastMessageAt: DateTime.now().subtract(const Duration(days: 1)),
      kind: ConversationKind.support,
    ),
  ];

  static final Map<String, List<ChatMessageModel>> _messages = {
    'lc1': [
      ChatMessageModel(
        id: 'm1',
        content: 'Bonjour ! Je suis en route avec votre commande #MPL-2024. Je devrais arriver dans 10 minutes.',
        sentAt: DateTime.now().subtract(const Duration(minutes: 10)),
        isMine: true,
      ),
      ChatMessageModel(
        id: 'm2',
        content: 'Super, merci ! Je vous attends devant l\'immeuble.',
        sentAt: DateTime.now().subtract(const Duration(minutes: 8)),
        isMine: false,
      ),
      ChatMessageModel(
        id: 'm3',
        content: 'Entendu, à tout de suite.',
        sentAt: DateTime.now().subtract(const Duration(minutes: 3)),
        isMine: true,
      ),
    ],
    'lc2': [
      ChatMessageModel(
        id: 'm4',
        content: 'Parfait, je descends vous ouvrir le portail principal.',
        sentAt: DateTime.now().subtract(const Duration(hours: 2)),
        isMine: false,
      ),
    ],
    'lv1': [
      ChatMessageModel(
        id: 'm5',
        content: 'La commande est prête, vous pouvez venir la récupérer.',
        sentAt: DateTime.now().subtract(const Duration(minutes: 20)),
        isMine: false,
      ),
    ],
    'ls1': [
      ChatMessageModel(
        id: 'm6',
        content: 'Votre document a bien été mis à jour.',
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
