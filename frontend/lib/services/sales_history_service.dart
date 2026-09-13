import '../models/sales_history_model.dart';

/// Provides the sales totals and transaction list shown on
/// [SalesHistoryScreen].
///
/// Mocked for now — no network call. Replace [getHistory] with a real
/// `GET /api/sub-orders?status=DELIVERED` call once wired up; the return
/// shape stays close (a transaction maps to one delivered `SubOrder`).
class SalesHistoryService {
  SalesHistoryModel getHistory() {
    return SalesHistoryModel(
      totalSales: 1250000,
      ordersProcessed: 124,
      transactions: [
        SaleTransactionModel(
          orderNumber: '#CMD-8492',
          customerName: 'Fatou Diop',
          itemsSummary: '1x Sac en cuir artisanal',
          dateTime: DateTime(2023, 10, 24, 14, 30),
          amount: 45000,
          status: SaleStatus.completed,
        ),
        SaleTransactionModel(
          orderNumber: '#CMD-8491',
          customerName: 'Kwame Mensah',
          itemsSummary: '2x Collier en perles',
          dateTime: DateTime(2023, 10, 24, 11, 15),
          amount: 12500,
          status: SaleStatus.completed,
        ),
        SaleTransactionModel(
          orderNumber: '#CMD-8490',
          customerName: 'Amina Kone',
          itemsSummary: '1x Vase en argile',
          dateTime: DateTime(2023, 10, 23, 9, 45),
          amount: 28000,
          status: SaleStatus.cancelled,
        ),
        SaleTransactionModel(
          orderNumber: '#CMD-8489',
          customerName: 'Oumar Sy',
          itemsSummary: '3x Tissu Batik',
          dateTime: DateTime(2023, 10, 22, 16, 20),
          amount: 55000,
          status: SaleStatus.completed,
        ),
      ],
    );
  }
}
