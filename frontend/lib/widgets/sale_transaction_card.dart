import 'package:flutter/material.dart';

import '../models/sales_history_model.dart';
import '../theme/app_color_scheme.dart';
import '../utils/formatters.dart';

/// One row in [SalesHistoryScreen]'s transaction list: order number, status
/// badge, customer, items summary, date and amount (struck through when
/// cancelled).
class SaleTransactionCard extends StatelessWidget {
  final SaleTransactionModel transaction;

  const SaleTransactionCard({super.key, required this.transaction});

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    final isCancelled = transaction.status == SaleStatus.cancelled;
    final statusColor = isCancelled ? colors.error : colors.primary;

    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 10, offset: const Offset(0, 4)),
        ],
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(color: colors.inputFill, borderRadius: BorderRadius.circular(10)),
            child: Icon(Icons.receipt_outlined, size: 18, color: colors.textMuted),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Text(transaction.orderNumber, style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
                    const SizedBox(width: 8),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                      decoration: BoxDecoration(
                        color: statusColor.withValues(alpha: 0.12),
                        borderRadius: BorderRadius.circular(20),
                      ),
                      child: Text(
                        transaction.status.label,
                        style: TextStyle(color: statusColor, fontSize: 10, fontWeight: FontWeight.w600),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 4),
                Text(transaction.customerName, style: TextStyle(fontSize: 13, color: colors.textDark)),
                Text(
                  '${transaction.itemsSummary} • ${formatOrderDateTime(transaction.dateTime)}',
                  style: TextStyle(fontSize: 12, color: colors.textMuted),
                ),
              ],
            ),
          ),
          Text(
            formatFcfa(transaction.amount),
            style: TextStyle(
              fontWeight: FontWeight.bold,
              color: isCancelled ? colors.textMuted : colors.textDark,
              decoration: isCancelled ? TextDecoration.lineThrough : null,
            ),
          ),
        ],
      ),
    );
  }
}
