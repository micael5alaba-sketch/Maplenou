import 'package:flutter/material.dart';

import '../models/app_notification_model.dart';
import '../services/notification_history_service.dart';
import '../theme/app_color_scheme.dart';
import '../utils/formatters.dart';

/// Historique réel des notifications push reçues sur cet appareil (voir
/// `NotificationHistoryService`) — remplace l'ancien bouton "bientôt
/// disponible" présent sur tous les tableaux de bord (acheteur, vendeur,
/// livreur).
class NotificationsScreen extends StatefulWidget {
  const NotificationsScreen({super.key});

  @override
  State<NotificationsScreen> createState() => _NotificationsScreenState();
}

class _NotificationsScreenState extends State<NotificationsScreen> {
  late Future<List<AppNotification>> _future;

  @override
  void initState() {
    super.initState();
    _future = _loadAndMarkRead();
  }

  Future<List<AppNotification>> _loadAndMarkRead() async {
    final list = await NotificationHistoryService().getAll();
    await NotificationHistoryService().markAllRead();
    return list;
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return Scaffold(
      backgroundColor: colors.homeBackground,
      body: SafeArea(
        child: Column(
          children: [
            _buildHeader(context, colors),
            Expanded(
              child: FutureBuilder<List<AppNotification>>(
                future: _future,
                builder: (context, snapshot) {
                  if (snapshot.connectionState == ConnectionState.waiting) {
                    return const Center(child: CircularProgressIndicator());
                  }
                  final items = snapshot.data ?? const [];
                  if (items.isEmpty) return _buildEmptyState(colors);
                  return ListView.separated(
                    padding: const EdgeInsets.all(16),
                    itemCount: items.length,
                    separatorBuilder: (_, _) => const SizedBox(height: 10),
                    itemBuilder: (context, i) => _buildTile(colors, items[i]),
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader(BuildContext context, AppColorScheme colors) {
    return Container(
      color: colors.surface,
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
      child: Row(
        children: [
          IconButton(
            icon: Icon(Icons.arrow_back_rounded, color: colors.textDark),
            onPressed: () => Navigator.of(context).pop(),
          ),
          Expanded(
            child: Text('Notifications', textAlign: TextAlign.center, style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
          ),
          const SizedBox(width: 48),
        ],
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
            Icon(Icons.notifications_none_rounded, size: 56, color: colors.textMuted),
            const SizedBox(height: 12),
            Text('Aucune notification pour le moment', style: TextStyle(color: colors.textMuted), textAlign: TextAlign.center),
            const SizedBox(height: 6),
            Text(
              'Les notifications reçues sur cet appareil s\'afficheront ici.',
              style: TextStyle(color: colors.textMuted, fontSize: 12),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTile(AppColorScheme colors, AppNotification notification) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(14),
        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 8, offset: const Offset(0, 2))],
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.notifications_rounded, color: colors.primary),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(notification.title, style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark)),
                if (notification.body.isNotEmpty) ...[
                  const SizedBox(height: 2),
                  Text(notification.body, style: TextStyle(color: colors.textMuted, fontSize: 13)),
                ],
                const SizedBox(height: 6),
                Text(formatRelativeDate(notification.receivedAt), style: TextStyle(fontSize: 11, color: colors.textMuted)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
