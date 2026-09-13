import 'package:flutter/material.dart';

import '../models/courier_history_model.dart';
import '../theme/app_color_scheme.dart';
import '../utils/formatters.dart';

/// One row in [CourierHistoryScreen]'s "Paiements" tab.
class PaymentHistoryRow extends StatelessWidget {
  final CourierPaymentModel payment;

  const PaymentHistoryRow({super.key, required this.payment});

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 10, offset: const Offset(0, 3)),
        ],
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Transfert ${payment.reference}', style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark)),
              Text(formatOrderDateTime(payment.date), style: TextStyle(fontSize: 12, color: colors.textMuted)),
            ],
          ),
          Column(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Text(formatFcfa(payment.amount), style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
              Container(
                margin: const EdgeInsets.only(top: 2),
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                decoration: BoxDecoration(color: colors.primary.withValues(alpha: 0.12), borderRadius: BorderRadius.circular(20)),
                child: Text('VERSÉ', style: TextStyle(color: colors.primary, fontSize: 10, fontWeight: FontWeight.w700)),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
