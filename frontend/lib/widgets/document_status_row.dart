import 'package:flutter/material.dart';

import '../models/courier_document_model.dart';
import '../theme/app_color_scheme.dart';

/// One row in [CourierDocumentsScreen]: icon, title, expiry/status info,
/// a status badge and a "Mettre à jour" action.
class DocumentStatusRow extends StatelessWidget {
  final CourierDocumentModel document;
  final VoidCallback onUpdate;

  const DocumentStatusRow({super.key, required this.document, required this.onUpdate});

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    final isPending = document.status == DocumentStatus.pending;
    final statusColor = isPending ? colors.accentOrange : colors.primary;

    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(16),
        border: isPending ? Border(left: BorderSide(color: colors.accentOrange, width: 3)) : null,
        boxShadow: [
          BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 10, offset: const Offset(0, 3)),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(color: colors.inputFill, borderRadius: BorderRadius.circular(10)),
                child: Icon(Icons.description_outlined, size: 18, color: colors.textDark),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(document.title, style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark)),
                    Text(document.infoLine, style: TextStyle(fontSize: 12, color: colors.textMuted)),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(color: statusColor.withValues(alpha: 0.12), borderRadius: BorderRadius.circular(20)),
                child: Text(
                  isPending ? 'En attente' : 'Validé',
                  style: TextStyle(color: statusColor, fontSize: 11, fontWeight: FontWeight.w700),
                ),
              ),
              TextButton(
                onPressed: onUpdate,
                style: TextButton.styleFrom(
                  foregroundColor: isPending ? Colors.white : colors.textDark,
                  backgroundColor: isPending ? colors.accentOrange : Colors.transparent,
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
                ),
                child: const Text('Mettre à jour', style: TextStyle(fontSize: 12, fontWeight: FontWeight.w600)),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
