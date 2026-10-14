/// Résumé des frais mensuels d'activité (voir `CLAUDE.md` — "Exigences des
/// nouveaux frais vendeurs"). Miroir de `SellerFeeSummaryResponse`
/// (`sellerfee/dto/SellerFeeSummaryResponse.java`).
class SellerFeeSummaryModel {
  final String category;
  final num monthlyFeeAmount;
  final num unpaidFeesTotal;
  final num availableBalance;
  final num estimatedNetPayout;
  final String? pendingCategoryChangeRequested;

  const SellerFeeSummaryModel({
    required this.category,
    required this.monthlyFeeAmount,
    required this.unpaidFeesTotal,
    required this.availableBalance,
    required this.estimatedNetPayout,
    this.pendingCategoryChangeRequested,
  });

  String get categoryLabel => category == 'HIGH_PRICE' ? 'Prix élevés' : 'Standard';

  factory SellerFeeSummaryModel.fromJson(Map<String, dynamic> json) {
    return SellerFeeSummaryModel(
      category: json['category'] as String,
      monthlyFeeAmount: json['monthlyFeeAmount'] as num,
      unpaidFeesTotal: json['unpaidFeesTotal'] as num,
      availableBalance: json['availableBalance'] as num,
      estimatedNetPayout: json['estimatedNetPayout'] as num,
      pendingCategoryChangeRequested: json['pendingCategoryChangeRequested'] as String?,
    );
  }
}

/// Un frais mensuel donné (un par mois d'activité) — miroir de
/// `SellerMonthlyFeeResponse`.
class SellerMonthlyFeeModel {
  final String id;
  final DateTime billingMonth;
  final String category;
  final num amount;
  final num amountPaid;
  final String status;

  const SellerMonthlyFeeModel({
    required this.id,
    required this.billingMonth,
    required this.category,
    required this.amount,
    required this.amountPaid,
    required this.status,
  });

  factory SellerMonthlyFeeModel.fromJson(Map<String, dynamic> json) {
    return SellerMonthlyFeeModel(
      id: json['id'] as String,
      billingMonth: DateTime.parse(json['billingMonth'] as String),
      category: json['category'] as String,
      amount: json['amount'] as num,
      amountPaid: json['amountPaid'] as num,
      status: json['status'] as String,
    );
  }
}
