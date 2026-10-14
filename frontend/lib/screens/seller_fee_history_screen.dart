import 'package:flutter/material.dart';

import '../models/seller_fee_model.dart';
import '../services/api_client.dart';
import '../services/seller_fee_service.dart';
import '../theme/app_color_scheme.dart';
import '../utils/formatters.dart';

/// Historique des frais mensuels d'activité (un par mois : dû, partiellement
/// payé, payé ou annulé) — `GET /api/seller/fees/history`.
class SellerFeeHistoryScreen extends StatefulWidget {
  const SellerFeeHistoryScreen({super.key});

  @override
  State<SellerFeeHistoryScreen> createState() => _SellerFeeHistoryScreenState();
}

class _SellerFeeHistoryScreenState extends State<SellerFeeHistoryScreen> {
  late Future<List<SellerMonthlyFeeModel>> _future;

  static const _monthNames = [
    'Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin',
    'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre',
  ];

  @override
  void initState() {
    super.initState();
    _future = SellerFeeService().getFeeHistory(size: 100);
  }

  String _monthLabel(DateTime date) => '${_monthNames[date.month - 1]} ${date.year}';

  String _statusLabel(String status) {
    switch (status) {
      case 'PAID':
        return 'Payé';
      case 'PARTIALLY_PAID':
        return 'Partiellement payé';
      case 'CANCELLED':
        return 'Annulé';
      default:
        return 'En attente';
    }
  }

  Color _statusColor(AppColorScheme colors, String status) {
    switch (status) {
      case 'PAID':
        return colors.primary;
      case 'PARTIALLY_PAID':
        return colors.accentOrange;
      case 'CANCELLED':
        return colors.textMuted;
      default:
        return colors.error;
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
              child: FutureBuilder<List<SellerMonthlyFeeModel>>(
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
                            : "Impossible de charger l'historique.",
                        style: TextStyle(color: colors.textMuted),
                      ),
                    );
                  }
                  final items = snapshot.data ?? const [];
                  if (items.isEmpty) {
                    return Center(
                      child: Text('Aucun frais pour le moment.', style: TextStyle(color: colors.textMuted)),
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
            child: Text("Frais d'activité", textAlign: TextAlign.center, style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
          ),
          const SizedBox(width: 48),
        ],
      ),
    );
  }

  Widget _buildRow(AppColorScheme colors, SellerMonthlyFeeModel fee) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(14),
        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 8, offset: const Offset(0, 2))],
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(_monthLabel(fee.billingMonth), style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark)),
                Text(
                  fee.amountPaid > 0 && fee.amountPaid < fee.amount
                      ? '${formatFcfa(fee.amountPaid)} / ${formatFcfa(fee.amount)}'
                      : formatFcfa(fee.amount),
                  style: TextStyle(fontSize: 12, color: colors.textMuted),
                ),
              ],
            ),
          ),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
            decoration: BoxDecoration(
              color: _statusColor(colors, fee.status).withValues(alpha: 0.12),
              borderRadius: BorderRadius.circular(20),
            ),
            child: Text(
              _statusLabel(fee.status),
              style: TextStyle(fontSize: 11, fontWeight: FontWeight.w700, color: _statusColor(colors, fee.status)),
            ),
          ),
        ],
      ),
    );
  }
}
