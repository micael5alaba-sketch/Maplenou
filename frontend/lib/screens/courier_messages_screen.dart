import 'package:flutter/material.dart';

import '../models/conversation_model.dart';
import '../services/courier_messaging_service.dart';
import '../theme/app_color_scheme.dart';
import '../widgets/app_drawer.dart';
import '../widgets/app_logo.dart';
import '../widgets/conversation_list_tile.dart';
import '../widgets/courier_bottom_navigation.dart';
import 'courier_conversation_screen.dart';
import 'courier_deliveries_screen.dart';
import 'courier_profile_screen.dart';

/// Courier's conversation list, split into "Clients", "Vendeurs" and
/// "Support" — three tabs, unlike the seller's two (see [ConversationKind]
/// for why "Vendeurs" has no backend type yet).
///
/// Backed by [CourierMessagingService] mocked data for now — no network
/// call yet.
class CourierMessagesScreen extends StatefulWidget {
  const CourierMessagesScreen({super.key});

  @override
  State<CourierMessagesScreen> createState() => _CourierMessagesScreenState();
}

class _CourierMessagesScreenState extends State<CourierMessagesScreen> {
  final _scaffoldKey = GlobalKey<ScaffoldState>();
  final _service = CourierMessagingService();
  ConversationKind _selectedKind = ConversationKind.client;

  void _showComingSoon(String feature) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text('$feature bientôt disponible.')));
  }

  void _openConversation(ConversationPreviewModel conversation) {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => CourierConversationScreen(
          conversationId: conversation.id,
          participantName: conversation.participantName,
          participantAvatarUrl: conversation.participantAvatarUrl,
        ),
      ),
    );
  }

  void _onTabSelected(CourierTab tab) {
    if (tab == CourierTab.messages) return;
    switch (tab) {
      case CourierTab.dashboard:
        Navigator.of(context).pop();
        break;
      case CourierTab.deliveries:
        Navigator.of(context).push(MaterialPageRoute(builder: (_) => const CourierDeliveriesScreen()));
        break;
      case CourierTab.messages:
        break;
      case CourierTab.profile:
        Navigator.of(context).push(MaterialPageRoute(builder: (_) => const CourierProfileScreen()));
        break;
    }
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    final conversations = _service.getConversations(kind: _selectedKind);

    return Scaffold(
      key: _scaffoldKey,
      backgroundColor: colors.homeBackground,
      drawer: AppDrawer(onComingSoon: _showComingSoon),
      body: SafeArea(
        child: Column(
          children: [
            _buildHeader(colors),
            const SizedBox(height: 12),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: Text('Messagerie', style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: colors.textDark)),
            ),
            const SizedBox(height: 12),
            _buildTabs(colors),
            const SizedBox(height: 8),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: Align(
                alignment: Alignment.centerLeft,
                child: Text('Réponses rapides', style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: colors.textDark)),
              ),
            ),
            const SizedBox(height: 8),
            _buildQuickReplies(colors),
            const SizedBox(height: 12),
            Expanded(
              child: conversations.isEmpty
                  ? Center(child: Text('Aucune conversation.', style: TextStyle(color: colors.textMuted)))
                  : ListView.separated(
                      padding: const EdgeInsets.fromLTRB(16, 0, 16, 24),
                      itemCount: conversations.length,
                      separatorBuilder: (_, _) => const SizedBox(height: 10),
                      itemBuilder: (context, index) {
                        final conversation = conversations[index];
                        return ConversationListTile(conversation: conversation, onTap: () => _openConversation(conversation));
                      },
                    ),
            ),
          ],
        ),
      ),
      bottomNavigationBar: CourierBottomNavigation(currentTab: CourierTab.messages, onTabSelected: _onTabSelected),
    );
  }

  Widget _buildHeader(AppColorScheme colors) {
    return Container(
      color: colors.surface,
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      child: Row(
        children: [
          IconButton(
            icon: Icon(Icons.menu_rounded, color: colors.textDark),
            onPressed: () => _scaffoldKey.currentState?.openDrawer(),
          ),
          const Expanded(child: Center(child: AppLogo(variant: AppLogoVariant.horizontalColor, size: 34))),
          IconButton(
            icon: Icon(Icons.notifications_none_rounded, color: colors.textDark),
            onPressed: () => _showComingSoon('Notifications'),
          ),
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
            _buildTab(colors, 'Vendeurs', ConversationKind.vendor),
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
        onTap: () => setState(() => _selectedKind = kind),
        child: Container(
          padding: const EdgeInsets.symmetric(vertical: 10),
          decoration: BoxDecoration(color: isActive ? colors.surface : Colors.transparent, borderRadius: BorderRadius.circular(10)),
          alignment: Alignment.center,
          child: Text(
            label,
            style: TextStyle(color: isActive ? colors.textDark : colors.textMuted, fontWeight: isActive ? FontWeight.w700 : FontWeight.w500, fontSize: 13),
          ),
        ),
      ),
    );
  }

  Widget _buildQuickReplies(AppColorScheme colors) {
    const replies = ["J'arrive dans 5 min", 'Je suis devant'];

    return SizedBox(
      height: 40,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 16),
        itemCount: replies.length,
        separatorBuilder: (_, _) => const SizedBox(width: 8),
        itemBuilder: (context, index) {
          final isFirst = index == 0;
          return Container(
            padding: const EdgeInsets.symmetric(horizontal: 14),
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: isFirst ? colors.primary : colors.surface,
              borderRadius: BorderRadius.circular(20),
              border: isFirst ? null : Border.all(color: colors.border),
            ),
            child: Text(
              replies[index],
              style: TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: isFirst ? Colors.white : colors.textDark),
            ),
          );
        },
      ),
    );
  }
}
