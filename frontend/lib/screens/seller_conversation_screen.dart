import 'package:flutter/material.dart';

import '../models/chat_message_model.dart';
import '../services/api_client.dart';
import '../services/seller_messaging_service.dart';
import '../theme/app_color_scheme.dart';
import '../widgets/chat_bubble.dart';
import '../widgets/message_input_bar.dart';

/// One conversation thread — reached from [SellerMessagesScreen].
///
/// Backed by [SellerMessagingService] (`GET/POST /api/conversations/{id}/messages`).
class SellerConversationScreen extends StatefulWidget {
  final String conversationId;
  final String participantName;
  final String? participantAvatarUrl;

  const SellerConversationScreen({
    super.key,
    required this.conversationId,
    required this.participantName,
    this.participantAvatarUrl,
  });

  @override
  State<SellerConversationScreen> createState() => _SellerConversationScreenState();
}

class _SellerConversationScreenState extends State<SellerConversationScreen> {
  final _service = SellerMessagingService();
  final _scrollController = ScrollController();
  late Future<List<ChatMessageModel>> _messagesFuture;

  static const _quickReplies = [
    "J'arrive dans 5 min",
    'Merci pour votre commande !',
    'Le colis est prêt',
  ];

  @override
  void initState() {
    super.initState();
    _messagesFuture = _service.getMessages(widget.conversationId);
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  Future<void> _handleSend(String content) async {
    try {
      await _service.sendMessage(widget.conversationId, content);
      setState(() => _messagesFuture = _service.getMessages(widget.conversationId));
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (_scrollController.hasClients) {
          _scrollController.animateTo(
            _scrollController.position.maxScrollExtent,
            duration: const Duration(milliseconds: 250),
            curve: Curves.easeOut,
          );
        }
      });
    } on ApiException catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(SnackBar(content: Text(e.message)));
    }
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return Scaffold(
      backgroundColor: colors.homeBackground,
      body: SafeArea(
        child: Column(
          children: [
            _buildHeader(colors),
            Expanded(
              child: FutureBuilder<List<ChatMessageModel>>(
                future: _messagesFuture,
                builder: (context, snapshot) {
                  if (snapshot.connectionState == ConnectionState.waiting) {
                    return const Center(child: CircularProgressIndicator());
                  }
                  if (snapshot.hasError) {
                    return Center(
                      child: Text(
                        snapshot.error is ApiException
                            ? (snapshot.error as ApiException).message
                            : 'Impossible de charger les messages.',
                        style: TextStyle(color: colors.textMuted),
                      ),
                    );
                  }
                  final messages = snapshot.data ?? const [];
                  return ListView(
                    controller: _scrollController,
                    padding: const EdgeInsets.all(16),
                    children: [for (final message in messages) ChatBubble(message: message)],
                  );
                },
              ),
            ),
            _buildQuickReplies(colors),
            MessageInputBar(onSend: _handleSend),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader(AppColorScheme colors) {
    return Container(
      color: colors.surface,
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
      child: Row(
        children: [
          IconButton(
            icon: Icon(Icons.arrow_back_rounded, color: colors.textDark),
            onPressed: () => Navigator.of(context).pop(),
          ),
          CircleAvatar(
            radius: 18,
            backgroundColor: colors.inputFill,
            backgroundImage: widget.participantAvatarUrl != null ? NetworkImage(widget.participantAvatarUrl!) : null,
            child: widget.participantAvatarUrl == null
                ? Text(
                    widget.participantName.isNotEmpty ? widget.participantName[0].toUpperCase() : '?',
                    style: TextStyle(fontWeight: FontWeight.bold, color: colors.primary, fontSize: 13),
                  )
                : null,
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              widget.participantName,
              style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildQuickReplies(AppColorScheme colors) {
    return SizedBox(
      height: 42,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 16),
        itemCount: _quickReplies.length,
        separatorBuilder: (_, _) => const SizedBox(width: 8),
        itemBuilder: (context, index) {
          final reply = _quickReplies[index];
          return OutlinedButton(
            onPressed: () => _handleSend(reply),
            style: OutlinedButton.styleFrom(
              foregroundColor: colors.textDark,
              side: BorderSide(color: colors.border),
              padding: const EdgeInsets.symmetric(horizontal: 14),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
            ),
            child: Text(reply, style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600)),
          );
        },
      ),
    );
  }
}
