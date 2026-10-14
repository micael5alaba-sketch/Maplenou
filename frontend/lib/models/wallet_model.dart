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
  final String id;
  final DateTime date;
  final num amount;
  final String method;
  final WithdrawalStatus status;

  const WithdrawalModel({
    required this.id,
    required this.date,
    required this.amount,
    required this.method,
    required this.status,
  });

  factory WithdrawalModel.fromJson(Map<String, dynamic> json) {
    final status = json['status'] as String;
    final paidAt = json['paidAt'] as String?;
    return WithdrawalModel(
      id: json['id'] as String,
      date: DateTime.parse(paidAt ?? json['createdAt'] as String),
      amount: json['amount'] as num,
      method: 'Reversement Maplenou',
      status: status == 'PAID' ? WithdrawalStatus.completed : WithdrawalStatus.pending,
    );
  }
}

/// Détail d'un reversement, avec les sous-commandes incluses — pour générer
/// la facture PDF. Miroir de `PayoutDetailResponse`
/// (`payout/dto/PayoutDetailResponse.java`).
class PayoutDetailModel {
  final String id;
  final String shopName;
  final DateTime periodStart;
  final DateTime periodEnd;
  final num amount;
  final num commissionTotal;
  final String status;
  final DateTime? paidAt;
  final List<PayoutLineModel> subOrders;

  const PayoutDetailModel({
    required this.id,
    required this.shopName,
    required this.periodStart,
    required this.periodEnd,
    required this.amount,
    required this.commissionTotal,
    required this.status,
    this.paidAt,
    required this.subOrders,
  });

  factory PayoutDetailModel.fromJson(Map<String, dynamic> json) {
    final subOrders = json['subOrders'] as List<dynamic>? ?? [];
    return PayoutDetailModel(
      id: json['id'] as String,
      shopName: json['shopName'] as String,
      periodStart: DateTime.parse(json['periodStart'] as String),
      periodEnd: DateTime.parse(json['periodEnd'] as String),
      amount: json['amount'] as num,
      commissionTotal: json['commissionTotal'] as num,
      status: json['status'] as String,
      paidAt: json['paidAt'] != null ? DateTime.parse(json['paidAt'] as String) : null,
      subOrders: subOrders.map((s) => PayoutLineModel.fromJson(s as Map<String, dynamic>)).toList(),
    );
  }
}

/// One sub-order line within a [PayoutDetailModel]'s invoice. Pas de n° de
/// commande lisible côté `SubOrderSummary` (juste son UUID) : la ligne se
/// décrit plutôt par ses articles et sa date.
class PayoutLineModel {
  final DateTime createdAt;
  final String itemsSummary;
  final num subtotal;
  final num commissionAmount;
  final num netAmount;

  const PayoutLineModel({
    required this.createdAt,
    required this.itemsSummary,
    required this.subtotal,
    required this.commissionAmount,
    required this.netAmount,
  });

  factory PayoutLineModel.fromJson(Map<String, dynamic> json) {
    final items = json['items'] as List<dynamic>? ?? [];
    final summary = items.map((i) => (i as Map<String, dynamic>)['productName'] as String).join(', ');
    return PayoutLineModel(
      createdAt: DateTime.parse(json['createdAt'] as String),
      itemsSummary: summary.isEmpty ? 'Commande' : summary,
      subtotal: json['subtotal'] as num,
      commissionAmount: json['commissionAmount'] as num,
      netAmount: json['netAmount'] as num,
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
