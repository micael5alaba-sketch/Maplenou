import 'dart:async';

import 'package:firebase_database/firebase_database.dart';
import 'package:geolocator/geolocator.dart';

import 'firebase_auth_bridge.dart';
import 'location_service.dart';

/// Pousse la position GPS du livreur en temps réel sur Firebase Realtime
/// Database pendant une livraison IN_TRANSIT
/// (`deliveries/{deliveryId}/location`), pour que l'acheteur puisse la
/// suivre en direct sur [OrderTrackingScreen]. Le backend ne participe pas
/// à ce flux — voir `notification/FirebaseRealtimeDbStructure.java` pour le
/// contrat exact, c'est un échange 100% client Firebase ↔ client Firebase.
///
/// Silencieux par design si Firebase n'a pas pu s'initialiser (voir
/// `main.dart`) : [start] lève alors une [LocationException], à afficher
/// comme un message non bloquant plutôt qu'une erreur fatale.
class CourierLocationTrackingService {
  StreamSubscription<Position>? _subscription;

  bool get isTracking => _subscription != null;

  Future<void> start(String deliveryId) async {
    if (_subscription != null) return;

    if (!await Geolocator.isLocationServiceEnabled()) {
      throw LocationException('Activez la localisation dans les réglages de votre téléphone.');
    }
    var permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
      if (permission == LocationPermission.denied) {
        throw LocationException('Permission de localisation refusée.');
      }
    }
    if (permission == LocationPermission.deniedForever) {
      throw LocationException(
        "Permission de localisation bloquée — activez-la depuis les réglages de l'application.",
      );
    }

    final DatabaseReference ref;
    try {
      await FirebaseAuthBridge().ensureSignedIn();
      ref = FirebaseDatabase.instance.ref('deliveries/$deliveryId/location');
    } catch (_) {
      throw LocationException('Le suivi en direct est indisponible pour le moment.');
    }

    _subscription = Geolocator.getPositionStream(
      locationSettings: const LocationSettings(accuracy: LocationAccuracy.high, distanceFilter: 15),
    ).listen((position) {
      ref.set({
        'lat': position.latitude,
        'lng': position.longitude,
        'heading': position.heading,
        'updatedAt': DateTime.now().millisecondsSinceEpoch,
      });
    });
  }

  Future<void> stop() async {
    await _subscription?.cancel();
    _subscription = null;
  }
}
