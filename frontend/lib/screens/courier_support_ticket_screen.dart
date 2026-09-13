import 'package:flutter/material.dart';

import '../services/support_ticket_service.dart';
import '../theme/app_color_scheme.dart';
import '../widgets/primary_button.dart';
import 'courier_ticket_confirmation_screen.dart';

/// "Créer un ticket" — the courier's support-request form.
///
/// Backed by [SupportTicketService], mocked: no structured ticket entity
/// exists on the backend yet (see that service's doc comment), so
/// submitting just simulates a delay and generates a ticket number
/// locally.
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

  String _category = _categories.first;
  bool _isSubmitting = false;

  @override
  void dispose() {
    _subjectController.dispose();
    _descriptionController.dispose();
    super.dispose();
  }

  void _showComingSoon(String feature) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text('$feature bientôt disponible.')));
  }

  Future<void> _handleSubmit() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() => _isSubmitting = true);
    final ticketNumber = await SupportTicketService().submitTicket(
      category: _category,
      subject: _subjectController.text.trim(),
      description: _descriptionController.text.trim(),
    );
    if (!mounted) return;
    setState(() => _isSubmitting = false);

    Navigator.of(context).pushReplacement(
      MaterialPageRoute(builder: (_) => CourierTicketConfirmationScreen(ticketNumber: ticketNumber)),
    );
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
                        onTap: () => _showComingSoon('La sélection de commande'),
                        child: Container(
                          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                          decoration: BoxDecoration(color: colors.inputFill, borderRadius: BorderRadius.circular(14)),
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Text('Sélectionnez une commande récente...', style: TextStyle(color: colors.textMuted)),
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
                        onTap: () => _showComingSoon('L\'ajout de photo'),
                        child: Container(
                          width: double.infinity,
                          padding: const EdgeInsets.symmetric(vertical: 16),
                          decoration: BoxDecoration(
                            borderRadius: BorderRadius.circular(14),
                            border: Border.all(color: colors.border),
                          ),
                          child: Column(
                            children: [
                              Icon(Icons.add_photo_alternate_outlined, color: colors.textMuted),
                              const SizedBox(height: 6),
                              Text('Ajouter une photo ou une preuve', style: TextStyle(color: colors.textMuted, fontSize: 13)),
                              const SizedBox(height: 2),
                              Text('Formats acceptés : JPG, PNG (Max 5MB)', style: TextStyle(color: colors.textMuted, fontSize: 11)),
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
          IconButton(
            icon: Icon(Icons.notifications_none_rounded, color: colors.textDark),
            onPressed: () => _showComingSoon('Notifications'),
          ),
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
