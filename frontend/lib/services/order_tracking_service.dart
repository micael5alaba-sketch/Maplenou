import '../models/order_tracking_model.dart';
import 'delivery_status_service.dart';
import 'order_service.dart';

/// Construit le suivi d'une commande à partir du vrai `Order`
/// (`OrderService.getOrder`) et, si elle existe déjà, de la vraie
/// `Delivery` de sa première sous-commande (`DeliveryStatusService`).
///
/// Simplification assumée : une commande multi-boutiques a une timeline par
/// sous-commande côté backend, mais cet écran n'affiche qu'un seul
/// parcours — celui de la première sous-commande, cas très majoritaire
/// (une seule boutique par commande) pour cette version.
class OrderTrackingService {
  final _orderService = OrderService();
  final _deliveryStatusService = DeliveryStatusService();

  static const _statusOrder = ['PENDING', 'PREPARING', 'READY_FOR_PICKUP', 'IN_DELIVERY', 'DELIVERED'];
  static const _statusLabels = {
    'PENDING': 'Commande reçue',
    'PREPARING': 'En préparation',
    'READY_FOR_PICKUP': 'Prête pour le retrait',
    'IN_DELIVERY': 'En livraison',
    'DELIVERED': 'Livrée',
  };

  Future<OrderTrackingModel> getTracking(String orderId) async {
    final order = await _orderService.getOrder(orderId);

    final deliveryAddress = order.deliveryAddress?.summary ?? 'Adresse de livraison de la commande';
    final addressNote = order.deliveryAddress?.details;

    if (order.subOrders.isEmpty) {
      return OrderTrackingModel(
        orderNumber: order.orderNumber,
        deliveryAddress: deliveryAddress,
        addressNote: addressNote,
        steps: const [OrderTrackingStep(label: 'Commande reçue', timeLabel: '', isDone: true, isCurrent: true)],
      );
    }

    final subOrder = order.subOrders.first;
    final currentStatus = subOrder.status;

    if (currentStatus == 'CANCELLED' || currentStatus == 'RETURN_REQUESTED' || currentStatus == 'RETURNED') {
      return OrderTrackingModel(
        orderNumber: order.orderNumber,
        deliveryAddress: deliveryAddress,
        addressNote: addressNote,
        steps: [OrderTrackingStep(label: _cancelledLabel(currentStatus), timeLabel: '', isDone: true, isCurrent: true)],
      );
    }

    final currentIndex = _statusOrder.indexOf(currentStatus);
    final steps = [
      for (var i = 0; i < _statusOrder.length; i++)
        OrderTrackingStep(
          label: _statusLabels[_statusOrder[i]]!,
          timeLabel: '',
          isDone: currentIndex >= 0 && i < currentIndex,
          isCurrent: i == currentIndex,
        ),
    ];

    String? courierName;
    String? deliveryId;
    if (currentStatus == 'IN_DELIVERY' || currentStatus == 'DELIVERED') {
      final delivery = await _deliveryStatusService.getForSubOrder(subOrder.id);
      courierName = delivery?.agentName;
      deliveryId = delivery?.id;
    }

    return OrderTrackingModel(
      orderNumber: order.orderNumber,
      deliveryId: deliveryId,
      courierName: courierName,
      deliveryAddress: deliveryAddress,
      addressNote: addressNote,
      steps: steps,
      isDelivered: currentStatus == 'DELIVERED',
    );
  }

  String _cancelledLabel(String status) {
    switch (status) {
      case 'RETURN_REQUESTED':
        return 'Retour demandé';
      case 'RETURNED':
        return 'Retournée';
      default:
        return 'Commande annulée';
    }
  }
}
