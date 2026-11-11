import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/services/notification_router.dart';
import 'package:frontend/services/push_notification_service.dart';

void main() {
  test('un nouveau message ouvre la discussion', () {
    expect(
      resolveNotificationTarget({'type': 'NEW_MESSAGE', 'conversationId': 'c1'}),
      const NotificationTarget(NotificationTargetKind.conversation, 'c1'),
    );
  });

  test('une nouvelle commande vendeur ouvre le detail de la commande', () {
    expect(
      resolveNotificationTarget({'type': 'NEW_SUB_ORDER', 'subOrderId': 's9'}),
      const NotificationTarget(NotificationTargetKind.sellerOrderDetail, 's9'),
    );
  });

  test('une livraison assignee ouvre la course du livreur', () {
    expect(
      resolveNotificationTarget({'type': 'DELIVERY_ASSIGNED', 'deliveryId': 'd3'}),
      const NotificationTarget(NotificationTargetKind.courierDelivery, 'd3'),
    );
  });

  test('un nouveau produit ouvre la boutique', () {
    expect(
      resolveNotificationTarget({'type': 'NEW_PRODUCT', 'shopSlug': 'ma-boutique'}),
      const NotificationTarget(NotificationTargetKind.shop, 'ma-boutique'),
    );
  });

  test('commande payee ou annulee : suivi de la commande, sinon liste', () {
    expect(
      resolveNotificationTarget({'type': 'ORDER_PAID', 'orderId': 'o1'}),
      const NotificationTarget(NotificationTargetKind.buyerOrderTracking, 'o1'),
    );
    expect(
      resolveNotificationTarget({'type': 'SUB_ORDER_STATUS', 'subOrderId': 's1'}),
      const NotificationTarget(NotificationTargetKind.myOrders),
    );
  });

  test('type inconnu ou identifiant manquant : pas de destination forcee', () {
    expect(resolveNotificationTarget({'type': 'AUTRE'}), isNull);
    expect(resolveNotificationTarget({'type': 'NEW_MESSAGE'}), isNull);
    expect(resolveNotificationTarget({}), isNull);
  });

  test('la liste des notifications mene au meme ecran que le pop-up', () {
    final data = notificationDataFromHistory('NEW_MESSAGE', 'c7');
    expect(resolveNotificationTarget(data), const NotificationTarget(NotificationTargetKind.conversation, 'c7'));
    expect(
      resolveNotificationTarget(notificationDataFromHistory('NEW_PRODUCT', 'boutique-x')),
      const NotificationTarget(NotificationTargetKind.shop, 'boutique-x'),
    );
  });

  test('le parametre notif pose par le service worker est decode', () {
    final encoded = base64Url.encode(utf8.encode(jsonEncode({'type': 'NEW_MESSAGE', 'conversationId': 'abc'})));
    final data = PushNotificationService.decodeLaunchParameter(encoded.replaceAll('=', ''));
    expect(data, {'type': 'NEW_MESSAGE', 'conversationId': 'abc'});
    expect(PushNotificationService.decodeLaunchParameter('%%pas-du-base64%%'), isNull);
    expect(PushNotificationService.decodeLaunchParameter(null), isNull);
  });
}
