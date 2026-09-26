import 'package:flutter/material.dart';

import '../models/courier_document_model.dart';
import '../services/api_client.dart';
import '../services/courier_documents_service.dart';
import '../services/media_upload_service.dart';
import '../theme/app_color_scheme.dart';
import '../widgets/document_status_row.dart';

/// "Mes Documents" — les 4 documents légaux exigés d'un livreur, avec envoi
/// (photo Cloudinary + date d'expiration optionnelle) et vérification admin.
///
/// Backed by [CourierDocumentsService] (`GET/POST /api/delivery-agents/me/documents`).
class CourierDocumentsScreen extends StatefulWidget {
  const CourierDocumentsScreen({super.key});

  @override
  State<CourierDocumentsScreen> createState() => _CourierDocumentsScreenState();
}

class _CourierDocumentsScreenState extends State<CourierDocumentsScreen> {
  final _service = CourierDocumentsService();
  final _mediaUploadService = MediaUploadService();
  late Future<List<CourierDocumentModel>> _documentsFuture;
  bool _isUploading = false;

  @override
  void initState() {
    super.initState();
    _documentsFuture = _service.getDocuments();
  }

  void _showMessage(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(message)));
  }

  Future<void> _updateDocument(CourierDocumentModel document) async {
    final fileUrl = await _mediaUploadService.pickImageAndUpload(folder: 'courier-documents');
    if (fileUrl == null || !mounted) return;

    final expiresAt = await showDatePicker(
      context: context,
      initialDate: DateTime.now().add(const Duration(days: 365)),
      firstDate: DateTime.now(),
      lastDate: DateTime.now().add(const Duration(days: 3650)),
      helpText: "Date d'expiration (optionnelle)",
      cancelText: 'Aucune',
    );
    if (!mounted) return;

    setState(() => _isUploading = true);
    try {
      await _service.submit(type: document.type, fileUrl: fileUrl, expiresAt: expiresAt);
      if (!mounted) return;
      setState(() => _documentsFuture = _service.getDocuments());
      _showMessage('Document envoyé, en attente de vérification.');
    } on ApiException catch (e) {
      _showMessage(e.message);
    } finally {
      if (mounted) setState(() => _isUploading = false);
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
              child: Stack(
                children: [
                  FutureBuilder<List<CourierDocumentModel>>(
                    future: _documentsFuture,
                    builder: (context, snapshot) {
                      if (snapshot.connectionState == ConnectionState.waiting) {
                        return const Center(child: CircularProgressIndicator());
                      }
                      if (snapshot.hasError) {
                        final message = snapshot.error is ApiException
                            ? (snapshot.error as ApiException).message
                            : 'Impossible de charger vos documents.';
                        return Center(child: Text(message, style: TextStyle(color: colors.textMuted)));
                      }
                      final documents = snapshot.data ?? const [];

                      return ListView(
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
                            DocumentStatusRow(document: document, onUpdate: _isUploading ? () {} : () => _updateDocument(document)),
                            const SizedBox(height: 12),
                          ],
                        ],
                      );
                    },
                  ),
                  if (_isUploading)
                    Container(
                      color: Colors.black.withValues(alpha: 0.05),
                      child: const Center(child: CircularProgressIndicator()),
                    ),
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
