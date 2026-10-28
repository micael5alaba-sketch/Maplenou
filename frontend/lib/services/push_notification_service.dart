import 'dart:io' show Platform;

import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter/material.dart';

import '../navigation.dart';
import '../screens/seller_conversation_screen.dart';
import 'api_client.dart';

/// Handler appelé par le système quand une notification arrive alors que
/// l'app est en arrière-plan/fermée — doit être une fonction top-level
/// (isolate séparé), voir `main.dart`. Ne fait plus rien ici : la notification
/// est déjà persistée côté serveur (`NotificationService.send`, avant même
/// l'envoi FCM) — `NotificationsScreen` la lit depuis `GET /api/notifications`
/// au lieu d'un historique local reconstruit message par message.
@pragma('vm:entry-point')
Future<void> firebaseMessagingBackgroundHandler(RemoteMessage message) async {}

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

      // TODO (audit du 16/10/2026) : app ouverte au premier plan, FCM n'affiche pas de bannière
      // automatiquement sur Android — brancher flutter_local_notifications ici pour en afficher
      // une explicitement. Pas fait dans ce lot : nouvelle dépendance + config native Android/iOS
      // à tester sur un vrai appareil, hors de portée de cette session.

      // Notification tapée pendant que l'app est en arrière-plan.
      FirebaseMessaging.onMessageOpenedApp.listen(_handleNotificationTap);
      // App démarrée à froid en tapant directement sur la notification (terminée avant).
      final initialMessage = await messaging.getInitialMessage();
      if (initialMessage != null) _handleNotificationTap(initialMessage);

      _initialized = true;
    } catch (_) {
      // Firebase indisponible ou permission refusée : le reste de l'app continue sans push.
    }
  }

  /// Signalé dans le rapport de modification du 28/10/2026 : taper sur une notification de
  /// nouveau message n'ouvrait rien de précis, juste l'app sur son écran par défaut. Seul le
  /// type NEW_MESSAGE est traité ici (c'est la seule demande explicite) — les autres types de
  /// notification continuent d'ouvrir l'app normalement, sans navigation forcée.
  void _handleNotificationTap(RemoteMessage message) {
    final data = message.data;
    if (data['type'] != 'NEW_MESSAGE') return;
    final conversationId = data['conversationId'] as String?;
    if (conversationId == null) return;

    final navigatorState = navigatorKey.currentState;
    if (navigatorState == null) return;
    navigatorState.push(
      MaterialPageRoute(builder: (_) => SellerConversationScreen(conversationId: conversationId)),
    );
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
