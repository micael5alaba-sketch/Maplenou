import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../models/app_notification_model.dart';

/// Historique local des notifications push reçues sur CET appareil.
/// `FcmService` côté backend envoie en fire-and-forget (pas de table
/// `notifications` en base) : il n'existe donc pas d'historique partagé
/// entre appareils, seulement ce que cet appareil a lui-même reçu.
class NotificationHistoryService {
  static const _key = 'notification_history';
  static const _maxEntries = 100;

  Future<List<AppNotification>> getAll() async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getStringList(_key) ?? const [];
    return raw.map(AppNotification.decode).toList();
  }

  Future<void> addFromMessage(RemoteMessage message) async {
    final notification = AppNotification(
      title: message.notification?.title ?? message.data['title'] as String? ?? 'Maplenou',
      body: message.notification?.body ?? message.data['body'] as String? ?? '',
      data: message.data,
      receivedAt: DateTime.now(),
    );
    await _add(notification);
  }

  Future<void> _add(AppNotification notification) async {
    final prefs = await SharedPreferences.getInstance();
    final list = await getAll();
    list.insert(0, notification);
    final trimmed = list.length > _maxEntries ? list.sublist(0, _maxEntries) : list;
    await prefs.setStringList(_key, trimmed.map((n) => n.encode()).toList());
  }

  Future<void> markAllRead() async {
    final prefs = await SharedPreferences.getInstance();
    final list = await getAll();
    for (final notification in list) {
      notification.read = true;
    }
    await prefs.setStringList(_key, list.map((n) => n.encode()).toList());
  }

  Future<int> unreadCount() async => (await getAll()).where((n) => !n.read).length;
}
