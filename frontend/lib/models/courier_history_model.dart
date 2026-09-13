/// One completed or cancelled ride in [CourierHistoryScreen]'s "Courses"
/// tab.
class RideHistoryItem {
  final DateTime dateTime;
  final num amount;
  final String fromLabel;
  final String toLabel;
  final bool cancelled;
  final String? cancelReason;

  const RideHistoryItem({
    required this.dateTime,
    required this.amount,
    required this.fromLabel,
    required this.toLabel,
    this.cancelled = false,
    this.cancelReason,
  });
}

/// One payout received, shown in [CourierHistoryScreen]'s "Paiements" tab.
class CourierPaymentModel {
  final String reference;
  final DateTime date;
  final num amount;

  const CourierPaymentModel({required this.reference, required this.date, required this.amount});
}

class CourierHistoryModel {
  final List<RideHistoryItem> completedRides;
  final List<CourierPaymentModel> payments;
  final List<RideHistoryItem> cancelledRides;

  const CourierHistoryModel({
    required this.completedRides,
    required this.payments,
    required this.cancelledRides,
  });
}
