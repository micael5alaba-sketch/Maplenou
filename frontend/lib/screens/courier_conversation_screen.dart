import 'package:flutter/material.dart';

import '../models/chat_message_model.dart';
import '../services/courier_messaging_service.dart';
import '../theme/app_color_scheme.dart';
import '../widgets/chat_bubble.dart';
import '../widgets/message_input_bar.dart';

/// One conversation thread — reached from [CourierMessagesScreen].
/// Mirrors [SellerConversationScreen] but backed by
/// [CourierMessagingService] (kept as a separate mocked service per role,
/// same convention as [VendorOrdersService] vs a future buyer equivalent).
class CourierConversationScreen extends StatefulWidget {
  final String conversationId;
  final String participantName;
  final String? participantAvatarUrl;

  const CourierConversationScreen({
    super.key,
    required this.conversationId,
    required this.participantName,
    this.participantAvatarUrl,
  });

  @override
  State<CourierConversationScreen> createState() => _CourierConversationScreenState();
}

class _CourierConversationScreenState extends State<CourierConversationScreen> {
  final _service = CourierMessagingService();
  final _scrollController = ScrollController();
  late List<ChatMessageModel> _messages;

  @override
  void initState() {
    super.initState();
    _messages = _service.getMessages(widget.conversationId);
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  void _handleSend(String content) {
    _service.sendMessage(widget.conversationId, content);
    setState(() => _messages = _service.getMessages(widget.conversationId));
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scrollController.hasClients) {
        _scrollController.animateTo(
          _scrollController.position.maxScrollExtent,
          duration: const Duration(milliseconds: 250),
          curve: Curves.easeOut,
        );
      }
    });
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
              child: ListView(
                controller: _scrollController,
                padding: const EdgeInsets.all(16),
                children: [for (final message in _messages) ChatBubble(message: message)],
              ),
            ),
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
          Expanded(child: Text(widget.participantName, style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark))),
        ],
      ),
    );
  }
}
