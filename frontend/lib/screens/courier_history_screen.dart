import 'package:flutter/material.dart';

import '../services/courier_history_service.dart';
import '../theme/app_color_scheme.dart';
import '../widgets/filter_pill.dart';
import '../widgets/payment_history_row.dart';
import '../widgets/ride_history_row.dart';
import '../widgets/search_field.dart';

enum _HistoryTab { completed, payments, cancelled }

/// Courier's history: completed rides, payments received, and
/// cancellations — reached from [CourierProfileScreen].
///
/// Backed by [CourierHistoryService] mocked data for now — no network call.
class CourierHistoryScreen extends StatefulWidget {
  const CourierHistoryScreen({super.key});

  @override
  State<CourierHistoryScreen> createState() => _CourierHistoryScreenState();
}

class _CourierHistoryScreenState extends State<CourierHistoryScreen> {
  final _history = CourierHistoryService().getHistory();
  final _searchController = TextEditingController();
  _HistoryTab _tab = _HistoryTab.completed;

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return Scaffold(
      backgroundColor: colors.homeBackground,
      body: SafeArea(
        child: Column(
          children: [
            _buildHeader(colors),
            Expanded(
              child: ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  Text('Historique', style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: colors.textDark)),
                  const SizedBox(height: 4),
                  Text('Vos courses terminées', style: TextStyle(fontSize: 13, color: colors.textMuted)),
                  const SizedBox(height: 14),
                  SearchField(hintText: 'Rechercher une course, un paiement...', controller: _searchController),
                  const SizedBox(height: 12),
                  _buildFilters(),
                  const SizedBox(height: 16),
                  ..._buildContent(colors),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader(AppColorScheme colors) {
    return Container(
      color: colors.surface,
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
      child: Row(
        children: [
          IconButton(
            icon: Icon(Icons.arrow_back_rounded, color: colors.textDark),
            onPressed: () => Navigator.of(context).pop(),
          ),
          Expanded(child: Text('Historique', textAlign: TextAlign.center, style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark))),
          const SizedBox(width: 48),
        ],
      ),
    );
  }

  Widget _buildFilters() {
    return Row(
      children: [
        FilterPill(label: 'Courses Terminées', isActive: _tab == _HistoryTab.completed, onTap: () => setState(() => _tab = _HistoryTab.completed)),
        const SizedBox(width: 8),
        FilterPill(label: 'Paiements Reçus', isActive: _tab == _HistoryTab.payments, onTap: () => setState(() => _tab = _HistoryTab.payments)),
        const SizedBox(width: 8),
        FilterPill(label: 'Annulées', isActive: _tab == _HistoryTab.cancelled, onTap: () => setState(() => _tab = _HistoryTab.cancelled)),
      ],
    );
  }

  List<Widget> _buildContent(AppColorScheme colors) {
    switch (_tab) {
      case _HistoryTab.completed:
        if (_history.completedRides.isEmpty) return [_buildEmptyState(colors, 'Aucune course terminée.')];
        return [
          for (final ride in _history.completedRides) ...[RideHistoryRow(ride: ride), const SizedBox(height: 12)],
        ];
      case _HistoryTab.payments:
        if (_history.payments.isEmpty) return [_buildEmptyState(colors, 'Aucun paiement reçu.')];
        return [
          for (final payment in _history.payments) ...[PaymentHistoryRow(payment: payment), const SizedBox(height: 12)],
        ];
      case _HistoryTab.cancelled:
        if (_history.cancelledRides.isEmpty) return [_buildEmptyState(colors, 'Aucune course annulée.')];
        return [
          for (final ride in _history.cancelledRides) ...[RideHistoryRow(ride: ride), const SizedBox(height: 12)],
        ];
    }
  }

  Widget _buildEmptyState(AppColorScheme colors, String message) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 48),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.inbox_outlined, size: 44, color: colors.textMuted),
          const SizedBox(height: 12),
          Text(message, style: TextStyle(color: colors.textMuted, fontWeight: FontWeight.w500)),
        ],
      ),
    );
  }
}
