import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:geolocator/geolocator.dart';

import 'api_client.dart';

/// État de la localisation du livreur, pour l'afficher (bandeau sur son tableau de bord).
enum PresenceStatus {
  /// Pas encore démarré (ou arrêté).
  idle,

  /// Position envoyée en continu : le système peut lui affecter les courses proches.
  active,

  /// Localisation refusée ou désactivée : aucune course ne peut lui être affectée automatiquement.
  denied,
}

/// Envoie en permanence la position du livreur au backend (`PUT /api/delivery-agents/me/location`)
/// dès qu'il a activé la localisation — plus de bouton « En ligne » à actionner. Le système connaît
/// ainsi la position de chaque livreur à tout instant et affecte une course au livreur le plus proche
/// de la boutique (voir `DeliveryDispatchService` côté backend) ; l'admin voit les mêmes positions
/// sur sa carte et peut toujours affecter à la main.
///
/// Sur Android, le suivi continue application en arrière-plan grâce à un service de premier plan
/// (notification permanente « localisation active ») ; sur le web, uniquement tant que la page est
/// ouverte. Silencieux par conception : un échec réseau ou GPS ne gêne jamais le livreur, le
/// prochain envoi réessaie.
class CourierPresenceService extends ChangeNotifier {
  CourierPresenceService._();

  static final CourierPresenceService instance = CourierPresenceService._();

  /// Un envoi au plus toutes les 20 s quand le livreur bouge...
  static const _minSendInterval = Duration(seconds: 20);

  /// ...et un envoi de rappel toutes les 60 s même immobile (position « fraîche » côté serveur).
  static const _keepAliveInterval = Duration(seconds: 60);

  final _client = ApiClient();

  PresenceStatus _status = PresenceStatus.idle;
  StreamSubscription<Position>? _subscription;
  Timer? _keepAlive;
  Position? _lastPosition;
  DateTime? _lastSent;

  PresenceStatus get status => _status;

  /// Démarre le suivi (idempotent). Demande la permission de localisation si nécessaire.
  Future<void> start() async {
    if (_subscription != null) return;
    if (!await _ensurePermission()) {
      _setStatus(PresenceStatus.denied);
      return;
    }

    _subscription = Geolocator.getPositionStream(locationSettings: _settings()).listen(
      _onPosition,
      onError: (_) {
        // GPS momentanément indisponible : le flux reprend tout seul.
      },
    );
    _keepAlive = Timer.periodic(_keepAliveInterval, (_) => _send(force: true));
    _setStatus(PresenceStatus.active);
    // Première position tout de suite (le flux peut mettre quelques secondes à émettre).
    try {
      _onPosition(await Geolocator.getCurrentPosition(locationSettings: _settings()));
    } catch (_) {}
  }

  /// Arrête le suivi (déconnexion).
  Future<void> stop() async {
    await _subscription?.cancel();
    _subscription = null;
    _keepAlive?.cancel();
    _keepAlive = null;
    _lastPosition = null;
    _lastSent = null;
    _setStatus(PresenceStatus.idle);
  }

  /// Nouvelle tentative après un refus (bouton « Activer la localisation »).
  Future<void> retry() async {
    await stop();
    await start();
  }

  void _setStatus(PresenceStatus status) {
    if (_status == status) return;
    _status = status;
    notifyListeners();
  }

  LocationSettings _settings() {
    if (!kIsWeb && defaultTargetPlatform == TargetPlatform.android) {
      return AndroidSettings(
        accuracy: LocationAccuracy.high,
        distanceFilter: 25,
        intervalDuration: const Duration(seconds: 20),
        // Service de premier plan : continue d'envoyer la position application en arrière-plan.
        foregroundNotificationConfig: const ForegroundNotificationConfig(
          notificationTitle: 'Maplenou — localisation active',
          notificationText: 'Vous recevez les courses proches de vous.',
          enableWakeLock: true,
        ),
      );
    }
    if (!kIsWeb && defaultTargetPlatform == TargetPlatform.iOS) {
      return AppleSettings(
        accuracy: LocationAccuracy.high,
        distanceFilter: 25,
        activityType: ActivityType.automotiveNavigation,
        pauseLocationUpdatesAutomatically: false,
        showBackgroundLocationIndicator: true,
        allowBackgroundLocationUpdates: true,
      );
    }
    return const LocationSettings(accuracy: LocationAccuracy.high, distanceFilter: 25);
  }

  Future<bool> _ensurePermission() async {
    try {
      if (!await Geolocator.isLocationServiceEnabled()) return false;
      var permission = await Geolocator.checkPermission();
      if (permission == LocationPermission.denied) {
        permission = await Geolocator.requestPermission();
      }
      return permission == LocationPermission.always || permission == LocationPermission.whileInUse;
    } catch (_) {
      return false;
    }
  }

  void _onPosition(Position position) {
    _lastPosition = position;
    final last = _lastSent;
    if (last == null || DateTime.now().difference(last) >= _minSendInterval) {
      _send();
    }
  }

  Future<void> _send({bool force = false}) async {
    final position = _lastPosition;
    if (position == null) return;
    if (!force && _lastSent != null && DateTime.now().difference(_lastSent!) < _minSendInterval) return;
    _lastSent = DateTime.now();
    try {
      await _client.put('/api/delivery-agents/me/location', body: {
        'latitude': position.latitude,
        'longitude': position.longitude,
      });
    } catch (_) {
      // Réessayé au prochain envoi.
    }
  }
}
