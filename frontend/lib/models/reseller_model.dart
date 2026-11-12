/// Modèles du programme partenaire revendeur (`reseller/dto/ResellerDtos.java`). Montants en FCFA entiers.
class ResellerProfileModel {
  final String id;
  final String status;
  final String? statusReason;
  final DateTime createdAt;

  const ResellerProfileModel({required this.id, required this.status, this.statusReason, required this.createdAt});

  factory ResellerProfileModel.fromJson(Map<String, dynamic> json) => ResellerProfileModel(
        id: json['id'] as String,
        status: json['status'] as String,
        statusReason: json['statusReason'] as String?,
        createdAt: DateTime.parse(json['createdAt'] as String),
      );

  bool get isActive => status == 'APPROVED';
}

class ResellerSettingsModel {
  final double shareRate;
  final int minUnitPrice;
  final int attributionDays;
  final int minWithdrawal;
  final double withdrawalFeeRate;
  final bool programActive;

  const ResellerSettingsModel({
    required this.shareRate,
    required this.minUnitPrice,
    required this.attributionDays,
    required this.minWithdrawal,
    required this.withdrawalFeeRate,
    required this.programActive,
  });

  factory ResellerSettingsModel.fromJson(Map<String, dynamic> json) => ResellerSettingsModel(
        shareRate: (json['shareRate'] as num).toDouble(),
        minUnitPrice: (json['minUnitPrice'] as num).toInt(),
        attributionDays: (json['attributionDays'] as num).toInt(),
        minWithdrawal: (json['minWithdrawal'] as num).toInt(),
        withdrawalFeeRate: (json['withdrawalFeeRate'] as num).toDouble(),
        programActive: json['programActive'] as bool,
      );
}

class ResellerCatalogItem {
  final String productId;
  final String name;
  final String shopName;
  final String? imageUrl;
  final int unitPrice;
  final int partnerShare;
  final String? linkUrl;
  final int clicks;
  final int unitsSold;

  const ResellerCatalogItem({
    required this.productId,
    required this.name,
    required this.shopName,
    required this.imageUrl,
    required this.unitPrice,
    required this.partnerShare,
    required this.linkUrl,
    this.clicks = 0,
    this.unitsSold = 0,
  });

  factory ResellerCatalogItem.fromJson(Map<String, dynamic> json) => ResellerCatalogItem(
        productId: json['productId'] as String,
        name: json['name'] as String,
        shopName: json['shopName'] as String? ?? '',
        imageUrl: json['imageUrl'] as String?,
        unitPrice: (json['unitPrice'] as num).toInt(),
        partnerShare: (json['partnerShare'] as num).toInt(),
        linkUrl: json['linkUrl'] as String?,
        clicks: (json['clicks'] as num?)?.toInt() ?? 0,
        unitsSold: (json['unitsSold'] as num?)?.toInt() ?? 0,
      );
}

class ResellerCommissionModel {
  final String productName;
  final int unitPrice;
  final int partnerShare;
  final String status;
  final DateTime createdAt;

  const ResellerCommissionModel({
    required this.productName,
    required this.unitPrice,
    required this.partnerShare,
    required this.status,
    required this.createdAt,
  });

  factory ResellerCommissionModel.fromJson(Map<String, dynamic> json) => ResellerCommissionModel(
        productName: json['productName'] as String,
        unitPrice: (json['unitPrice'] as num).toInt(),
        partnerShare: (json['partnerShare'] as num).toInt(),
        status: json['status'] as String,
        createdAt: DateTime.parse(json['createdAt'] as String),
      );

  String get statusLabel => switch (status) {
        'PENDING' => 'En attente de livraison',
        'CREDITED' => 'Créditée',
        'CANCELLED' => 'Annulée',
        _ => status,
      };
}

class ResellerWalletModel {
  final int available;
  final int reserved;
  final int pending;
  final int totalEarned;
  final int totalPaid;
  final int totalFees;
  final int clicks;

  const ResellerWalletModel({
    required this.available,
    required this.reserved,
    required this.pending,
    required this.totalEarned,
    required this.totalPaid,
    required this.totalFees,
    required this.clicks,
  });

  factory ResellerWalletModel.fromJson(Map<String, dynamic> json) => ResellerWalletModel(
        available: (json['available'] as num).toInt(),
        reserved: (json['reserved'] as num).toInt(),
        pending: (json['pending'] as num).toInt(),
        totalEarned: (json['totalEarned'] as num).toInt(),
        totalPaid: (json['totalPaid'] as num).toInt(),
        totalFees: (json['totalFees'] as num).toInt(),
        clicks: (json['clicks'] as num).toInt(),
      );
}

class ResellerWithdrawalModel {
  final int amount;
  final int feeAmount;
  final int netAmount;
  final String method;
  final String status;
  final DateTime createdAt;

  const ResellerWithdrawalModel({
    required this.amount,
    required this.feeAmount,
    required this.netAmount,
    required this.method,
    required this.status,
    required this.createdAt,
  });

  factory ResellerWithdrawalModel.fromJson(Map<String, dynamic> json) => ResellerWithdrawalModel(
        amount: (json['amount'] as num).toInt(),
        feeAmount: (json['feeAmount'] as num).toInt(),
        netAmount: (json['netAmount'] as num).toInt(),
        method: json['method'] as String,
        status: json['status'] as String,
        createdAt: DateTime.parse(json['createdAt'] as String),
      );

  String get methodLabel => method == 'MIX_BY_YAS' ? 'Mix by Yas' : 'Moov Africa';

  String get statusLabel => switch (status) {
        'PENDING' => 'Reçu',
        'PREPARED' => 'En vérification',
        'SUBMITTED' => "En attente d'approbation",
        'NEEDS_CORRECTION' => 'À corriger',
        'APPROVED' => 'Approuvé',
        'PROCESSING' => 'Virement en cours',
        'PAID' => 'Payé',
        'REJECTED' => 'Rejeté',
        'FAILED' => 'Échec du virement',
        _ => status,
      };
}

/// Statistiques du partenaire sur une période (`GET /api/reseller/stats`).
class ResellerStatsModel {
  final int clicks;
  final int attributedSales;
  final int unitsSold;
  final double conversionRate;

  const ResellerStatsModel({
    required this.clicks,
    required this.attributedSales,
    required this.unitsSold,
    required this.conversionRate,
  });

  factory ResellerStatsModel.fromJson(Map<String, dynamic> json) => ResellerStatsModel(
        clicks: (json['clicks'] as num).toInt(),
        attributedSales: (json['attributedSales'] as num).toInt(),
        unitsSold: (json['unitsSold'] as num).toInt(),
        conversionRate: (json['conversionRate'] as num).toDouble(),
      );
}
