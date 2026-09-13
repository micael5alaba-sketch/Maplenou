import '../models/courier_history_model.dart';

/// Provides the completed rides, payments and cancellations shown on
/// [CourierHistoryScreen]. Mocked for now — no network call.
class CourierHistoryService {
  CourierHistoryModel getHistory() {
    return CourierHistoryModel(
      completedRides: [
        RideHistoryItem(
          dateTime: DateTime.now().subtract(const Duration(hours: 3)),
          amount: 2500,
          fromLabel: 'Cocody, Riviera 2, Abidjan',
          toLabel: 'Marcory, Zone 4, Abidjan',
        ),
        RideHistoryItem(
          dateTime: DateTime.now().subtract(const Duration(days: 1, hours: 4)),
          amount: 3200,
          fromLabel: 'Plateau, Avenue Chardy, Abidjan',
          toLabel: 'Treichville, Avenue 16, Abidjan',
        ),
        RideHistoryItem(
          dateTime: DateTime(2026, 8, 22, 16, 45),
          amount: 1800,
          fromLabel: 'Yopougon, Niangon Nord, Abidjan',
          toLabel: 'Adjamé, Marché Gouro, Abidjan',
        ),
        RideHistoryItem(
          dateTime: DateTime(2026, 8, 20, 9, 30),
          amount: 4500,
          fromLabel: 'Bingerville, Carrefour CIE, Abidjan',
          toLabel: 'Cocody, Angré CHU, Abidjan',
        ),
      ],
      payments: [
        CourierPaymentModel(reference: '#TR-8829', date: DateTime(2026, 8, 24, 9, 15), amount: 45000),
      ],
      cancelledRides: [
        RideHistoryItem(
          dateTime: DateTime.now().subtract(const Duration(days: 1, hours: 6)),
          amount: 0,
          fromLabel: 'Plateau, Rue des Banques, Abidjan',
          toLabel: 'Cocody, Mermoz, Abidjan',
          cancelled: true,
          cancelReason: 'Client absent',
        ),
      ],
    );
  }
}
