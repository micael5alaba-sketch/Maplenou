import 'package:flutter/material.dart';

import '../models/wallet_model.dart';
import '../services/wallet_service.dart';
import '../theme/app_color_scheme.dart';
import '../widgets/payment_method_tile.dart';
import '../widgets/wallet_balance_card.dart';
import '../widgets/wallet_stats_grid.dart';
import '../widgets/withdrawal_history_section.dart';

/// Seller's wallet: available balance, registered payout methods, quick
/// stats and withdrawal history.
///
/// Reached from [SellerProfileScreen] — pushed on top, so it gets a back
/// arrow instead of the hamburger menu.
///
/// Backed by [WalletService] mocked data for now — no network call yet.
/// Swapping it for real calls later only touches that service: balance and
/// withdrawal history from `GET /api/seller/payouts`, payment methods from
/// the now-available `GET/POST/PATCH/DELETE /api/seller/payout-methods`.
class WalletScreen extends StatelessWidget {
  const WalletScreen({super.key});

  void _showComingSoon(BuildContext context, String feature) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text('$feature bientôt disponible.')));
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    final wallet = WalletService().getWallet();

    return Scaffold(
      backgroundColor: colors.homeBackground,
      body: SafeArea(
        child: Column(
          children: [
            _buildHeader(context),
            Expanded(
              child: ListView(
                padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
                children: [
                  WalletBalanceCard(
                    availableBalance: wallet.availableBalance,
                    changePercent: wallet.changePercent,
                    onWithdraw: () => _showComingSoon(context, 'La demande de retrait'),
                  ),
                  const SizedBox(height: 16),
                  _buildPaymentMethodsSection(context, wallet),
                  const SizedBox(height: 16),
                  WalletStatsGrid(
                    totalEarnedMonth: wallet.totalEarnedMonth,
                    salesCountMonth: wallet.salesCountMonth,
                    pendingAmount: wallet.pendingAmount,
                    averageRating: wallet.averageRating,
                  ),
                  const SizedBox(height: 16),
                  WithdrawalHistorySection(
                    withdrawals: wallet.withdrawals,
                    onSeeAll: () => _showComingSoon(context, "L'historique complet des retraits"),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader(BuildContext context) {
    final colors = context.colors;

    return Container(
      color: colors.surface,
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      child: Row(
        children: [
          IconButton(
            icon: Icon(Icons.arrow_back_rounded, color: colors.textDark),
            onPressed: () => Navigator.of(context).pop(),
          ),
          Expanded(
            child: Text(
              'Mon Portefeuille',
              style: TextStyle(fontSize: 17, fontWeight: FontWeight.bold, color: colors.textDark),
            ),
          ),
          IconButton(
            icon: Icon(Icons.notifications_none_rounded, color: colors.textDark),
            onPressed: () => _showComingSoon(context, 'Notifications'),
          ),
        ],
      ),
    );
  }

  Widget _buildPaymentMethodsSection(BuildContext context, WalletModel wallet) {
    final colors = context.colors;

    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 12, offset: const Offset(0, 4)),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text('Moyens de paiement', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: colors.textDark)),
              IconButton(
                icon: Icon(Icons.add_circle_outline_rounded, color: colors.primary),
                onPressed: () => _showComingSoon(context, "L'ajout d'un moyen de paiement"),
              ),
            ],
          ),
          for (final method in wallet.paymentMethods) ...[
            const SizedBox(height: 8),
            PaymentMethodTile(method: method),
          ],
        ],
      ),
    );
  }
}
