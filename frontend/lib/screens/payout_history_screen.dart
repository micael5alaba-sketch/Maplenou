import 'dart:io';

import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';
import 'package:share_plus/share_plus.dart';

import '../models/wallet_model.dart';
import '../services/api_client.dart';
import '../services/payout_service.dart';
import '../theme/app_color_scheme.dart';
import '../utils/formatters.dart';
import '../utils/payout_invoice.dart';

/// Historique complet des reversements vendeur (pas seulement les derniers
/// affichés sur [WalletScreen]) — `GET /api/seller/payouts`.
class PayoutHistoryScreen extends StatefulWidget {
  const PayoutHistoryScreen({super.key});

  @override
  State<PayoutHistoryScreen> createState() => _PayoutHistoryScreenState();
}

class _PayoutHistoryScreenState extends State<PayoutHistoryScreen> {
  late Future<List<WithdrawalModel>> _future;
  String? _downloadingId;

  @override
  void initState() {
    super.initState();
    _future = PayoutService().listMyPayouts(size: 200);
  }

  Future<void> _downloadInvoice(WithdrawalModel withdrawal) async {
    setState(() => _downloadingId = withdrawal.id);
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
      if (mounted) setState(() => _downloadingId = null);
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
            _buildHeader(context, colors),
            Expanded(
              child: FutureBuilder<List<WithdrawalModel>>(
                future: _future,
                builder: (context, snapshot) {
                  if (snapshot.connectionState == ConnectionState.waiting) {
                    return const Center(child: CircularProgressIndicator());
                  }
                  if (snapshot.hasError) {
                    return Center(
                      child: Text(
                        snapshot.error is ApiException
                            ? (snapshot.error as ApiException).message
                            : 'Impossible de charger l\'historique.',
                        style: TextStyle(color: colors.textMuted),
                      ),
                    );
                  }
                  final items = snapshot.data ?? const [];
                  if (items.isEmpty) {
                    return Center(
                      child: Text('Aucun reversement pour le moment.', style: TextStyle(color: colors.textMuted)),
                    );
                  }
                  return ListView.separated(
                    padding: const EdgeInsets.all(16),
                    itemCount: items.length,
                    separatorBuilder: (_, _) => const SizedBox(height: 10),
                    itemBuilder: (context, i) => _buildRow(colors, items[i]),
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader(BuildContext context, AppColorScheme colors) {
    return Container(
      color: colors.surface,
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
      child: Row(
        children: [
          IconButton(
            icon: Icon(Icons.arrow_back_rounded, color: colors.textDark),
            onPressed: () => Navigator.of(context).pop(),
          ),
          Expanded(
            child: Text('Historique des retraits', textAlign: TextAlign.center, style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
          ),
          const SizedBox(width: 48),
        ],
      ),
    );
  }

  Widget _buildRow(AppColorScheme colors, WithdrawalModel withdrawal) {
    final isPending = withdrawal.status == WithdrawalStatus.pending;
    final isDownloading = _downloadingId == withdrawal.id;

    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(14),
        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 8, offset: const Offset(0, 2))],
      ),
      child: Row(
        children: [
          Icon(
            isPending ? Icons.access_time_rounded : Icons.check_circle_rounded,
            size: 20,
            color: isPending ? colors.accentOrange : colors.primary,
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(formatFcfa(withdrawal.amount), style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
                Text(
                  '${formatOrderDateTime(withdrawal.date).split(' • ').first} • ${withdrawal.method}',
                  style: TextStyle(fontSize: 12, color: colors.textMuted),
                ),
              ],
            ),
          ),
          isDownloading
              ? SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: colors.primary))
              : IconButton(
                  icon: Icon(Icons.download_outlined, size: 20, color: colors.textMuted),
                  tooltip: 'Télécharger la facture',
                  onPressed: () => _downloadInvoice(withdrawal),
                ),
        ],
      ),
    );
  }
}
