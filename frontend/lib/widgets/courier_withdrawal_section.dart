import 'package:flutter/material.dart';

import '../models/courier_withdrawal_model.dart';
import '../services/api_client.dart';
import '../services/courier_earnings_service.dart';
import '../theme/app_color_scheme.dart';
import '../utils/formatters.dart';

/// Solde disponible, demande de retrait et suivi des retraits du livreur (onglet "Paiements").
/// Un retrait n'est jamais payé directement : il suit le circuit de l'équipe Maplenou
/// (vérification, approbation du Super Admin) avant le virement sur les coordonnées indiquées.
class CourierWithdrawalSection extends StatefulWidget {
  const CourierWithdrawalSection({super.key});

  @override
  State<CourierWithdrawalSection> createState() => _CourierWithdrawalSectionState();
}

class _CourierWithdrawalSectionState extends State<CourierWithdrawalSection> {
  final _service = CourierEarningsService();
  late Future<(num, List<CourierWithdrawalModel>)> _future;
  bool _isRequesting = false;

  @override
  void initState() {
    super.initState();
    _future = _load();
  }

  Future<(num, List<CourierWithdrawalModel>)> _load() async {
    final balance = await _service.getAvailableBalance();
    final withdrawals = await _service.getWithdrawals();
    return (balance, withdrawals);
  }

  Future<void> _requestWithdrawal() async {
    final controller = TextEditingController();
    final details = await showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Demander un retrait'),
        content: TextField(
          controller: controller,
          decoration: const InputDecoration(
            labelText: 'Coordonnées de paiement',
            hintText: 'Ex. T-Money +22890123456',
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('Annuler')),
          TextButton(onPressed: () => Navigator.pop(context, controller.text.trim()), child: const Text('Envoyer')),
        ],
      ),
    );
    if (details == null || details.length < 5) return;

    setState(() => _isRequesting = true);
    try {
      await _service.requestWithdrawal(details);
      if (!mounted) return;
      setState(() => _future = _load());
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(const SnackBar(
          content: Text("Demande de retrait envoyée. Elle sera vérifiée puis approuvée par l'équipe."),
        ));
    } on ApiException catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(SnackBar(content: Text(e.message)));
    } finally {
      if (mounted) setState(() => _isRequesting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return FutureBuilder<(num, List<CourierWithdrawalModel>)>(
      future: _future,
      builder: (context, snapshot) {
        if (!snapshot.hasData) return const SizedBox.shrink();
        final (balance, withdrawals) = snapshot.data!;

        return Container(
          margin: const EdgeInsets.only(bottom: 16),
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: colors.surface,
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: colors.border),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Solde disponible', style: TextStyle(color: colors.textMuted, fontSize: 12)),
              const SizedBox(height: 2),
              Text(formatFcfa(balance),
                  style: TextStyle(color: colors.textDark, fontSize: 22, fontWeight: FontWeight.bold)),
              const SizedBox(height: 10),
              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: balance > 0 && !_isRequesting ? _requestWithdrawal : null,
                  child: Text(_isRequesting ? 'Envoi...' : 'Demander un retrait'),
                ),
              ),
              if (withdrawals.isNotEmpty) ...[
                const SizedBox(height: 14),
                Text('Mes retraits', style: TextStyle(color: colors.textDark, fontWeight: FontWeight.w700)),
                const SizedBox(height: 6),
                for (final w in withdrawals)
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 4),
                    child: Row(
                      children: [
                        Expanded(
                          child: Text(
                            '${w.createdAt.day}/${w.createdAt.month}/${w.createdAt.year} · ${formatFcfa(w.amount)}',
                            style: TextStyle(color: colors.textDark, fontSize: 13),
                          ),
                        ),
                        Text(w.statusLabel,
                            style: TextStyle(color: colors.textMuted, fontSize: 12, fontWeight: FontWeight.w600)),
                      ],
                    ),
                  ),
              ],
            ],
          ),
        );
      },
    );
  }
}
