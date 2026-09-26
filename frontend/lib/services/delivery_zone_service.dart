import '../models/delivery_zone_model.dart';
import 'api_client.dart';

/// `GET /api/delivery-zones` (public, `DeliveryZoneController.java`) — liste
/// gérée dynamiquement par l'admin (`CLAUDE.md` §5). Mise en cache en mémoire
/// pour la session en cours : la liste change rarement et est consultée
/// depuis plusieurs écrans (fiche produit, checkout).
class DeliveryZoneService {
  static List<DeliveryZoneModel>? _cachedZones;

  Future<List<DeliveryZoneModel>> getActiveZones() async {
    final cached = _cachedZones;
    if (cached != null) return cached;
    final data = await ApiClient().get('/api/delivery-zones') as List<dynamic>;
    final zones = data
        .map((json) => DeliveryZoneModel.fromJson(json as Map<String, dynamic>))
        .where((zone) => zone.active)
        .toList();
    _cachedZones = zones;
    return zones;
  }

  /// Résout la zone à utiliser pour un quartier/ville donné : correspondance
  /// par nom de zone, sinon repli sur la zone active la moins chère (fiche
  /// produit sans adresse encore choisie, ou quartier non couvert).
  Future<DeliveryZoneModel?> resolveZone({String? district, String? city}) async {
    final zones = await getActiveZones();
    if (zones.isEmpty) return null;

    DeliveryZoneModel? matchByName(String? name) {
      if (name == null || name.trim().isEmpty) return null;
      final normalized = name.trim().toLowerCase();
      for (final zone in zones) {
        if (zone.name.trim().toLowerCase() == normalized) return zone;
      }
      return null;
    }

    final cheapest = [...zones]..sort((a, b) => a.deliveryFee.compareTo(b.deliveryFee));
    return matchByName(district) ?? matchByName(city) ?? cheapest.first;
  }
}
