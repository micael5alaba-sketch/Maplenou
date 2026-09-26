import '../models/courier_dashboard_model.dart';
import '../models/courier_earning_model.dart';
import '../models/seller_dashboard_model.dart' show SalesPoint;
import '../models/user_model.dart';
import 'courier_delivery_service.dart';
import 'courier_earnings_service.dart';
import 'user_service.dart';

/// Assemble le dashboard livreur depuis trois sources réelles :
/// `GET /api/users/me` (nom), `CourierEarningsService` (gains) et
/// `CourierDeliveryService` (livraisons assignées/en cours/terminées).
/// `isOnline` reste un état local (pas de notion backend de disponibilité) ;
/// `dailyGoal` un objectif fixe côté app, pas une donnée mesurée ;
/// `rating` reste `null` (pas de système de notation livreur).
class CourierDashboardService {
  static const num _dailyGoal = 10000;

  final _userService = UserService();
  final _earningsService = CourierEarningsService();
  final _deliveryService = CourierDeliveryService();

  Future<CourierDashboardModel> getDashboard({required bool isOnline}) async {
    final UserModel user = await _userService.getCurrentUser();
    final earnings = await _earningsService.getEarnings();
    final deliveries = await _deliveryService.getMyDeliveries();

    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);

    final weeklySeries = _buildWeeklySeries(earnings, now);
    final weeklyTotal = weeklySeries.fold<num>(0, (sum, p) => sum + p.value);
    final previousWeekTotal = _sumInRange(earnings, now.subtract(const Duration(days: 14)), now.subtract(const Duration(days: 7)));
    final weeklyChangePercent = previousWeekTotal == 0
        ? null
        : ((weeklyTotal - previousWeekTotal) / previousWeekTotal * 100).toDouble();

    final dailyEarnings = earnings
        .where((e) => !e.createdAt.isBefore(today))
        .fold<num>(0, (sum, e) => sum + e.amount);

    final delivered = deliveries.where((d) => d.status == 'DELIVERED');
    final deliveredToday = delivered.where((d) => !(d.deliveredAt ?? d.createdAt).isBefore(today));

    final activeStatuses = deliveries.where((d) => d.status == 'ASSIGNED' || d.status == 'IN_TRANSIT').toList();
    final activeDeliveries = <ActiveDeliveryModel>[];
    for (final delivery in activeStatuses) {
      final navigation = await _deliveryService.getNavigation(delivery.id);
      final isEnRoute = delivery.status == 'IN_TRANSIT';
      activeDeliveries.add(ActiveDeliveryModel(
        id: delivery.id,
        status: isEnRoute ? ActiveDeliveryStatus.enRoute : ActiveDeliveryStatus.toCollect,
        distanceKm: _deliveryService.straightLineDistanceKm(navigation),
        pickupOrClientLabel: isEnRoute ? navigation.dropoff.label : navigation.pickup.label,
        addressLine: isEnRoute ? navigation.dropoff.address : navigation.pickup.address,
      ));
    }

    return CourierDashboardModel(
      courierName: user.fullName,
      isOnline: isOnline,
      weeklyEarningsSeries: weeklySeries,
      weeklyTotal: weeklyTotal,
      weeklyChangePercent: weeklyChangePercent,
      dailyEarnings: dailyEarnings,
      dailyGoal: _dailyGoal,
      rating: null,
      validatedDeliveries: delivered.length,
      totalDeliveriesToday: deliveredToday.length,
      activeDeliveries: isOnline ? activeDeliveries : const [],
    );
  }

  List<SalesPoint> _buildWeeklySeries(List<CourierEarningModel> earnings, DateTime now) {
    const labels = ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'];
    final today = DateTime(now.year, now.month, now.day);
    final points = <SalesPoint>[];
    for (var i = 6; i >= 0; i--) {
      final day = today.subtract(Duration(days: i));
      final total = earnings
          .where((e) => e.createdAt.year == day.year && e.createdAt.month == day.month && e.createdAt.day == day.day)
          .fold<num>(0, (sum, e) => sum + e.amount);
      points.add(SalesPoint(label: labels[day.weekday - 1], value: total.toDouble()));
    }
    return points;
  }

  num _sumInRange(List<CourierEarningModel> earnings, DateTime from, DateTime to) {
    return earnings
        .where((e) => !e.createdAt.isBefore(from) && e.createdAt.isBefore(to))
        .fold<num>(0, (sum, e) => sum + e.amount);
  }
}
