import '../models/seller_dashboard_model.dart' show SalesPoint;

/// Status of an [ActiveDeliveryModel] shown on [CourierDashboardScreen] —
/// miroir simplifié de `DeliveryStatus` (`ASSIGNED` → toCollect,
/// `IN_TRANSIT` → enRoute).
enum ActiveDeliveryStatus { toCollect, enRoute }

/// One in-progress delivery shown on the courier's dashboard.
class ActiveDeliveryModel {
  final String id;
  final ActiveDeliveryStatus status;
  final double? distanceKm;
  final String pickupOrClientLabel;
  final String addressLine;

  const ActiveDeliveryModel({
    required this.id,
    required this.status,
    this.distanceKm,
    required this.pickupOrClientLabel,
    required this.addressLine,
  });
}

/// Everything shown on [CourierDashboardScreen]. `isOnline` reste un état
/// purement local (bascule d'UI) — le backend n'a pas de notion de
/// disponibilité livreur. Les gains/compteurs viennent de
/// `CourierEarningsService`/`CourierDeliveryService` ; [rating] reste
/// `null` (pas de système de notation livreur), [dailyGoal] est un
/// objectif fixe côté app, pas une donnée backend.
class CourierDashboardModel {
  final String courierName;
  final bool isOnline;

  final List<SalesPoint> weeklyEarningsSeries;
  final num weeklyTotal;
  final double? weeklyChangePercent;

  final num dailyEarnings;
  final num dailyGoal;

  final double? rating;
  final int validatedDeliveries;
  final int totalDeliveriesToday;

  final List<ActiveDeliveryModel> activeDeliveries;

  const CourierDashboardModel({
    required this.courierName,
    required this.isOnline,
    required this.weeklyEarningsSeries,
    required this.weeklyTotal,
    this.weeklyChangePercent,
    required this.dailyEarnings,
    required this.dailyGoal,
    this.rating,
    required this.validatedDeliveries,
    required this.totalDeliveriesToday,
    required this.activeDeliveries,
  });

  CourierDashboardModel copyWith({bool? isOnline, List<ActiveDeliveryModel>? activeDeliveries}) {
    return CourierDashboardModel(
      courierName: courierName,
      isOnline: isOnline ?? this.isOnline,
      weeklyEarningsSeries: weeklyEarningsSeries,
      weeklyTotal: weeklyTotal,
      weeklyChangePercent: weeklyChangePercent,
      dailyEarnings: dailyEarnings,
      dailyGoal: dailyGoal,
      rating: rating,
      validatedDeliveries: validatedDeliveries,
      totalDeliveriesToday: totalDeliveriesToday,
      activeDeliveries: activeDeliveries ?? this.activeDeliveries,
    );
  }
}
