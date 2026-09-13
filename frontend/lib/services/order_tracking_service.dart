import '../models/order_tracking_model.dart';

/// Provides the data shown on [OrderTrackingScreen].
///
/// Mocked for now — no network call, and no live GPS position feed either
/// (the backend's `Delivery` only has a final status, not a live position —
/// flagged as a gap alongside the courier dashboard's equivalent one).
/// The status timeline itself maps directly onto real `SubOrderStatus`
/// transitions (`PENDING`→`PREPARING`→`READY_FOR_PICKUP`→`IN_DELIVERY`→
/// `DELIVERED`, see `MODELE_DONNEES.md`) once wired up.
class OrderTrackingService {
  OrderTrackingModel getTracking(String orderNumber) {
    return OrderTrackingModel(
      orderNumber: orderNumber,
      etaLabel: "Aujourd'hui, 14:30 - 15:00",
      courierName: 'Koffi Mensah',
      courierRating: 4.9,
      courierDeliveryCount: 120,
      deliveryAddress: 'Résidence Les Cocotiers, Apt 4B, Cocody Danga, Abidjan',
      addressNote: "Appeler en arrivant au portail.",
      steps: const [
        OrderTrackingStep(label: 'Commande confirmée', timeLabel: '13:45 - Payé avec succès', isDone: true),
        OrderTrackingStep(label: 'En préparation', timeLabel: '14:00 - Le vendeur prépare votre colis', isDone: true),
        OrderTrackingStep(label: 'Remis au livreur', timeLabel: '14:15 - En route vers votre adresse', isCurrent: true),
        OrderTrackingStep(label: 'Livré', timeLabel: ''),
      ],
    );
  }
}
