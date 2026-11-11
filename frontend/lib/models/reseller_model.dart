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
  final int tierThreshold;
  final double rateLow;
  final double rateHigh;
  final int attributionDays;
  final int minWithdrawal;
  final double withdrawalFeeRate;
  final bool programActive;

  const ResellerSettingsModel({
    required this.shareRate,
    required this.minUnitPrice,
    required this.tierThreshold,
    required this.rateLow,
    required this.rateHigh,
    required this.attributionDays,
    required this.minWithdrawal,
    required this.withdrawalFeeRate,
    required this.programActive,
  });

  factory ResellerSettingsModel.fromJson(Map<String, dynamic> json) => ResellerSettingsModel(
        shareRate: (json['shareRate'] as num).toDouble(),
        minUnitPrice: (json['minUnitPrice'] as num).toInt(),
        tierThreshold: (json['tierThreshold'] as num).toInt(),
        rateLow: (json['rateLow'] as num).toDouble(),
        rateHigh: (json['rateHigh'] as num).toDouble(),
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

  const ResellerCatalogItem({
    required this.productId,
    required this.name,
    required this.shopName,
    required this.imageUrl,
    required this.unitPrice,
    required this.partnerShare,
    required this.linkUrl,
  });

  factory ResellerCatalogItem.fromJson(Map<String, dynamic> json) => ResellerCatalogItem(
        productId: json['productId'] as String,
        name: json['name'] as String,
        shopName: json['shopName'] as String? ?? '',
        imageUrl: json['imageUrl'] as String?,
        unitPrice: (json['unitPrice'] as num).toInt(),
        partnerShare: (json['partnerShare'] as num).toInt(),
        linkUrl: json['linkUrl'] as String?,
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
