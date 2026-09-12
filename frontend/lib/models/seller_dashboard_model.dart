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

/// One row of "Catégories les plus vendues".
class TopCategoryModel {
  final String name;
  final int productCount;

  /// 0-100.
  final double percent;

  const TopCategoryModel({
    required this.name,
    required this.productCount,
    required this.percent,
  });
}

/// The "Produit le plus vendu" card.
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

/// Everything shown on [SellerDashboardScreen].
class SellerDashboardModel {
  final String sellerName;

  final num weeklyRevenue;

  /// e.g. 8.4 meaning "+8.4%"; negative for a decline.
  final double revenueChangePercent;

  final int ongoingOrdersCount;
  final int readyToShipCount;
  final num availableBalance;

  final Map<SalesChartRange, List<SalesPoint>> salesByRange;
  final List<TopCategoryModel> topCategories;
  final BestSellingProductModel bestSellingProduct;

  const SellerDashboardModel({
    required this.sellerName,
    required this.weeklyRevenue,
    required this.revenueChangePercent,
    required this.ongoingOrdersCount,
    required this.readyToShipCount,
    required this.availableBalance,
    required this.salesByRange,
    required this.topCategories,
    required this.bestSellingProduct,
  });
}
