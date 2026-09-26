import '../models/seller_dashboard_model.dart';
import '../models/user_model.dart';
import 'api_client.dart';
import 'user_service.dart';

/// `GET /api/sellers/me/kpis` (`seller/SellerKpiController.java`), plus le
/// nom du compte (`GET /api/users/me`) pour la salutation.
class SellerDashboardService {
  final _client = ApiClient();
  final _userService = UserService();

  Future<SellerDashboardModel> getDashboard() async {
    final kpiData = await _client.get('/api/sellers/me/kpis') as Map<String, dynamic>;
    final UserModel user = await _userService.getCurrentUser();

    final bestSellingJson = kpiData['bestSellingProduct'] as Map<String, dynamic>?;

    return SellerDashboardModel(
      sellerName: user.fullName,
      weeklyRevenue: kpiData['revenueLast7Days'] as num,
      revenueChangePercent: (kpiData['revenueChangePercent'] as num?)?.toDouble(),
      ongoingOrdersCount: kpiData['ongoingOrdersCount'] as int,
      readyToShipCount: kpiData['readyToShipCount'] as int,
      availableBalance: kpiData['availableBalance'] as num,
      salesByRange: {
        SalesChartRange.oneWeek: _parseSeries(kpiData['salesLast7Days'] as List<dynamic>),
        SalesChartRange.twoWeeks: _parseSeries(kpiData['salesLast14Days'] as List<dynamic>),
        SalesChartRange.fourWeeks: _parseSeries(kpiData['salesLast28Days'] as List<dynamic>),
      },
      bestSellingProduct: bestSellingJson == null
          ? null
          : BestSellingProductModel(
              name: bestSellingJson['productName'] as String,
              unitsSold: bestSellingJson['unitsSold'] as int,
            ),
    );
  }

  List<SalesPoint> _parseSeries(List<dynamic> points) {
    return points.map((json) {
      final point = json as Map<String, dynamic>;
      final date = DateTime.parse(point['date'] as String);
      return SalesPoint(label: '${date.day}/${date.month}', value: (point['amount'] as num).toDouble());
    }).toList();
  }
}
