import 'package:flutter/material.dart';

import '../models/wallet_model.dart';
import '../services/api_client.dart';
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
/// Backed by [WalletService] (`GET /api/sellers/me/kpis`,
/// `GET /api/seller/payouts`, `GET /api/seller/payout-methods`).
class WalletScreen extends StatefulWidget {
  const WalletScreen({super.key});

  @override
  State<WalletScreen> createState() => _WalletScreenState();
}

class _WalletScreenState extends State<WalletScreen> {
  late Future<WalletModel> _walletFuture;

  @override
  void initState() {
    super.initState();
    _walletFuture = WalletService().getWallet();
  }

  void _showComingSoon(String feature) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text('$feature bientôt disponible.')));
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return Scaffold(
      backgroundColor: colors.homeBackground,
      body: SafeArea(
        child: Column(
          children: [
            _buildHeader(),
            Expanded(
              child: FutureBuilder<WalletModel>(
                future: _walletFuture,
                builder: (context, snapshot) {
                  if (snapshot.connectionState == ConnectionState.waiting) {
                    return const Center(child: CircularProgressIndicator());
                  }
                  if (snapshot.hasError || !snapshot.hasData) {
                    return _buildErrorState(colors, snapshot.error);
                  }
                  return _buildContent(snapshot.data!);
                },
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildErrorState(AppColorScheme colors, Object? error) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.error_outline_rounded, size: 48, color: colors.error),
            const SizedBox(height: 12),
            Text(
              error is ApiException ? error.message : 'Impossible de charger le portefeuille.',
              textAlign: TextAlign.center,
              style: TextStyle(color: colors.textMuted),
            ),
            const SizedBox(height: 12),
            TextButton(
              onPressed: () => setState(() => _walletFuture = WalletService().getWallet()),
              child: const Text('Réessayer'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildContent(WalletModel wallet) {
    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
      children: [
        WalletBalanceCard(
          availableBalance: wallet.availableBalance,
          onWithdraw: () => _showComingSoon('La demande de retrait'),
        ),
        const SizedBox(height: 16),
        _buildPaymentMethodsSection(wallet),
        const SizedBox(height: 16),
        WalletStatsGrid(
          totalPaidOut: wallet.totalPaidOut,
          paidPayoutsCount: wallet.paidPayoutsCount,
          pendingAmount: wallet.pendingAmount,
          averageRating: wallet.averageRating,
        ),
        const SizedBox(height: 16),
        WithdrawalHistorySection(
          withdrawals: wallet.withdrawals,
          onSeeAll: () => _showComingSoon("L'historique complet des retraits"),
        ),
      ],
    );
  }

  Widget _buildHeader() {
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
            onPressed: () => _showComingSoon('Notifications'),
          ),
        ],
      ),
    );
  }

  Widget _buildPaymentMethodsSection(WalletModel wallet) {
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
                onPressed: () => _showComingSoon("L'ajout d'un moyen de paiement"),
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
