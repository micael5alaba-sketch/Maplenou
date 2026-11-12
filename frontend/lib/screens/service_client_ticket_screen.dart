import 'package:flutter/material.dart';

import '../models/service_client_model.dart';
import '../services/api_client.dart';
import '../services/service_client_service.dart';
import '../theme/app_color_scheme.dart';
import '../utils/formatters.dart';

/// Une demande : description, conversation avec l'équipe, réponse, demande de réouverture et note de satisfaction.
class ServiceClientTicketScreen extends StatefulWidget {
  final String ticketId;

  const ServiceClientTicketScreen({super.key, required this.ticketId});

  @override
  State<ServiceClientTicketScreen> createState() => _ServiceClientTicketScreenState();
}

class _ServiceClientTicketScreenState extends State<ServiceClientTicketScreen> {
  final _service = ServiceClientService();
  final _reply = TextEditingController();
  late Future<(SupportTicketDetail, List<SupportMessage>)> _future = _load();

  /// Identifiant du message en cours d'envoi : conservé tant qu'il n'a pas réussi, pour qu'un renvoi (double appui,
  /// coupure réseau) ne publie jamais deux fois le même message.
  String? _pendingMessageId;
  bool _sending = false;

  Future<(SupportTicketDetail, List<SupportMessage>)> _load() async {
    final results = await Future.wait([_service.getTicket(widget.ticketId), _service.getMessages(widget.ticketId)]);
    return (results[0] as SupportTicketDetail, results[1] as List<SupportMessage>);
  }

  void _reload() => setState(() => _future = _load());

  @override
  void dispose() {
    _reply.dispose();
    super.dispose();
  }

  void _toast(String message) {
    if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }

  Future<void> _send() async {
    final text = _reply.text.trim();
    if (text.isEmpty || _sending) return;
    setState(() => _sending = true);
    _pendingMessageId ??= ServiceClientService.newMessageId();
    try {
      await _service.sendMessage(widget.ticketId, text, clientMessageId: _pendingMessageId!);
      _pendingMessageId = null;
      _reply.clear();
      _reload();
    } on ApiException catch (e) {
      _toast(e.message);
    } finally {
      if (mounted) setState(() => _sending = false);
    }
  }

  Future<String?> _askText(String title, String hint, {bool required = true}) async {
    final controller = TextEditingController();
    final value = await showDialog<String>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: Text(title),
        content: TextField(controller: controller, maxLines: 4, maxLength: 1000, decoration: InputDecoration(hintText: hint)),
        actions: [
          TextButton(onPressed: () => Navigator.of(dialogContext).pop(), child: const Text('Annuler')),
          FilledButton(onPressed: () => Navigator.of(dialogContext).pop(controller.text.trim()), child: const Text('Envoyer')),
        ],
      ),
    );
    if (value == null || (required && value.isEmpty)) return null;
    return value;
  }

  Future<void> _reopen() async {
    final reason = await _askText('Rouvrir la demande', 'Pourquoi le problème n\'est-il pas résolu ?');
    if (reason == null) return;
    try {
      await _service.requestReopen(widget.ticketId, reason);
      _toast('Votre demande de réouverture a été envoyée.');
      _reload();
    } on ApiException catch (e) {
      _toast(e.message);
    }
  }

  Future<void> _rate() async {
    var rating = 5;
    final commentCtrl = TextEditingController();
    final ok = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => StatefulBuilder(
        builder: (context, setLocal) => AlertDialog(
          title: const Text('Votre avis sur le service client'),
          content: Column(mainAxisSize: MainAxisSize.min, children: [
            Row(mainAxisAlignment: MainAxisAlignment.center, children: [
              for (var i = 1; i <= 5; i++)
                IconButton(
                  icon: Icon(i <= rating ? Icons.star_rounded : Icons.star_border_rounded, color: Colors.amber),
                  onPressed: () => setLocal(() => rating = i),
                ),
            ]),
            TextField(controller: commentCtrl, maxLines: 3, maxLength: 1000, decoration: const InputDecoration(hintText: 'Un commentaire (facultatif)')),
          ]),
          actions: [
            TextButton(onPressed: () => Navigator.of(dialogContext).pop(false), child: const Text('Annuler')),
            FilledButton(onPressed: () => Navigator.of(dialogContext).pop(true), child: const Text('Envoyer')),
          ],
        ),
      ),
    );
    if (ok != true) return;
    try {
      await _service.rate(widget.ticketId, rating, commentCtrl.text.trim());
      _toast('Merci pour votre avis.');
      _reload();
    } on ApiException catch (e) {
      _toast(e.message);
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
        title: const Text('Ma demande', style: TextStyle(fontWeight: FontWeight.bold)),
      ),
      body: SafeArea(
        child: FutureBuilder<(SupportTicketDetail, List<SupportMessage>)>(
          future: _future,
          builder: (context, snapshot) {
            if (snapshot.connectionState == ConnectionState.waiting) {
              return const Center(child: CircularProgressIndicator());
            }
            if (snapshot.hasError) {
              final e = snapshot.error;
              return Center(
                child: Column(mainAxisSize: MainAxisSize.min, children: [
                  Text(e is ApiException ? e.message : 'Service client indisponible.', style: TextStyle(color: colors.error)),
                  const SizedBox(height: 8),
                  FilledButton(onPressed: _reload, child: const Text('Réessayer')),
                ]),
              );
            }
            final (ticket, messages) = snapshot.data!;
            return Column(children: [
              Expanded(
                child: RefreshIndicator(
                  onRefresh: () async => _reload(),
                  child: ListView(padding: const EdgeInsets.all(16), children: [
                    Text(ticket.ticketNumber, style: TextStyle(fontSize: 12, color: colors.textMuted)),
                    Text(ticket.subject, style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: colors.textDark)),
                    const SizedBox(height: 6),
                    Wrap(spacing: 8, children: [
                      Chip(label: Text(ticket.statusLabel), visualDensity: VisualDensity.compact),
                      Chip(label: Text(ticket.categoryName), visualDensity: VisualDensity.compact),
                      if (ticket.orderRef != null) Chip(label: Text('Commande ${ticket.orderRef}'), visualDensity: VisualDensity.compact),
                    ]),
                    const SizedBox(height: 8),
                    Text(ticket.description, style: TextStyle(color: colors.textDark, height: 1.4)),
                    const Divider(height: 28),
                    if (messages.isEmpty)
                      Text('Notre équipe vous répondra ici.', style: TextStyle(color: colors.textMuted)),
                    for (final m in messages) _bubble(colors, m),
                    const SizedBox(height: 8),
                    if (ticket.can('REQUEST_REOPEN') && !ticket.reopenPending)
                      OutlinedButton.icon(onPressed: _reopen, icon: const Icon(Icons.replay_rounded), label: const Text('Le problème persiste : rouvrir')),
                    if (ticket.reopenPending)
                      Text('Votre demande de réouverture est en cours d\'examen.', style: TextStyle(color: colors.textMuted)),
                    if (ticket.can('RATE') && ticket.rating == null)
                      OutlinedButton.icon(onPressed: _rate, icon: const Icon(Icons.star_outline_rounded), label: const Text('Donner mon avis')),
                    if (ticket.rating != null) Text('Votre note : ${ticket.rating}/5', style: TextStyle(color: colors.textMuted)),
                  ]),
                ),
              ),
              if (ticket.can('SEND_MESSAGE'))
                Container(
                  color: colors.surface,
                  padding: const EdgeInsets.fromLTRB(12, 8, 12, 8),
                  child: Row(children: [
                    Expanded(
                      child: TextField(
                        controller: _reply,
                        minLines: 1,
                        maxLines: 4,
                        maxLength: 5000,
                        decoration: const InputDecoration(hintText: 'Votre message', counterText: ''),
                      ),
                    ),
                    IconButton(
                      onPressed: _sending ? null : _send,
                      icon: _sending
                          ? const SizedBox(height: 18, width: 18, child: CircularProgressIndicator(strokeWidth: 2))
                          : Icon(Icons.send_rounded, color: colors.primary),
                    ),
                  ]),
                ),
            ]);
          },
        ),
      ),
    );
  }

  Widget _bubble(AppColorScheme colors, SupportMessage m) {
    return Align(
      alignment: m.fromStaff ? Alignment.centerLeft : Alignment.centerRight,
      child: Container(
        margin: const EdgeInsets.only(bottom: 8),
        padding: const EdgeInsets.all(12),
        constraints: BoxConstraints(maxWidth: MediaQuery.of(context).size.width * 0.8),
        decoration: BoxDecoration(
          color: m.fromStaff ? colors.surface : colors.primary.withValues(alpha: 0.15),
          borderRadius: BorderRadius.circular(14),
        ),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(m.fromStaff ? 'Service client Maplenou' : 'Vous',
              style: TextStyle(fontSize: 11, fontWeight: FontWeight.w600, color: colors.textMuted)),
          const SizedBox(height: 2),
          Text(m.body, style: TextStyle(color: colors.textDark)),
          const SizedBox(height: 2),
          Text(formatRelativeDate(m.createdAt), style: TextStyle(fontSize: 10, color: colors.textMuted)),
        ]),
      ),
    );
  }
}
