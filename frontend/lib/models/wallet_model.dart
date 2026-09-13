/// One payment method registered by the seller to receive payouts
/// (T-Money, Flooz, bank transfer...).
class PaymentMethodModel {
  final String name;
  final String maskedNumber;
  final bool isDefault;

  const PaymentMethodModel({
    required this.name,
    required this.maskedNumber,
    this.isDefault = false,
  });
}

/// Status of a [WithdrawalModel].
enum WithdrawalStatus { pending, completed }

/// One row in "Historique des retraits".
class WithdrawalModel {
  final DateTime date;
  final num amount;
  final String method;
  final WithdrawalStatus status;

  const WithdrawalModel({
    required this.date,
    required this.amount,
    required this.method,
    required this.status,
  });
}

/// Everything shown on [WalletScreen].
class WalletModel {
  final num availableBalance;

  /// e.g. 12.5 meaning "+12.5% ce mois-ci".
  final double changePercent;

  final List<PaymentMethodModel> paymentMethods;

  final num totalEarnedMonth;
  final int salesCountMonth;
  final num pendingAmount;
  final double averageRating;

  final List<WithdrawalModel> withdrawals;

  const WalletModel({
    required this.availableBalance,
    required this.changePercent,
    required this.paymentMethods,
    required this.totalEarnedMonth,
    required this.salesCountMonth,
    required this.pendingAmount,
    required this.averageRating,
    required this.withdrawals,
  });
}
