import 'dart:async';

import 'api_client.dart';
import 'location_service.dart';

/// Envoie périodiquement la position du livreur au backend tant qu'il est "En ligne" sur son
/// dashboard (`PUT /api/delivery-agents/me/location`), pour que l'admin voie sur sa carte où se
/// trouvent les livreurs libres au moment d'affecter une livraison. Distinct du suivi GPS en
/// direct d'une course ([CourierLocationTrackingService], Firebase RTDB).
///
/// Silencieux par conception : un échec (GPS coupé, permission refusée, réseau) ne doit jamais
/// gêner le livreur, le ping suivant réessaie simplement.
class CourierPresenceService {
  static const _interval = Duration(seconds: 45);

  final _client = ApiClient();
  final _locationService = LocationService();
  Timer? _timer;

  bool get isRunning => _timer != null;

  void start() {
    if (_timer != null) return;
    _ping();
    _timer = Timer.periodic(_interval, (_) => _ping());
  }

  void stop() {
    _timer?.cancel();
    _timer = null;
  }

  Future<void> _ping() async {
    try {
      final position = await _locationService.getCurrentPosition();
      await _client.put('/api/delivery-agents/me/location', body: {
        'latitude': position.latitude,
        'longitude': position.longitude,
      });
    } catch (_) {
      // Voir la doc de la classe : réessai au prochain ping.
    }
  }
}
