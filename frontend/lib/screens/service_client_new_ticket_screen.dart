import 'package:flutter/material.dart';

import '../models/service_client_model.dart';
import '../services/api_client.dart';
import '../services/service_client_service.dart';
import '../theme/app_color_scheme.dart';
import 'service_client_ticket_screen.dart';

/// Nouvelle demande d'aide : catégorie, sujet, description, numéro de commande facultatif.
class ServiceClientNewTicketScreen extends StatefulWidget {
  /// Numéro de commande à pré-remplir (ex. depuis l'écran de suivi d'une commande).
  final String? orderRef;

  const ServiceClientNewTicketScreen({super.key, this.orderRef});

  @override
  State<ServiceClientNewTicketScreen> createState() => _ServiceClientNewTicketScreenState();
}

class _ServiceClientNewTicketScreenState extends State<ServiceClientNewTicketScreen> {
  final _service = ServiceClientService();
  final _formKey = GlobalKey<FormState>();
  final _subject = TextEditingController();
  final _description = TextEditingController();
  late final _orderRef = TextEditingController(text: widget.orderRef ?? '');
  late final Future<List<SupportCategoryModel>> _categories = _service.getCategories();
  String? _category;
  bool _urgent = false;
  bool _sending = false;

  @override
  void dispose() {
    _subject.dispose();
    _description.dispose();
    _orderRef.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate() || _sending) return;
    setState(() => _sending = true);
    try {
      final ticket = await _service.createTicket(
        subject: _subject.text.trim(),
        description: _description.text.trim(),
        categoryCode: _category!,
        orderRef: _orderRef.text.trim(),
        urgent: _urgent,
      );
      if (!mounted) return;
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(builder: (_) => ServiceClientTicketScreen(ticketId: ticket.id)),
      );
    } on ApiException catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
    } finally {
      if (mounted) setState(() => _sending = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    return Scaffold(
      backgroundColor: colors.homeBackground,
      appBar: AppBar(
        backgroundColor: colors.surface,
        foregroundColor: colors.textDark,
        elevation: 0,
        title: const Text('Nouvelle demande', style: TextStyle(fontWeight: FontWeight.bold)),
      ),
      body: SafeArea(
        child: FutureBuilder<List<SupportCategoryModel>>(
          future: _categories,
          builder: (context, snapshot) {
            if (snapshot.connectionState == ConnectionState.waiting) {
              return const Center(child: CircularProgressIndicator());
            }
            if (snapshot.hasError) {
              final e = snapshot.error;
              return Center(child: Text(e is ApiException ? e.message : 'Service client indisponible.',
                  style: TextStyle(color: colors.error)));
            }
            final categories = snapshot.data ?? const [];
            return Form(
              key: _formKey,
              child: ListView(padding: const EdgeInsets.all(16), children: [
                DropdownButtonFormField<String>(
                  initialValue: _category,
                  decoration: const InputDecoration(labelText: 'Sujet de la demande'),
                  items: [for (final c in categories) DropdownMenuItem(value: c.code, child: Text(c.name))],
                  onChanged: (v) => setState(() => _category = v),
                  validator: (v) => v == null ? 'Choisissez un sujet' : null,
                ),
                const SizedBox(height: 12),
                TextFormField(
                  controller: _subject,
                  maxLength: 200,
                  decoration: const InputDecoration(labelText: 'Titre'),
                  validator: (v) => (v == null || v.trim().length < 3) ? 'Indiquez un titre' : null,
                ),
                TextFormField(
                  controller: _description,
                  maxLength: 5000,
                  minLines: 4,
                  maxLines: 8,
                  decoration: const InputDecoration(labelText: 'Décrivez votre problème', alignLabelWithHint: true),
                  validator: (v) => (v == null || v.trim().length < 10) ? 'Décrivez le problème (10 caractères minimum)' : null,
                ),
                TextFormField(
                  controller: _orderRef,
                  maxLength: 40,
                  decoration: const InputDecoration(labelText: 'Numéro de commande (facultatif)', hintText: 'CMD-000123'),
                  validator: (v) => (v != null && v.isNotEmpty && !RegExp(r'^[A-Za-z0-9-]+$').hasMatch(v))
                      ? 'Lettres, chiffres et tirets uniquement'
                      : null,
                ),
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  title: const Text('C\'est urgent'),
                  subtitle: const Text('Une livraison bloquée ou un paiement débité sans commande.'),
                  value: _urgent,
                  onChanged: (v) => setState(() => _urgent = v),
                ),
                const SizedBox(height: 8),
                FilledButton(
                  onPressed: _sending ? null : _submit,
                  child: _sending
                      ? const SizedBox(height: 18, width: 18, child: CircularProgressIndicator(strokeWidth: 2))
                      : const Text('Envoyer ma demande'),
                ),
              ]),
            );
          },
        ),
      ),
    );
  }
}
