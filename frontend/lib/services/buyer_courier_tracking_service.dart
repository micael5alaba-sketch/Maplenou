import 'package:firebase_database/firebase_database.dart';

import '../models/courier_live_location_model.dart';
import 'firebase_auth_bridge.dart';

/// Écoute la position GPS temps réel du livreur pendant une livraison en
/// cours, publiée par [CourierLocationTrackingService] sur Firebase
/// Realtime Database. Le flux renvoie `null` tant qu'aucune position n'a
/// encore été publiée (livreur pas encore parti) ou si Firebase est
/// indisponible — jamais une erreur qui casserait l'écran de suivi.
class BuyerCourierTrackingService {
  Stream<CourierLiveLocationModel?> watch(String deliveryId) async* {
    try {
      await FirebaseAuthBridge().ensureSignedIn();
    } catch (_) {
      yield null;
      return;
    }

    final ref = FirebaseDatabase.instance.ref('deliveries/$deliveryId/location');
    yield* ref.onValue.map((event) {
      final data = event.snapshot.value;
      if (data is! Map) return null;
      return CourierLiveLocationModel.fromMap(data);
    }).handleError((_) {});
  }
}
