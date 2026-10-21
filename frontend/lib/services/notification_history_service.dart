import '../models/app_notification_model.dart';
import 'api_client.dart';

/// Historique des notifications — `GET /api/notifications` côté backend
/// (`NotificationController.java`). Persisté serveur depuis le 21/10/2026 (voir
/// audit externe du 16/10/2026) : avant ça, `FcmService` envoyait en fire-and-forget
/// sans rien garder, donc rien après réinstallation de l'app ni sur un second appareil.
class NotificationHistoryService {
  Future<List<AppNotification>> getAll() async {
    final data = await ApiClient().get('/api/notifications', query: {'size': '50'}) as Map<String, dynamic>;
    final content = data['content'] as List<dynamic>? ?? [];
    return content.map((json) => AppNotification.fromJson(json as Map<String, dynamic>)).toList();
  }

  Future<void> markAllRead() => ApiClient().patch('/api/notifications/read-all');

  Future<int> unreadCount() async {
    final data = await ApiClient().get('/api/notifications/unread-count') as Map<String, dynamic>;
    return (data['count'] as num?)?.toInt() ?? 0;
  }
}
