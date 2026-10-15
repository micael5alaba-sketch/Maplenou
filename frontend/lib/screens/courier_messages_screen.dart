import 'package:flutter/material.dart';

import '../models/conversation_model.dart';
import '../services/courier_messaging_service.dart';
import '../theme/app_color_scheme.dart';
import '../widgets/app_drawer.dart';
import '../widgets/app_logo.dart';
import '../widgets/conversation_list_tile.dart';
import '../widgets/courier_bottom_navigation.dart';
import 'courier_conversation_screen.dart';
import 'courier_dashboard_screen.dart';
import 'courier_deliveries_screen.dart';
import 'courier_profile_screen.dart';
import 'notifications_screen.dart';

/// Courier's conversation list, split into "Clients" (BUYER_COURIER),
/// "Vendeurs" (SELLER_COURIER) and "Support" — backed by
/// [CourierMessagingService], toutes réelles.
class CourierMessagesScreen extends StatefulWidget {
  const CourierMessagesScreen({super.key});

  @override
  State<CourierMessagesScreen> createState() => _CourierMessagesScreenState();
}

class _CourierMessagesScreenState extends State<CourierMessagesScreen> {
  final _scaffoldKey = GlobalKey<ScaffoldState>();
  final _service = CourierMessagingService();
  ConversationKind _selectedKind = ConversationKind.courierClient;
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

  void _showComingSoon(String feature) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text('$feature bientôt disponible.')));
  }

  void _openConversation(ConversationPreviewModel conversation) {
    Navigator.of(context)
        .push(
          MaterialPageRoute(
            builder: (_) => CourierConversationScreen(
              conversationId: conversation.id,
              participantName: conversation.participantName,
              participantAvatarUrl: conversation.participantAvatarUrl,
            ),
          ),
        )
        .then((_) => setState(() => _conversationsFuture = _service.getConversations(kind: _selectedKind)));
  }

  // `pushReplacement` dans tous les cas, voir courier_dashboard_screen.dart.
  void _onTabSelected(CourierTab tab) {
    if (tab == CourierTab.messages) return;
    switch (tab) {
      case CourierTab.dashboard:
        Navigator.of(context).pushReplacement(MaterialPageRoute(builder: (_) => const CourierDashboardScreen()));
        break;
      case CourierTab.deliveries:
        Navigator.of(context).pushReplacement(MaterialPageRoute(builder: (_) => const CourierDeliveriesScreen()));
        break;
      case CourierTab.messages:
        break;
      case CourierTab.profile:
        Navigator.of(context).pushReplacement(MaterialPageRoute(builder: (_) => const CourierProfileScreen()));
        break;
    }
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

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
              child: FutureBuilder<List<ConversationPreviewModel>>(
                future: _conversationsFuture,
                builder: (context, snapshot) {
                  if (snapshot.connectionState == ConnectionState.waiting) {
                    return const Center(child: CircularProgressIndicator());
                  }
                  final conversations = snapshot.data ?? const [];
                  if (conversations.isEmpty) {
                    return Center(child: Text('Aucune conversation.', style: TextStyle(color: colors.textMuted)));
                  }
                  return ListView.separated(
                    padding: const EdgeInsets.fromLTRB(16, 0, 16, 24),
                    itemCount: conversations.length,
                    separatorBuilder: (_, _) => const SizedBox(height: 10),
                    itemBuilder: (context, index) {
                      final conversation = conversations[index];
                      return ConversationListTile(conversation: conversation, onTap: () => _openConversation(conversation));
                    },
                  );
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
            onPressed: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const NotificationsScreen())),
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
            _buildTab(colors, 'Clients', ConversationKind.courierClient),
            _buildTab(colors, 'Vendeurs', ConversationKind.courierShop),
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
