import 'package:flutter/material.dart';

import '../services/courier_documents_service.dart';
import '../theme/app_color_scheme.dart';
import '../widgets/document_status_row.dart';

/// "Mes Documents" — legal documents required to keep deliveries active.
///
/// Backed by [CourierDocumentsService] mocked data for now — no network
/// call, no backend entity to store/verify these yet (flagged as a gap).
class CourierDocumentsScreen extends StatelessWidget {
  const CourierDocumentsScreen({super.key});

  void _showComingSoon(BuildContext context, String feature) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text('$feature bientôt disponible.')));
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    final documents = CourierDocumentsService().getDocuments();

    return Scaffold(
      backgroundColor: colors.homeBackground,
      body: SafeArea(
        child: Column(
          children: [
            _buildHeader(context, colors),
            Expanded(
              child: ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  Container(
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(color: colors.inputFill, borderRadius: BorderRadius.circular(16)),
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Icon(Icons.shield_outlined, color: colors.primary),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text('Sécurité & Conformité', style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
                              const SizedBox(height: 4),
                              Text(
                                'Pour garantir la sécurité de notre réseau et activer vos livraisons, veuillez maintenir vos documents officiels à jour. Vos données sont cryptées et traitées avec la plus haute confidentialité.',
                                style: TextStyle(fontSize: 12, color: colors.textMuted, height: 1.4),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 16),
                  for (final document in documents) ...[
                    DocumentStatusRow(document: document, onUpdate: () => _showComingSoon(context, 'La mise à jour de ce document')),
                    const SizedBox(height: 12),
                  ],
                ],
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
          Expanded(child: Text('Mes Documents', textAlign: TextAlign.center, style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark))),
          const SizedBox(width: 48),
        ],
      ),
    );
  }
}
