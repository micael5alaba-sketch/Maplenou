import 'dart:io' show Platform;

import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart' show kIsWeb;

import 'api_client.dart';
import 'notification_history_service.dart';

/// Handler appelé par le système quand une notification arrive alors que
/// l'app est en arrière-plan/fermée — doit être une fonction top-level
/// (isolate séparé), voir `main.dart`.
@pragma('vm:entry-point')
Future<void> firebaseMessagingBackgroundHandler(RemoteMessage message) async {
  await NotificationHistoryService().addFromMessage(message);
}

/// Enregistre le device auprès du backend (`POST /api/notifications/tokens`,
/// voir `DeviceTokenController`) pour recevoir les notifications push
/// envoyées par `FcmService` — ce qui manquait entièrement côté client :
/// le backend était prêt, mais aucun appareil ne s'enregistrait jamais, donc
/// `FcmService.sendToUser` ne trouvait jamais de token et ne faisait rien.
class PushNotificationService {
  bool _initialized = false;

  Future<void> initialize() async {
    if (_initialized) return;
    try {
      final messaging = FirebaseMessaging.instance;
      await messaging.requestPermission(alert: true, badge: true, sound: true);

      final token = await messaging.getToken();
      if (token != null) await _registerToken(token);
      messaging.onTokenRefresh.listen(_registerToken);

      FirebaseMessaging.onMessage.listen((message) {
        NotificationHistoryService().addFromMessage(message);
      });

      _initialized = true;
    } catch (_) {
      // Firebase indisponible ou permission refusée : le reste de l'app continue sans push.
    }
  }

  Future<void> _registerToken(String token) async {
    try {
      await ApiClient().post('/api/notifications/tokens', body: {
        'token': token,
        'platform': _platform(),
      });
    } catch (_) {
      // Pas bloquant — réessayé au prochain démarrage/rafraîchissement du token.
    }
  }

  String _platform() {
    if (kIsWeb) return 'WEB';
    return Platform.isIOS ? 'IOS' : 'ANDROID';
  }

  /// Appelé à la déconnexion — évite d'envoyer des push à un appareil dont
  /// personne n'est plus session ouverte.
  Future<void> unregisterAll() async {
    try {
      await ApiClient().delete('/api/notifications/tokens');
    } catch (_) {}
  }
}
