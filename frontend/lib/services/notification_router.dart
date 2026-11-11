import 'package:flutter/material.dart';

import '../navigation.dart';
import '../screens/courier_delivery_detail_screen.dart';
import '../screens/my_orders_screen.dart';
import '../screens/order_detail_screen.dart';
import '../screens/order_tracking_screen.dart';
import '../screens/orders_management_screen.dart';
import '../screens/seller_conversation_screen.dart';
import '../screens/seller_dashboard_screen.dart';
import '../screens/shop_detail_screen.dart';
import '../screens/vendor_catalog_screen.dart';

/// Où mène un tap sur une notification (pop-up système, bannière dans l'app ou liste).
enum NotificationTargetKind {
  conversation,
  sellerOrderDetail,
  buyerOrderTracking,
  myOrders,
  courierDelivery,
  sellerOrders,
  vendorCatalog,
  sellerDashboard,
  shop,
}

class NotificationTarget {
  final NotificationTargetKind kind;

  /// Identifiant de la ressource (conversation, commande, livraison, slug de boutique...), si le
  /// type d'écran en a besoin.
  final String? id;

  const NotificationTarget(this.kind, [this.id]);

  @override
  bool operator ==(Object other) => other is NotificationTarget && other.kind == kind && other.id == id;

  @override
  int get hashCode => Object.hash(kind, id);
}

/// Décide de l'écran à ouvrir à partir des données d'une notification (charge utile FCM). Fonction
/// pure, testée sans navigation. `null` = pas de destination précise (l'app s'ouvre simplement).
NotificationTarget? resolveNotificationTarget(Map<String, dynamic> data) {
  String? text(String key) {
    final value = data[key];
    return value is String && value.isNotEmpty ? value : null;
  }

  switch (text('type')) {
    case 'NEW_MESSAGE':
      final id = text('conversationId');
      return id == null ? null : NotificationTarget(NotificationTargetKind.conversation, id);
    case 'NEW_SUB_ORDER':
      final id = text('subOrderId');
      return id == null ? null : NotificationTarget(NotificationTargetKind.sellerOrderDetail, id);
    case 'ORDER_PAID':
    case 'ORDER_CANCELLED':
      final id = text('orderId');
      return id == null
          ? const NotificationTarget(NotificationTargetKind.myOrders)
          : NotificationTarget(NotificationTargetKind.buyerOrderTracking, id);
    case 'SUB_ORDER_STATUS':
    case 'RETURN_DECIDED':
      return const NotificationTarget(NotificationTargetKind.myOrders);
    case 'DELIVERY_ASSIGNED':
      final id = text('deliveryId');
      return id == null ? null : NotificationTarget(NotificationTargetKind.courierDelivery, id);
    case 'RETURN_CREATED':
      return const NotificationTarget(NotificationTargetKind.sellerOrders);
    case 'LOW_STOCK':
      return const NotificationTarget(NotificationTargetKind.vendorCatalog);
    case 'SHOP_STATUS':
    case 'SELLER_FEE_ACTIVATED':
    case 'SELLER_FEE_DEDUCTED':
      return const NotificationTarget(NotificationTargetKind.sellerDashboard);
    case 'NEW_PRODUCT':
      final slug = text('shopSlug');
      return slug == null ? null : NotificationTarget(NotificationTargetKind.shop, slug);
  }
  return null;
}

/// Reconstitue les données d'une notification de l'historique serveur (type + référence) pour
/// ouvrir le même écran que le pop-up : la liste des notifications est cliquable elle aussi.
Map<String, dynamic> notificationDataFromHistory(String type, String? referenceId) {
  final key = switch (type) {
    'NEW_MESSAGE' => 'conversationId',
    'NEW_SUB_ORDER' => 'subOrderId',
    'ORDER_PAID' || 'ORDER_CANCELLED' => 'orderId',
    'DELIVERY_ASSIGNED' => 'deliveryId',
    'NEW_PRODUCT' => 'shopSlug',
    _ => null,
  };
  return {'type': type, if (key != null && referenceId != null) key: referenceId};
}

Widget? _screenFor(NotificationTarget target) {
  final id = target.id;
  return switch (target.kind) {
    NotificationTargetKind.conversation => SellerConversationScreen(conversationId: id!),
    NotificationTargetKind.sellerOrderDetail => OrderDetailScreen(subOrderId: id!),
    NotificationTargetKind.buyerOrderTracking => OrderTrackingScreen(orderId: id!),
    NotificationTargetKind.myOrders => const MyOrdersScreen(),
    NotificationTargetKind.courierDelivery => CourierDeliveryDetailScreen(deliveryId: id!),
    NotificationTargetKind.sellerOrders => const OrdersManagementScreen(),
    NotificationTargetKind.vendorCatalog => const VendorCatalogScreen(),
    NotificationTargetKind.sellerDashboard => const SellerDashboardScreen(),
    NotificationTargetKind.shop => ShopDetailScreen(shopSlug: id!),
  };
}

/// Ouvre l'écran lié à une notification. Sans destination précise, ne fait rien (l'app est déjà
/// ouverte sur l'écran courant).
void openNotification(Map<String, dynamic> data) {
  final target = resolveNotificationTarget(data);
  if (target == null) return;
  final navigator = navigatorKey.currentState;
  final screen = _screenFor(target);
  if (navigator == null || screen == null) return;
  navigator.push(MaterialPageRoute(builder: (_) => screen));
}
