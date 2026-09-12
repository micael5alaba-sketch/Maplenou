import '../models/vendor_order_model.dart';

/// Provides the seller's orders shown on [OrdersManagementScreen].
///
/// Mocked for now — no network call, but shaped like a real paginated,
/// filterable endpoint would be (`GET /api/shops/mine/orders?status=&page=`)
/// so swapping this for a real call later only touches this file.
class VendorOrdersService {
  static final List<VendorOrderModel> _allOrders = [
    VendorOrderModel(
      id: 'o1',
      orderNumber: '#CMD-8492',
      dateTime: DateTime(2023, 10, 24, 14, 30),
      customerName: 'Amina Diallo',
      productNames: const ['Sac en cuir'],
      status: VendorOrderStatus.toPrepare,
      amount: 25000,
    ),
    VendorOrderModel(
      id: 'o2',
      orderNumber: '#CMD-8493',
      dateTime: DateTime(2023, 10, 24, 10, 15),
      customerName: 'Koffi Mensah',
      productNames: const ['Tissu Wax', 'Bijoux dorés', 'Parfum'],
      status: VendorOrderStatus.shipping,
      amount: 47500,
    ),
    VendorOrderModel(
      id: 'o3',
      orderNumber: '#CMD-8488',
      dateTime: DateTime(2023, 10, 23, 18, 5),
      customerName: 'Sena Adjovi',
      productNames: const ['Chaussures'],
      status: VendorOrderStatus.delivered,
      amount: 32000,
    ),
    VendorOrderModel(
      id: 'o4',
      orderNumber: '#CMD-8481',
      dateTime: DateTime(2023, 10, 23, 9, 40),
      customerName: 'Yaovi Kokou',
      productNames: const ['Téléphone'],
      status: VendorOrderStatus.cancelled,
      amount: 125000,
    ),
    VendorOrderModel(
      id: 'o5',
      orderNumber: '#CMD-8479',
      dateTime: DateTime(2023, 10, 22, 16, 20),
      customerName: 'Akofa Mensah',
      productNames: const ['Bijoux dorés', 'Parfum'],
      status: VendorOrderStatus.toPrepare,
      amount: 18500,
    ),
    VendorOrderModel(
      id: 'o6',
      orderNumber: '#CMD-8475',
      dateTime: DateTime(2023, 10, 22, 11, 0),
      customerName: 'Fatou Traoré',
      productNames: const ['Tissu Wax'],
      status: VendorOrderStatus.shipping,
      amount: 22000,
    ),
    VendorOrderModel(
      id: 'o7',
      orderNumber: '#CMD-8470',
      dateTime: DateTime(2023, 10, 21, 13, 45),
      customerName: 'Kodjo Agbeko',
      productNames: const ['Sac en cuir', 'Chaussures'],
      status: VendorOrderStatus.delivered,
      amount: 58000,
    ),
    VendorOrderModel(
      id: 'o8',
      orderNumber: '#CMD-8466',
      dateTime: DateTime(2023, 10, 20, 8, 30),
      customerName: 'Ama Konan',
      productNames: const ['Parfum'],
      status: VendorOrderStatus.delivered,
      amount: 15500,
    ),
    VendorOrderModel(
      id: 'o9',
      orderNumber: '#CMD-8461',
      dateTime: DateTime(2023, 10, 19, 17, 10),
      customerName: 'Mawuli Dogbe',
      productNames: const ['Téléphone', 'Bijoux dorés'],
      status: VendorOrderStatus.toPrepare,
      amount: 138000,
    ),
    VendorOrderModel(
      id: 'o10',
      orderNumber: '#CMD-8455',
      dateTime: DateTime(2023, 10, 19, 12, 0),
      customerName: 'Adjoa Sena',
      productNames: const ['Chaussures'],
      status: VendorOrderStatus.cancelled,
      amount: 28000,
    ),
  ];

  static const int pageSize = 6;

  /// Simulates a paginated, filterable order list. [page] is 0-based.
  Future<List<VendorOrderModel>> getOrders({
    VendorOrderStatus? status,
    required int page,
  }) async {
    await Future.delayed(const Duration(milliseconds: 600));

    final filtered = status == null
        ? _allOrders
        : _allOrders.where((order) => order.status == status).toList();

    final start = page * pageSize;
    if (start >= filtered.length) return [];
    final end = (start + pageSize).clamp(0, filtered.length);
    return filtered.sublist(start, end);
  }
}
