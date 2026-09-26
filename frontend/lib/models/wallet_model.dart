/// One payment method registered by the seller to receive payouts
/// (T-Money, Flooz, bank transfer...). Miroir de `PayoutMethodResponse`
/// (`payout/dto/PayoutMethodResponse.java`).
class PaymentMethodModel {
  final String id;
  final String name;
  final String maskedNumber;
  final bool isDefault;

  const PaymentMethodModel({
    required this.id,
    required this.name,
    required this.maskedNumber,
    this.isDefault = false,
  });

  static String labelForType(String type) {
    switch (type) {
      case 'T_MONEY':
        return 'T-Money';
      case 'FLOOZ':
        return 'Flooz';
      case 'BANK_TRANSFER':
        return 'Virement bancaire';
      default:
        return type;
    }
  }

  factory PaymentMethodModel.fromJson(Map<String, dynamic> json) {
    final accountNumber = json['accountNumber'] as String;
    final masked = accountNumber.length > 4
        ? '••• ${accountNumber.substring(accountNumber.length - 4)}'
        : accountNumber;
    return PaymentMethodModel(
      id: json['id'] as String,
      name: labelForType(json['type'] as String),
      maskedNumber: masked,
      isDefault: json['isDefault'] as bool? ?? false,
    );
  }
}

/// Status of a [WithdrawalModel].
enum WithdrawalStatus { pending, completed }

/// One row in "Historique des retraits". Miroir de `PayoutResponse`
/// (`payout/dto/PayoutResponse.java`).
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

  factory WithdrawalModel.fromJson(Map<String, dynamic> json) {
    final status = json['status'] as String;
    final paidAt = json['paidAt'] as String?;
    return WithdrawalModel(
      date: DateTime.parse(paidAt ?? json['createdAt'] as String),
      amount: json['amount'] as num,
      method: 'Reversement Maplenou',
      status: status == 'PAID' ? WithdrawalStatus.completed : WithdrawalStatus.pending,
    );
  }
}

/// Everything shown on [WalletScreen]. Assemblé depuis
/// `GET /api/sellers/me/kpis` (solde disponible), `GET /api/seller/payouts`
/// (historique + montant en attente) et `GET/POST /api/seller/payout-methods`.
class WalletModel {
  final num availableBalance;
  final List<PaymentMethodModel> paymentMethods;

  /// Total des reversements déjà payés (tout historique confondu — le
  /// backend n'a pas de filtrage par mois sur les payouts pour l'instant).
  final num totalPaidOut;

  /// Nombre de reversements déjà payés (pas un nombre de ventes : un
  /// reversement regroupe plusieurs sous-commandes livrées sur une semaine).
  final int paidPayoutsCount;

  final num pendingAmount;
  final double averageRating;

  final List<WithdrawalModel> withdrawals;

  const WalletModel({
    required this.availableBalance,
    required this.paymentMethods,
    required this.totalPaidOut,
    required this.paidPayoutsCount,
    required this.pendingAmount,
    required this.averageRating,
    required this.withdrawals,
  });
}
