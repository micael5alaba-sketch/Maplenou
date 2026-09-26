import 'package:flutter/material.dart';

import '../models/conversation_model.dart';
import '../services/api_client.dart';
import '../services/seller_messaging_service.dart';
import '../theme/app_color_scheme.dart';
import '../widgets/conversation_list_tile.dart';
import 'seller_conversation_screen.dart';

/// Seller's conversation list, split into "Clients" (buyer↔seller threads)
/// and "Support" tabs — mirrors the backend's `ConversationType`
/// (`BUYER_SELLER` / `SUPPORT`). A seller never has `Vendeurs`-type
/// threads, unlike the Livreur messaging maquette.
///
/// Reached from [SellerProfileScreen]. Backed by [SellerMessagingService]
/// (`GET /api/conversations`).
class SellerMessagesScreen extends StatefulWidget {
  const SellerMessagesScreen({super.key});

  @override
  State<SellerMessagesScreen> createState() => _SellerMessagesScreenState();
}

class _SellerMessagesScreenState extends State<SellerMessagesScreen> {
  final _service = SellerMessagingService();
  ConversationKind _selectedKind = ConversationKind.client;
  late Future<List<ConversationPreviewModel>> _conversationsFuture;

  @override
  void initState() {
    super.initState();
    _conversationsFuture = _service.getConversations(kind: _selectedKind);
  }

  void _selectKind(ConversationKind kind) {
    setState(() {
      _selectedKind = kind;
      _conversationsFuture = _service.getConversations(kind: kind);
    });
  }

  void _openConversation(ConversationPreviewModel conversation) {
    Navigator.of(context)
        .push(
          MaterialPageRoute(
            builder: (_) => SellerConversationScreen(
              conversationId: conversation.id,
              participantName: conversation.participantName,
              participantAvatarUrl: conversation.participantAvatarUrl,
            ),
          ),
        )
        .then((_) => setState(() => _conversationsFuture = _service.getConversations(kind: _selectedKind)));
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
            const SizedBox(height: 8),
            _buildTabs(colors),
            const SizedBox(height: 12),
            Expanded(
              child: FutureBuilder<List<ConversationPreviewModel>>(
                future: _conversationsFuture,
                builder: (context, snapshot) {
                  if (snapshot.connectionState == ConnectionState.waiting) {
                    return const Center(child: CircularProgressIndicator());
                  }
                  if (snapshot.hasError) {
                    return _buildErrorState(colors, snapshot.error);
                  }
                  final conversations = snapshot.data ?? const [];
                  if (conversations.isEmpty) return _buildEmptyState(colors);
                  return ListView.separated(
                    padding: const EdgeInsets.fromLTRB(16, 0, 16, 24),
                    itemCount: conversations.length,
                    separatorBuilder: (_, _) => const SizedBox(height: 10),
                    itemBuilder: (context, index) {
                      final conversation = conversations[index];
                      return ConversationListTile(
                        conversation: conversation,
                        onTap: () => _openConversation(conversation),
                      );
                    },
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildErrorState(AppColorScheme colors, Object? error) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.error_outline_rounded, size: 48, color: colors.error),
            const SizedBox(height: 12),
            Text(
              error is ApiException ? error.message : 'Impossible de charger les conversations.',
              textAlign: TextAlign.center,
              style: TextStyle(color: colors.textMuted),
            ),
            const SizedBox(height: 12),
            TextButton(
              onPressed: () => setState(() => _conversationsFuture = _service.getConversations(kind: _selectedKind)),
              child: const Text('Réessayer'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader(AppColorScheme colors) {
    return Container(
      color: colors.surface,
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      child: Row(
        children: [
          IconButton(
            icon: Icon(Icons.arrow_back_rounded, color: colors.textDark),
            onPressed: () => Navigator.of(context).pop(),
          ),
          Expanded(
            child: Text(
              'Messagerie',
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 17, fontWeight: FontWeight.bold, color: colors.textDark),
            ),
          ),
          const SizedBox(width: 48),
        ],
      ),
    );
  }

  Widget _buildTabs(AppColorScheme colors) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: Container(
        padding: const EdgeInsets.all(4),
        decoration: BoxDecoration(color: colors.inputFill, borderRadius: BorderRadius.circular(14)),
        child: Row(
          children: [
            _buildTab(colors, 'Clients', ConversationKind.client),
            _buildTab(colors, 'Support', ConversationKind.support),
          ],
        ),
      ),
    );
  }

  Widget _buildTab(AppColorScheme colors, String label, ConversationKind kind) {
    final isActive = _selectedKind == kind;

    return Expanded(
      child: GestureDetector(
        onTap: () => _selectKind(kind),
        child: Container(
          padding: const EdgeInsets.symmetric(vertical: 10),
          decoration: BoxDecoration(
            color: isActive ? colors.surface : Colors.transparent,
            borderRadius: BorderRadius.circular(10),
          ),
          alignment: Alignment.center,
          child: Text(
            label,
            style: TextStyle(
              color: isActive ? colors.textDark : colors.textMuted,
              fontWeight: isActive ? FontWeight.w700 : FontWeight.w500,
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildEmptyState(AppColorScheme colors) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.chat_bubble_outline_rounded, size: 48, color: colors.textMuted),
            const SizedBox(height: 12),
            Text('Aucune conversation pour le moment.', style: TextStyle(color: colors.textMuted, fontWeight: FontWeight.w500)),
          ],
        ),
      ),
    );
  }
}
