/// One point on the sales evolution chart.
class SalesPoint {
  final String label;
  final double value;

  const SalesPoint({required this.label, required this.value});
}

/// Time range filters for [SalesChartWidget].
enum SalesChartRange { oneWeek, twoWeeks, fourWeeks }

extension SalesChartRangeLabel on SalesChartRange {
  String get label {
    switch (this) {
      case SalesChartRange.oneWeek:
        return '1 semaine';
      case SalesChartRange.twoWeeks:
        return '2 semaines';
      case SalesChartRange.fourWeeks:
        return '4 semaines';
    }
  }
}

/// The "Produit le plus vendu" card. `null` tant qu'aucune vente n'a encore
/// été enregistrée pour la boutique.
class BestSellingProductModel {
  final String name;
  final String? imageUrl;
  final int unitsSold;

  const BestSellingProductModel({
    required this.name,
    this.imageUrl,
    required this.unitsSold,
  });
}

/// Everything shown on [SellerDashboardScreen]. Miroir de `SellerKpiResponse`
/// (`seller/dto/SellerKpiResponse.java`).
class SellerDashboardModel {
  final String sellerName;

  final num weeklyRevenue;

  /// e.g. 8.4 meaning "+8.4%"; négatif pour une baisse ; `null` si aucune
  /// vente sur la période de référence (pas de variation calculable).
  final double? revenueChangePercent;

  final int ongoingOrdersCount;
  final int readyToShipCount;
  final num availableBalance;

  final Map<SalesChartRange, List<SalesPoint>> salesByRange;
  final BestSellingProductModel? bestSellingProduct;

  const SellerDashboardModel({
    required this.sellerName,
    required this.weeklyRevenue,
    this.revenueChangePercent,
    required this.ongoingOrdersCount,
    required this.readyToShipCount,
    required this.availableBalance,
    required this.salesByRange,
    this.bestSellingProduct,
  });
}
