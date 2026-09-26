import '../models/courier_history_model.dart';
import 'courier_delivery_service.dart';
import 'courier_earnings_service.dart';

/// Historique livreur : livraisons terminées/échouées
/// (`CourierDeliveryService.getMyDeliveries`) et gains versés
/// (`CourierEarningsService` — "Paiements" ne montre que les gains déjà
/// marqués `PAID` par un admin via `PATCH /api/admin/courier-earnings/{id}/paid`,
/// voir `CourierEarningService` côté backend).
class CourierHistoryService {
  final _deliveryService = CourierDeliveryService();
  final _earningsService = CourierEarningsService();

  Future<CourierHistoryModel> getHistory() async {
    final deliveries = await _deliveryService.getMyDeliveries();
    final earnings = await _earningsService.getEarnings();
    final earningByDelivery = {for (final e in earnings) e.deliveryId: e};

    final completed = <RideHistoryItem>[];
    final cancelled = <RideHistoryItem>[];

    for (final delivery in deliveries) {
      if (delivery.status != 'DELIVERED' && delivery.status != 'FAILED') continue;

      String toLabel = delivery.orderNumber;
      try {
        final navigation = await _deliveryService.getNavigation(delivery.id);
        toLabel = navigation.dropoff.address;
      } catch (_) {
        // Historique quand même affiché même si l'adresse ne charge pas.
      }

      final earning = earningByDelivery[delivery.id];
      final item = RideHistoryItem(
        dateTime: delivery.deliveredAt ?? delivery.createdAt,
        amount: earning?.amount ?? 0,
        fromLabel: delivery.shopName,
        toLabel: toLabel,
        cancelled: delivery.status == 'FAILED',
        cancelReason: delivery.status == 'FAILED' ? delivery.notes : null,
      );

      if (delivery.status == 'FAILED') {
        cancelled.add(item);
      } else {
        completed.add(item);
      }
    }

    final payments = earnings
        .where((e) => e.status == 'PAID')
        .map((e) => CourierPaymentModel(
              reference: '#${e.id.substring(0, 6).toUpperCase()}',
              date: e.paidAt ?? e.createdAt,
              amount: e.amount,
            ))
        .toList();

    return CourierHistoryModel(completedRides: completed, payments: payments, cancelledRides: cancelled);
  }
}
