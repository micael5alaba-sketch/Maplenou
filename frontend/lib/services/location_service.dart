import 'package:geocoding/geocoding.dart';
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

/// Ville/quartier déduits d'une position GPS par géocodage inverse — tous
/// deux nullables : le géocodeur natif de l'appareil ne retourne pas
/// toujours un résultat exploitable (zone rurale, couverture cartographique
/// locale incomplète), notamment en Afrique de l'Ouest.
class ReverseGeocodeResult {
  final String? city;
  final String? district;

  const ReverseGeocodeResult({this.city, this.district});
}

/// Capture la position GPS actuelle de l'appareil, en gérant permission et
/// service de localisation. Utilisé pour géolocaliser une adresse acheteur
/// à la création (voir `AddressesScreen`) — le backend sait déjà tout
/// stocker/exposer (`Address.latitude/longitude`), il ne manquait que
/// cette capture côté app.
///
/// Le géocodage inverse (position -> ville/quartier) passe par le géocodeur
/// natif de l'appareil (`geocoding`, Android `Geocoder`/iOS `CLGeocoder`) —
/// pas de compte Google Maps Platform payant, conformément à la décision du
/// projet (voir `INVENTAIRE_ABONNEMENTS.md` §3).
class LocationService {
  Future<Position> getCurrentPosition() async {
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

    return Geolocator.getCurrentPosition(
      locationSettings: const LocationSettings(accuracy: LocationAccuracy.high),
    );
  }

  /// Renvoie `null` plutôt que de lever une exception : un échec de
  /// géocodage ne doit jamais bloquer l'enregistrement de l'adresse, la
  /// position GPS brute reste de toute façon capturée.
  Future<ReverseGeocodeResult?> reverseGeocode(double latitude, double longitude) async {
    try {
      final placemarks = await placemarkFromCoordinates(latitude, longitude);
      if (placemarks.isEmpty) return null;
      final place = placemarks.first;
      final city = (place.locality?.isNotEmpty ?? false)
          ? place.locality
          : (place.subAdministrativeArea?.isNotEmpty ?? false)
              ? place.subAdministrativeArea
              : place.administrativeArea;
      final district = (place.subLocality?.isNotEmpty ?? false) ? place.subLocality : place.thoroughfare;
      return ReverseGeocodeResult(city: city, district: district);
    } catch (_) {
      return null;
    }
  }
}
