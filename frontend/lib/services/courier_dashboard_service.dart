import '../models/courier_dashboard_model.dart';
import '../models/seller_dashboard_model.dart' show SalesPoint;

/// Provides the data shown on [CourierDashboardScreen].
///
/// Mocked for now — no network call. The courier's earnings/goal/rating
/// numbers have no backend model to map onto yet: `Payout` only exists for
/// shops, there is no per-delivery commission or balance tracked for
/// delivery agents (flagged as a gap — see the session notes on Livreur).
class CourierDashboardService {
  CourierDashboardModel getDashboard({required bool isOnline}) {
    return CourierDashboardModel(
      courierName: 'Amadou',
      isOnline: isOnline,
      weeklyEarningsSeries: const [
        SalesPoint(label: 'L', value: 22000),
        SalesPoint(label: 'M', value: 26000),
        SalesPoint(label: 'M', value: 21000),
        SalesPoint(label: 'J', value: 30000),
        SalesPoint(label: 'V', value: 34000),
        SalesPoint(label: 'S', value: 38000),
        SalesPoint(label: 'D', value: 14000),
      ],
      weeklyTotal: 185000,
      weeklyChangePercent: 12,
      dailyEarnings: 45000,
      dailyGoal: 60000,
      rating: 4.9,
      validatedDeliveries: 12,
      totalDeliveriesToday: 12,
      activeDeliveries: isOnline
          ? const [
              ActiveDeliveryModel(
                id: 'a1',
                status: ActiveDeliveryStatus.toCollect,
                distanceKm: 1.2,
                etaMinutes: 12,
                pickupOrClientLabel: 'Restaurant La Palmerae',
                addressLine: '45 Rue des Artisans',
              ),
              ActiveDeliveryModel(
                id: 'a2',
                status: ActiveDeliveryStatus.enRoute,
                distanceKm: 2.4,
                etaMinutes: 8,
                pickupOrClientLabel: 'Sarah M.',
                addressLine: 'Appt 4B, Résidence Océan',
                amountToCollect: 12500,
                clientName: 'Sarah M.',
              ),
            ]
          : const [],
    );
  }
}
