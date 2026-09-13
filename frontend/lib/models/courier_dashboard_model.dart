import '../models/seller_dashboard_model.dart' show SalesPoint;

/// Status of an [ActiveDeliveryModel] shown on [CourierDashboardScreen].
enum ActiveDeliveryStatus { toCollect, enRoute }

/// One in-progress delivery shown on the courier's dashboard.
class ActiveDeliveryModel {
  final String id;
  final ActiveDeliveryStatus status;
  final double distanceKm;
  final int etaMinutes;
  final String pickupOrClientLabel;
  final String addressLine;

  /// Only set once the courier is en route to the client (cash to collect).
  final num? amountToCollect;
  final String? clientName;

  const ActiveDeliveryModel({
    required this.id,
    required this.status,
    required this.distanceKm,
    required this.etaMinutes,
    required this.pickupOrClientLabel,
    required this.addressLine,
    this.amountToCollect,
    this.clientName,
  });
}

/// Everything shown on [CourierDashboardScreen].
class CourierDashboardModel {
  final String courierName;
  final bool isOnline;

  final List<SalesPoint> weeklyEarningsSeries;
  final num weeklyTotal;
  final double weeklyChangePercent;

  final num dailyEarnings;
  final num dailyGoal;

  final double rating;
  final int validatedDeliveries;
  final int totalDeliveriesToday;

  final List<ActiveDeliveryModel> activeDeliveries;

  const CourierDashboardModel({
    required this.courierName,
    required this.isOnline,
    required this.weeklyEarningsSeries,
    required this.weeklyTotal,
    required this.weeklyChangePercent,
    required this.dailyEarnings,
    required this.dailyGoal,
    required this.rating,
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
