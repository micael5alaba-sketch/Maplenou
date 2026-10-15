import 'dart:io';

import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';
import 'package:share_plus/share_plus.dart';

import '../models/seller_fee_model.dart';
import '../models/wallet_model.dart';
import '../services/api_client.dart';
import '../services/payout_service.dart';
import '../services/seller_fee_service.dart';
import '../services/wallet_service.dart';
import '../theme/app_color_scheme.dart';
import '../utils/formatters.dart';
import '../utils/payout_invoice.dart';
import '../widgets/payment_method_tile.dart';
import '../widgets/wallet_balance_card.dart';
import '../widgets/wallet_stats_grid.dart';
import '../widgets/withdrawal_history_section.dart';
import 'notifications_screen.dart';
import 'payout_history_screen.dart';
import 'seller_fee_history_screen.dart';

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
  late Future<SellerFeeSummaryModel> _feeSummaryFuture;

  @override
  void initState() {
    super.initState();
    _walletFuture = WalletService().getWallet();
    _feeSummaryFuture = SellerFeeService().getSummary();
  }

  /// `POST /api/seller/payouts/withdraw` — retrait à la demande, sans attendre le
  /// reversement hebdomadaire ni passer par une validation admin (voir PayoutService.java).
  Future<void> _handleWithdraw(num availableBalance) async {
    final colors = context.colors;

    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        backgroundColor: colors.surface,
        title: const Text('Demander un retrait'),
        content: Text('Retirer ${formatFcfa(availableBalance)} ? Les frais d\'activité non payés seront déduits.'),
        actions: [
          TextButton(onPressed: () => Navigator.of(dialogContext).pop(false), child: const Text('Annuler')),
          TextButton(onPressed: () => Navigator.of(dialogContext).pop(true), child: const Text('Confirmer')),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;

    try {
      await PayoutService().requestWithdrawal();
      if (!mounted) return;
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(const SnackBar(content: Text('Retrait demandé — en cours de traitement.')));
      _reload();
    } on ApiException catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(SnackBar(content: Text(e.message)));
    }
  }

  void _reload() {
    setState(() {
      _walletFuture = WalletService().getWallet();
      _feeSummaryFuture = SellerFeeService().getSummary();
    });
  }

  Future<void> _requestCategoryChange(String currentCategory) async {
    String selected = currentCategory == 'HIGH_PRICE' ? 'STANDARD' : 'HIGH_PRICE';
    final colors = context.colors;

    final confirmed = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      backgroundColor: colors.surface,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (sheetContext) {
        return StatefulBuilder(
          builder: (sheetContext, setSheetState) {
            return Padding(
              padding: EdgeInsets.only(bottom: MediaQuery.of(sheetContext).viewInsets.bottom),
              child: SafeArea(
                child: Padding(
                  padding: const EdgeInsets.all(20),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('Changer de catégorie', style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
                      const SizedBox(height: 4),
                      Text(
                        "Effectif au 1er jour du mois suivant l'approbation par l'équipe Maplenou.",
                        style: TextStyle(fontSize: 12, color: colors.textMuted),
                      ),
                      const SizedBox(height: 16),
                      RadioListTile<String>(
                        value: 'STANDARD',
                        groupValue: selected,
                        title: const Text('Standard — 500 FCFA/mois'),
                        onChanged: (v) => setSheetState(() => selected = v!),
                      ),
                      RadioListTile<String>(
                        value: 'HIGH_PRICE',
                        groupValue: selected,
                        title: const Text('Prix élevés — 2 000 FCFA/mois'),
                        onChanged: (v) => setSheetState(() => selected = v!),
                      ),
                      const SizedBox(height: 12),
                      SizedBox(
                        width: double.infinity,
                        child: ElevatedButton(
                          style: ElevatedButton.styleFrom(backgroundColor: colors.primary, foregroundColor: Colors.white),
                          onPressed: () => Navigator.of(sheetContext).pop(true),
                          child: const Text('Envoyer la demande'),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            );
          },
        );
      },
    );

    if (confirmed != true || !mounted) return;
    try {
      await SellerFeeService().requestCategoryChange(selected);
      _reload();
      if (!mounted) return;
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(const SnackBar(content: Text('Demande envoyée — en attente de validation.')));
    } on ApiException catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(SnackBar(content: Text(e.message)));
    }
  }

  Future<void> _addPaymentMethod() async {
    const typeLabels = {'T_MONEY': 'T-Money', 'FLOOZ': 'Flooz', 'BANK_TRANSFER': 'Virement bancaire'};
    String selectedType = 'T_MONEY';
    final accountController = TextEditingController();
    final colors = context.colors;

    final confirmed = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      backgroundColor: colors.surface,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (sheetContext) {
        return StatefulBuilder(
          builder: (sheetContext, setSheetState) {
            return Padding(
              padding: EdgeInsets.only(bottom: MediaQuery.of(sheetContext).viewInsets.bottom),
              child: SafeArea(
                child: Padding(
                  padding: const EdgeInsets.all(20),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('Nouveau moyen de paiement', style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
                      const SizedBox(height: 16),
                      DropdownButtonFormField<String>(
                        initialValue: selectedType,
                        decoration: const InputDecoration(labelText: 'Type'),
                        items: [
                          for (final entry in typeLabels.entries) DropdownMenuItem(value: entry.key, child: Text(entry.value)),
                        ],
                        onChanged: (value) => setSheetState(() => selectedType = value!),
                      ),
                      const SizedBox(height: 12),
                      TextField(
                        controller: accountController,
                        keyboardType: TextInputType.phone,
                        decoration: const InputDecoration(labelText: 'Numéro de compte / téléphone'),
                      ),
                      const SizedBox(height: 20),
                      SizedBox(
                        width: double.infinity,
                        child: ElevatedButton(
                          style: ElevatedButton.styleFrom(backgroundColor: colors.primary, foregroundColor: Colors.white),
                          onPressed: () => Navigator.of(sheetContext).pop(true),
                          child: const Text('Enregistrer'),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            );
          },
        );
      },
    );

    if (confirmed != true || accountController.text.trim().isEmpty || !mounted) return;
    try {
      await PayoutService().addPayoutMethod(type: selectedType, accountNumber: accountController.text.trim());
      _reload();
    } on ApiException catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(SnackBar(content: Text(e.message)));
    }
  }

  bool _isExportingInvoice = false;

  Future<void> _downloadInvoice(WithdrawalModel withdrawal) async {
    setState(() => _isExportingInvoice = true);
    try {
      final detail = await PayoutService().getPayoutDetail(withdrawal.id);
      final bytes = await buildPayoutInvoicePdf(detail);
      final dir = await getTemporaryDirectory();
      final file = File('${dir.path}/facture-reversement-${detail.id.substring(0, 8)}.pdf');
      await file.writeAsBytes(bytes);
      await Share.shareXFiles([XFile(file.path)], text: 'Facture reversement');
    } on ApiException catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(SnackBar(content: Text(e.message)));
    } finally {
      if (mounted) setState(() => _isExportingInvoice = false);
    }
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
          onWithdraw: () => _handleWithdraw(wallet.availableBalance),
        ),
        const SizedBox(height: 16),
        _buildFeeSummaryCard(),
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
          onSeeAll: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const PayoutHistoryScreen())),
          onDownloadInvoice: _isExportingInvoice ? (_) {} : _downloadInvoice,
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
            onPressed: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const NotificationsScreen())),
          ),
        ],
      ),
    );
  }

  Widget _buildFeeSummaryCard() {
    final colors = context.colors;

    return FutureBuilder<SellerFeeSummaryModel>(
      future: _feeSummaryFuture,
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const SizedBox.shrink();
        }
        if (snapshot.hasError || !snapshot.hasData) {
          return const SizedBox.shrink();
        }
        final fee = snapshot.data!;

        return Container(
          padding: const EdgeInsets.all(18),
          decoration: BoxDecoration(
            color: colors.surface,
            borderRadius: BorderRadius.circular(20),
            boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 12, offset: const Offset(0, 4))],
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text('Frais d\'activité', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: colors.textDark)),
                  GestureDetector(
                    onTap: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const SellerFeeHistoryScreen())),
                    child: Text('Historique', style: TextStyle(color: colors.primary, fontWeight: FontWeight.w600, fontSize: 13)),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('Catégorie', style: TextStyle(fontSize: 11, color: colors.textMuted)),
                        Text(fee.categoryLabel, style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark)),
                        Text('${formatFcfa(fee.monthlyFeeAmount)}/mois', style: TextStyle(fontSize: 12, color: colors.textMuted)),
                      ],
                    ),
                  ),
                  TextButton(
                    onPressed: fee.pendingCategoryChangeRequested != null ? null : () => _requestCategoryChange(fee.category),
                    child: Text(
                      fee.pendingCategoryChangeRequested != null ? 'Demande en attente' : 'Changer',
                      style: const TextStyle(fontSize: 12),
                    ),
                  ),
                ],
              ),
              if (fee.unpaidFeesTotal > 0) ...[
                const Divider(height: 20),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text('Frais à recouvrer', style: TextStyle(fontSize: 13, color: colors.textMuted)),
                    Text(formatFcfa(fee.unpaidFeesTotal), style: TextStyle(fontWeight: FontWeight.w600, color: colors.error)),
                  ],
                ),
                const SizedBox(height: 4),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text('Net estimé au prochain reversement', style: TextStyle(fontSize: 13, color: colors.textMuted)),
                    Text(formatFcfa(fee.estimatedNetPayout), style: TextStyle(fontWeight: FontWeight.bold, color: colors.primary)),
                  ],
                ),
              ],
            ],
          ),
        );
      },
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
                onPressed: _addPaymentMethod,
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
