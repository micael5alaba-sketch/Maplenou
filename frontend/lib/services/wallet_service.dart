import '../models/wallet_model.dart';

/// Provides the balance, payment methods and withdrawal history shown on
/// [WalletScreen].
///
/// Mocked for now — no network call. Replace [getWallet] with real calls
/// once auth is wired up: `GET /api/seller/payouts` for the balance and
/// withdrawal history, `GET /api/seller/payout-methods` for the registered
/// T-Money/Flooz/bank accounts.
class WalletService {
  WalletModel getWallet() {
    return WalletModel(
      availableBalance: 450000,
      changePercent: 12.5,
      paymentMethods: const [
        PaymentMethodModel(name: 'T-Money', maskedNumber: '*** *** 89', isDefault: true),
        PaymentMethodModel(name: 'Flooz', maskedNumber: '*** *** 42'),
      ],
      totalEarnedMonth: 185000,
      salesCountMonth: 42,
      pendingAmount: 25000,
      averageRating: 4.8,
      withdrawals: [
        WithdrawalModel(
          date: DateTime(2023, 10, 12),
          amount: 50000,
          method: 'T-Money',
          status: WithdrawalStatus.pending,
        ),
        WithdrawalModel(
          date: DateTime(2023, 9, 28),
          amount: 120000,
          method: 'Virement (Stripe)',
          status: WithdrawalStatus.completed,
        ),
        WithdrawalModel(
          date: DateTime(2023, 9, 5),
          amount: 75000,
          method: 'Flooz',
          status: WithdrawalStatus.completed,
        ),
      ],
    );
  }
}
