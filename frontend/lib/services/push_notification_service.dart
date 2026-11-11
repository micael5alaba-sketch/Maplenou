import 'dart:convert';
import 'dart:io' show Platform;

import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart' show kIsWeb;

import '../widgets/in_app_notification_banner.dart';
import 'api_client.dart';
import 'courier_presence_service.dart';
import 'notification_router.dart';
import 'unread_messages_service.dart';

/// Handler appelé par le système quand une notification arrive alors que
/// l'app est en arrière-plan/fermée — doit être une fonction top-level
/// (isolate séparé), voir `main.dart`. Ne fait rien ici : le système (Android) ou le service
/// worker (navigateur, `web/firebase-messaging-sw.js`) affiche déjà le pop-up, et la
/// notification est persistée côté serveur avant l'envoi (`NotificationService.send`).
@pragma('vm:entry-point')
Future<void> firebaseMessagingBackgroundHandler(RemoteMessage message) async {}

/// Notifications push : enregistrement de l'appareil (`POST /api/notifications/tokens`), pop-up
/// dans l'app quand elle est ouverte, et ouverture de l'écran concerné au tap sur une notification
/// (discussion, commande, livraison, boutique...) — quel que soit l'état de l'app (premier plan,
/// arrière-plan, fermée) et sur le navigateur web comme sur mobile.
class PushNotificationService {
  static Map<String, dynamic>? _pendingLaunch;

  bool _initialized = false;

  Future<void> initialize() async {
    if (_initialized) return;
    try {
      final messaging = FirebaseMessaging.instance;
      UnreadMessagesService.instance.start();
      await messaging.requestPermission(alert: true, badge: true, sound: true);
      await _registerCurrentToken();
      messaging.onTokenRefresh.listen(_registerToken);

      // App ouverte au premier plan : le système n'affiche aucune bannière, on affiche la nôtre
      // (pop-up en haut de l'écran, cliquable).
      FirebaseMessaging.onMessage.listen((message) {
        final title = message.notification?.title ?? 'Maplenou';
        final body = message.notification?.body ?? '';
        if (message.data['type'] == 'NEW_MESSAGE') UnreadMessagesService.instance.refresh();
        InAppNotificationBanner.show(title: title, body: body, data: Map<String, dynamic>.from(message.data));
      });

      // Pop-up système tapé pendant que l'app est en arrière-plan : l'écran concerné s'ouvre.
      FirebaseMessaging.onMessageOpenedApp
          .listen((message) => openNotification(Map<String, dynamic>.from(message.data)));

      // App démarrée à froid en tapant sur le pop-up : traité par handleLaunchNotification() une
      // fois l'écran d'accueil en place (le splash remplacerait un écran ouvert trop tôt).
      final initialMessage = await messaging.getInitialMessage();
      if (initialMessage != null) _pendingLaunch = Map<String, dynamic>.from(initialMessage.data);

      _initialized = true;
    } catch (_) {
      // Firebase indisponible ou permission refusée : le reste de l'app continue sans push.
    }
  }

  /// À appeler une fois l'écran d'accueil affiché : ouvre l'écran lié à la notification qui a
  /// lancé l'app (mobile : message initial ; navigateur : paramètre `notif` de l'URL posé par le
  /// service worker au clic sur le pop-up).
  void handleLaunchNotification() {
    var data = _pendingLaunch;
    _pendingLaunch = null;
    data ??= decodeLaunchParameter(Uri.base.queryParameters['notif']);
    if (data != null) openNotification(data);
  }

  /// Décode le paramètre `notif` (JSON encodé en base64 URL-safe) posé par le service worker.
  static Map<String, dynamic>? decodeLaunchParameter(String? param) {
    if (param == null || param.isEmpty) return null;
    try {
      final json = utf8.decode(base64Url.decode(base64Url.normalize(param)));
      return Map<String, dynamic>.from(jsonDecode(json) as Map);
    } catch (_) {
      return null;
    }
  }

  /// Vrai si l'utilisateur a autorisé les notifications sur cet appareil/navigateur.
  Future<bool> isPermissionGranted() async {
    try {
      final settings = await FirebaseMessaging.instance.getNotificationSettings();
      return settings.authorizationStatus == AuthorizationStatus.authorized ||
          settings.authorizationStatus == AuthorizationStatus.provisional;
    } catch (_) {
      return false;
    }
  }

  /// Demande l'autorisation (à appeler depuis un geste de l'utilisateur : Safari et certains
  /// navigateurs refusent la demande automatique) puis enregistre l'appareil.
  Future<bool> enable() async {
    try {
      final settings = await FirebaseMessaging.instance.requestPermission(alert: true, badge: true, sound: true);
      final granted = settings.authorizationStatus == AuthorizationStatus.authorized ||
          settings.authorizationStatus == AuthorizationStatus.provisional;
      if (granted) await _registerCurrentToken();
      return granted;
    } catch (_) {
      return false;
    }
  }

  Future<void> _registerCurrentToken() async {
    final token = await FirebaseMessaging.instance.getToken();
    if (token != null) await _registerToken(token);
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
    UnreadMessagesService.instance.stop();
    CourierPresenceService.instance.stop();
    try {
      await ApiClient().delete('/api/notifications/tokens');
    } catch (_) {}
  }
}
