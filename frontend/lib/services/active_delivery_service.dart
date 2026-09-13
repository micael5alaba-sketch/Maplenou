import '../models/active_delivery_detail_model.dart';

/// Provides the delivery-in-progress detail shown on
/// [CourierDeliveryDetailScreen] / [CourierDeliveryConfirmationScreen].
/// Mocked for now — no network call, and no live GPS position feed either
/// (flagged as a gap: the backend has no notion of a courier's live
/// position, only a final delivered/failed status).
class ActiveDeliveryService {
  ActiveDeliveryDetailModel getActiveDelivery(String deliveryId) {
    return const ActiveDeliveryDetailModel(
      orderNumber: '#CMD-8492',
      clientName: 'Aisha Diallo',
      clientOrdersCount: 3,
      clientRating: 4.9,
      deliveryAddress: 'Appartement 4B, Résidence Les Baobabs, Route de Ngor, Dakar',
      doorCode: '1234',
      etaMinutes: 12,
      items: ['2x Savon Artisanal Karité', '1x Huile Essentielle Baobab'],
      amountToCollect: 12500,
    );
  }
}
