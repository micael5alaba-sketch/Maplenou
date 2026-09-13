import 'package:flutter/material.dart';

import '../models/sales_history_model.dart';
import '../services/sales_history_service.dart';
import '../theme/app_color_scheme.dart';
import '../utils/formatters.dart';
import '../widgets/filter_pill.dart';
import '../widgets/sale_transaction_card.dart';
import '../widgets/search_field.dart';

/// Seller's full sales history: lifetime totals, then a searchable,
/// filterable list of past transactions (completed or cancelled).
///
/// Reached from [SellerProfileScreen] rather than the bottom navigation —
/// pushed on top, so it gets a back arrow instead of the hamburger menu.
///
/// Backed by [SalesHistoryService] mocked data for now — no network call
/// yet. Swapping it for a real `GET /api/sub-orders?status=DELIVERED` later
/// only touches that service, not this screen or its widgets.
class SalesHistoryScreen extends StatefulWidget {
  const SalesHistoryScreen({super.key});

  @override
  State<SalesHistoryScreen> createState() => _SalesHistoryScreenState();
}

enum _HistoryFilter { all, completed, cancelled }

class _SalesHistoryScreenState extends State<SalesHistoryScreen> {
  final _history = SalesHistoryService().getHistory();
  final _searchController = TextEditingController();

  _HistoryFilter _filter = _HistoryFilter.all;
  String _query = '';

  @override
  void initState() {
    super.initState();
    _searchController.addListener(() {
      setState(() => _query = _searchController.text.trim().toLowerCase());
    });
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  List<SaleTransactionModel> get _filteredTransactions {
    return _history.transactions.where((t) {
      final matchesFilter = switch (_filter) {
        _HistoryFilter.all => true,
        _HistoryFilter.completed => t.status == SaleStatus.completed,
        _HistoryFilter.cancelled => t.status == SaleStatus.cancelled,
      };
      final matchesQuery = _query.isEmpty ||
          t.orderNumber.toLowerCase().contains(_query) ||
          t.customerName.toLowerCase().contains(_query);
      return matchesFilter && matchesQuery;
    }).toList();
  }

  void _showComingSoon(String feature) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text('$feature bientôt disponible.')));
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    final transactions = _filteredTransactions;

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
                  _buildTotalCard(colors, 'TOTAL DES VENTES', formatFcfa(_history.totalSales), Icons.savings_outlined),
                  const SizedBox(height: 12),
                  _buildTotalCard(colors, 'COMMANDES TRAITÉES', '${_history.ordersProcessed}', Icons.shopping_bag_outlined),
                  const SizedBox(height: 16),
                  SearchField(
                    hintText: 'Rechercher une commande (#CMD, client...)',
                    controller: _searchController,
                  ),
                  const SizedBox(height: 12),
                  _buildFilters(),
                  const SizedBox(height: 16),
                  if (transactions.isEmpty)
                    _buildEmptyState(colors)
                  else
                    for (final t in transactions) ...[
                      SaleTransactionCard(transaction: t),
                      const SizedBox(height: 12),
                    ],
                  const SizedBox(height: 8),
                  OutlinedButton(
                    onPressed: () => _showComingSoon('Le chargement de transactions supplémentaires'),
                    style: OutlinedButton.styleFrom(
                      foregroundColor: colors.textDark,
                      side: BorderSide(color: colors.border),
                      padding: const EdgeInsets.symmetric(vertical: 14),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(30)),
                    ),
                    child: const Text('Charger plus de transactions', style: TextStyle(fontWeight: FontWeight.w600)),
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
              'Historique des Ventes',
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

  Widget _buildTotalCard(AppColorScheme colors, String label, String value, IconData icon) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(18),
        boxShadow: [
          BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 12, offset: const Offset(0, 4)),
        ],
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(label, style: TextStyle(fontSize: 11, fontWeight: FontWeight.w600, color: colors.textMuted)),
              const SizedBox(height: 6),
              Text(value, style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: colors.textDark)),
            ],
          ),
          Icon(icon, color: colors.textMuted, size: 26),
        ],
      ),
    );
  }

  Widget _buildFilters() {
    return Row(
      children: [
        FilterPill(
          label: 'Terminées',
          isActive: _filter == _HistoryFilter.completed,
          onTap: () => setState(() =>
              _filter = _filter == _HistoryFilter.completed ? _HistoryFilter.all : _HistoryFilter.completed),
        ),
        const SizedBox(width: 8),
        FilterPill(
          label: 'Annulées',
          isActive: _filter == _HistoryFilter.cancelled,
          onTap: () => setState(() =>
              _filter = _filter == _HistoryFilter.cancelled ? _HistoryFilter.all : _HistoryFilter.cancelled),
        ),
      ],
    );
  }

  Widget _buildEmptyState(AppColorScheme colors) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 48),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.search_off_rounded, size: 48, color: colors.textMuted),
          const SizedBox(height: 12),
          Text('Aucune transaction ne correspond.', style: TextStyle(color: colors.textMuted, fontWeight: FontWeight.w500)),
        ],
      ),
    );
  }
}
