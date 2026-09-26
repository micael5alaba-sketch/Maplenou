import 'package:geolocator/geolocator.dart';

/// Erreur levée quand la position n'a pas pu être obtenue (permission
/// refusée, service de localisation désactivé, etc.) — le message est déjà
/// prêt à être affiché tel quel dans un SnackBar.
class LocationException implements Exception {
  final String message;
  LocationException(this.message);

  @override
  String toString() => message;
}

/// Capture la position GPS actuelle de l'appareil, en gérant permission et
/// service de localisation. Utilisé pour géolocaliser une adresse acheteur
/// à la création (voir `AddressesScreen`) — le backend sait déjà tout
/// stocker/exposer (`Address.latitude/longitude`), il ne manquait que
/// cette capture côté app.
class LocationService {
  Future<Position> getCurrentPosition() async {
    if (!await Geolocator.isLocationServiceEnabled()) {
      throw LocationException('Active la localisation dans les réglages de ton téléphone.');
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
        "Permission de localisation bloquée — active-la depuis les réglages de l'application.",
      );
    }

    return Geolocator.getCurrentPosition(
      locationSettings: const LocationSettings(accuracy: LocationAccuracy.high),
    );
  }
}
