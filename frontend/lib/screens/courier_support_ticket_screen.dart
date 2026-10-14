import 'package:flutter/material.dart';

import '../models/delivery_model.dart';
import '../services/api_client.dart';
import '../services/courier_delivery_service.dart';
import '../services/media_upload_service.dart';
import '../services/support_ticket_service.dart';
import '../theme/app_color_scheme.dart';
import '../widgets/primary_button.dart';
import 'courier_ticket_confirmation_screen.dart';

/// "Créer un ticket" — the courier's support-request form.
///
/// Backed by [SupportTicketService] (`POST /api/support-tickets`, un vrai
/// ticket avec numéro de suivi généré côté backend), avec sélection d'une
/// vraie livraison récente ([CourierDeliveryService]) et upload réel d'une
/// photo de preuve ([MediaUploadService]).
class CourierSupportTicketScreen extends StatefulWidget {
  const CourierSupportTicketScreen({super.key});

  @override
  State<CourierSupportTicketScreen> createState() => _CourierSupportTicketScreenState();
}

class _CourierSupportTicketScreenState extends State<CourierSupportTicketScreen> {
  static const _categories = ['Problème de livraison', 'Problème de paiement', 'Autre incident'];

  final _formKey = GlobalKey<FormState>();
  final _subjectController = TextEditingController();
  final _descriptionController = TextEditingController();
  final _mediaUploadService = MediaUploadService();

  late final Future<List<DeliveryModel>> _deliveriesFuture = CourierDeliveryService().getMyDeliveries();

  String _category = _categories.first;
  DeliveryModel? _selectedDelivery;
  String? _photoUrl;
  bool _isUploadingPhoto = false;
  bool _isSubmitting = false;

  @override
  void dispose() {
    _subjectController.dispose();
    _descriptionController.dispose();
    super.dispose();
  }

  void _showMessage(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(message)));
  }

  Future<void> _pickDelivery() async {
    final deliveries = await _deliveriesFuture;
    if (!mounted) return;
    final colors = context.colors;
    final picked = await showModalBottomSheet<DeliveryModel?>(
      context: context,
      backgroundColor: colors.surface,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (sheetContext) {
        return SafeArea(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const SizedBox(height: 12),
              Text('Commande concernée', style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
              const SizedBox(height: 8),
              ListTile(
                title: Text('Aucune', style: TextStyle(color: colors.textDark)),
                onTap: () => Navigator.of(sheetContext).pop(),
              ),
              for (final delivery in deliveries)
                ListTile(
                  title: Text('#${delivery.orderNumber} — ${delivery.shopName}', style: TextStyle(color: colors.textDark)),
                  subtitle: Text(delivery.status, style: TextStyle(fontSize: 12, color: colors.textMuted)),
                  onTap: () => Navigator.of(sheetContext).pop(delivery),
                ),
              const SizedBox(height: 8),
            ],
          ),
        );
      },
    );
    setState(() => _selectedDelivery = picked);
  }

  Future<void> _addPhoto() async {
    setState(() => _isUploadingPhoto = true);
    try {
      final url = await _mediaUploadService.pickImageAndUpload(folder: 'support-tickets');
      if (url != null) setState(() => _photoUrl = url);
    } on ApiException catch (e) {
      _showMessage(e.message);
    } finally {
      if (mounted) setState(() => _isUploadingPhoto = false);
    }
  }

  Future<void> _handleSubmit() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() => _isSubmitting = true);
    try {
      final ticket = await SupportTicketService().submitTicket(
        category: _category,
        relatedDeliveryId: _selectedDelivery?.id,
        subject: _subjectController.text.trim(),
        description: _descriptionController.text.trim(),
        photoUrl: _photoUrl,
      );
      if (!mounted) return;
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(builder: (_) => CourierTicketConfirmationScreen(ticketNumber: ticket.ticketNumber)),
      );
    } on ApiException catch (e) {
      _showMessage(e.message);
    } finally {
      if (mounted) setState(() => _isSubmitting = false);
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
            _buildHeader(colors),
            Expanded(
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(16),
                child: Form(
                  key: _formKey,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Notre équipe support vous répond 7j/7 pour résoudre vos incidents le plus rapidement possible.',
                        style: TextStyle(color: colors.textMuted, height: 1.4),
                      ),
                      const SizedBox(height: 20),
                      Text("Catégorie de l'incident", style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: colors.textDark)),
                      const SizedBox(height: 10),
                      Wrap(
                        spacing: 8,
                        runSpacing: 8,
                        children: _categories.map((category) {
                          final isActive = category == _category;
                          return GestureDetector(
                            onTap: () => setState(() => _category = category),
                            child: Container(
                              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                              decoration: BoxDecoration(
                                color: isActive ? colors.primary : colors.surface,
                                borderRadius: BorderRadius.circular(20),
                                border: isActive ? null : Border.all(color: colors.border),
                              ),
                              child: Text(
                                category,
                                style: TextStyle(color: isActive ? Colors.white : colors.textDark, fontWeight: FontWeight.w600, fontSize: 13),
                              ),
                            ),
                          );
                        }).toList(),
                      ),
                      const SizedBox(height: 20),
                      Text('Commande concernée (Optionnel)', style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: colors.textDark)),
                      const SizedBox(height: 8),
                      InkWell(
                        onTap: _pickDelivery,
                        child: Container(
                          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                          decoration: BoxDecoration(color: colors.inputFill, borderRadius: BorderRadius.circular(14)),
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Expanded(
                                child: Text(
                                  _selectedDelivery != null ? '#${_selectedDelivery!.orderNumber} — ${_selectedDelivery!.shopName}' : 'Sélectionnez une commande récente...',
                                  style: TextStyle(color: _selectedDelivery != null ? colors.textDark : colors.textMuted),
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                              Icon(Icons.keyboard_arrow_down_rounded, color: colors.textMuted),
                            ],
                          ),
                        ),
                      ),
                      const SizedBox(height: 20),
                      Text('Sujet', style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: colors.textDark)),
                      const SizedBox(height: 8),
                      TextFormField(
                        controller: _subjectController,
                        decoration: _inputDecoration(colors, 'Ex: Client injoignable à l\'adresse'),
                        validator: (value) => (value == null || value.trim().isEmpty) ? 'Indique un sujet.' : null,
                      ),
                      const SizedBox(height: 20),
                      Text('Description détaillée', style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: colors.textDark)),
                      const SizedBox(height: 8),
                      TextFormField(
                        controller: _descriptionController,
                        maxLines: 4,
                        decoration: _inputDecoration(colors, 'Décrivez le problème avec un maximum de détails pour faciliter la résolution par notre équipe.'),
                        validator: (value) => (value == null || value.trim().length < 10) ? 'Décris le problème (10 caractères min).' : null,
                      ),
                      const SizedBox(height: 16),
                      InkWell(
                        onTap: _isUploadingPhoto ? null : _addPhoto,
                        child: Container(
                          width: double.infinity,
                          padding: const EdgeInsets.symmetric(vertical: 16),
                          decoration: BoxDecoration(
                            borderRadius: BorderRadius.circular(14),
                            border: Border.all(color: _photoUrl != null ? colors.primary : colors.border),
                          ),
                          child: _isUploadingPhoto
                              ? Center(child: SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: colors.primary)))
                              : Column(
                                  children: [
                                    Icon(
                                      _photoUrl != null ? Icons.check_circle_rounded : Icons.add_photo_alternate_outlined,
                                      color: _photoUrl != null ? colors.primary : colors.textMuted,
                                    ),
                                    const SizedBox(height: 6),
                                    Text(
                                      _photoUrl != null ? 'Photo ajoutée' : 'Ajouter une photo ou une preuve',
                                      style: TextStyle(color: _photoUrl != null ? colors.primary : colors.textMuted, fontSize: 13),
                                    ),
                                    if (_photoUrl == null) ...[
                                      const SizedBox(height: 2),
                                      Text('Formats acceptés : JPG, PNG (Max 5MB)', style: TextStyle(color: colors.textMuted, fontSize: 11)),
                                    ],
                                  ],
                                ),
                        ),
                      ),
                      const SizedBox(height: 24),
                      PrimaryButton(label: 'Envoyer le ticket au support', isLoading: _isSubmitting, onPressed: _handleSubmit),
                    ],
                  ),
                ),
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
          Expanded(
            child: Column(
              children: [
                Text('AIDE ET ASSISTANCE', style: TextStyle(fontSize: 10, fontWeight: FontWeight.w700, color: colors.textMuted)),
                Text('Créer un ticket', style: TextStyle(fontWeight: FontWeight.bold, color: colors.primary)),
              ],
            ),
          ),
          const SizedBox(width: 48),
        ],
      ),
    );
  }

  InputDecoration _inputDecoration(AppColorScheme colors, String hint) {
    final border = OutlineInputBorder(borderRadius: BorderRadius.circular(14), borderSide: BorderSide(color: colors.border));
    return InputDecoration(
      hintText: hint,
      hintStyle: TextStyle(color: colors.textMuted, fontSize: 13),
      filled: true,
      fillColor: colors.inputFill,
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      border: border,
      enabledBorder: border,
      focusedBorder: border.copyWith(borderSide: BorderSide(color: colors.primary, width: 1.6)),
      errorBorder: border.copyWith(borderSide: BorderSide(color: colors.error)),
      focusedErrorBorder: border.copyWith(borderSide: BorderSide(color: colors.error, width: 1.6)),
    );
  }
}
