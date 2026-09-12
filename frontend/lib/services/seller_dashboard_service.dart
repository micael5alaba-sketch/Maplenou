import '../models/seller_dashboard_model.dart';

/// Provides the data shown on [SellerDashboardScreen].
///
/// Mocked for now — no network call. Replace [getDashboard] with real
/// Spring Boot endpoints once they exist (seller orders, payouts, sales
/// analytics...); the return shape stays the same.
class SellerDashboardService {
  SellerDashboardModel getDashboard() {
    return SellerDashboardModel(
      sellerName: 'Koffi Mensah',
      weeklyRevenue: 285000,
      revenueChangePercent: 8.4,
      ongoingOrdersCount: 12,
      readyToShipCount: 4,
      availableBalance: 124500,
      salesByRange: {
        SalesChartRange.oneWeek: const [
          SalesPoint(label: 'Lun', value: 32000),
          SalesPoint(label: 'Mar', value: 41000),
          SalesPoint(label: 'Mer', value: 28000),
          SalesPoint(label: 'Jeu', value: 47000),
          SalesPoint(label: 'Ven', value: 52000),
          SalesPoint(label: 'Sam', value: 61000),
          SalesPoint(label: 'Dim', value: 24000),
        ],
        SalesChartRange.twoWeeks: const [
          SalesPoint(label: 'S1-Lun', value: 30000),
          SalesPoint(label: 'S1-Mar', value: 35000),
          SalesPoint(label: 'S1-Mer', value: 26000),
          SalesPoint(label: 'S1-Jeu', value: 40000),
          SalesPoint(label: 'S1-Ven', value: 45000),
          SalesPoint(label: 'S1-Sam', value: 50000),
          SalesPoint(label: 'S1-Dim', value: 20000),
          SalesPoint(label: 'Lun', value: 32000),
          SalesPoint(label: 'Mar', value: 41000),
          SalesPoint(label: 'Mer', value: 28000),
          SalesPoint(label: 'Jeu', value: 47000),
          SalesPoint(label: 'Ven', value: 52000),
          SalesPoint(label: 'Sam', value: 61000),
          SalesPoint(label: 'Dim', value: 24000),
        ],
        // Aggregated per week rather than per day — 28 daily points would
        // be unreadable on a phone-width chart.
        SalesChartRange.fourWeeks: const [
          SalesPoint(label: 'S1', value: 210000),
          SalesPoint(label: 'S2', value: 248000),
          SalesPoint(label: 'S3', value: 231000),
          SalesPoint(label: 'S4', value: 285000),
        ],
      },
      topCategories: const [
        TopCategoryModel(name: 'Sculptures en bois', productCount: 18, percent: 45),
        TopCategoryModel(name: 'Textiles traditionnels', productCount: 12, percent: 30),
        TopCategoryModel(name: 'Bijoux artisanaux', productCount: 7, percent: 15),
      ],
      bestSellingProduct: const BestSellingProductModel(
        name: 'Masque Baoulé Authentique',
        unitsSold: 42,
      ),
    );
  }
}
