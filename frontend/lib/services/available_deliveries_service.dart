import '../models/available_delivery_model.dart';

/// Provides the feed of deliveries a courier can accept, shown on
/// [CourierDeliveriesScreen]. Mocked for now — no network call, no real
/// live/polling feed either (the maquette's "Actualisation en direct..."
/// footer implies one, which would need a backend push/polling endpoint
/// that doesn't exist yet).
class AvailableDeliveriesService {
  List<AvailableDeliveryModel> getAvailableDeliveries() {
    final now = DateTime.now();
    return [
      AvailableDeliveryModel(
        id: 'd1',
        amount: 2500,
        postedAt: now.subtract(const Duration(minutes: 5)),
        pickupLabel: 'Le Maquis Doré',
        pickupDistanceKm: 1.2,
        pickupAddress: 'Zone 4, Rue Paul Langevin',
        dropoffLabel: 'Marcory Résidentiel',
        totalDistanceKm: 4.2,
        dropoffAddress: 'Mme. Konan',
        category: 'Restauration chaude',
        itemsSummary: '3 articles',
      ),
      AvailableDeliveryModel(
        id: 'd2',
        amount: 1800,
        postedAt: now.subtract(const Duration(minutes: 15)),
        pickupLabel: 'Plateau',
        pickupDistanceKm: 0.5,
        pickupAddress: 'Burger Palace, Avenue Chardy',
        dropoffLabel: 'Plateau',
        totalDistanceKm: 1.5,
        dropoffAddress: 'Tour Alpha, Bureau 402',
        category: 'Fast Food',
        itemsSummary: '1 sac',
      ),
      AvailableDeliveryModel(
        id: 'd3',
        amount: 1200,
        postedAt: now.subtract(const Duration(minutes: 25)),
        pickupLabel: 'Cocody',
        pickupDistanceKm: 2.1,
        pickupAddress: 'Pharmacie de la Paix',
        dropoffLabel: 'Riviera 3',
        totalDistanceKm: 3.8,
        dropoffAddress: 'Cité Verdoyante',
        category: 'Pharmacie',
        itemsSummary: '1 sachet',
      ),
      AvailableDeliveryModel(
        id: 'd4',
        amount: 3100,
        postedAt: now.subtract(const Duration(minutes: 40)),
        pickupLabel: 'Treichville',
        pickupDistanceKm: 1.8,
        pickupAddress: 'Atelier Tissage Ivoire',
        dropoffLabel: 'Plateau',
        totalDistanceKm: 6.2,
        dropoffAddress: 'Boutique Artisanale',
        category: 'Colis volumineux',
        itemsSummary: '1 carton',
      ),
    ];
  }
}
