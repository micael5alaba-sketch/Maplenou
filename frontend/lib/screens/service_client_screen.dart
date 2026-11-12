import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

import '../config/api_config.dart';
import '../models/service_client_model.dart';
import '../services/api_client.dart';
import '../services/service_client_service.dart';
import '../theme/app_color_scheme.dart';
import '../utils/formatters.dart';
import 'service_client_new_ticket_screen.dart';
import 'service_client_notifications_screen.dart';
import 'service_client_ticket_screen.dart';

/// Service client : mes demandes d'aide. Les tickets vivent dans `ServiceClient/` ; l'application s'y connecte avec
/// le compte Maplenou déjà ouvert (aucun second mot de passe).
class ServiceClientScreen extends StatefulWidget {
  const ServiceClientScreen({super.key});

  @override
  State<ServiceClientScreen> createState() => _ServiceClientScreenState();
}

class _ServiceClientScreenState extends State<ServiceClientScreen> {
  final _service = ServiceClientService();
  late Future<List<SupportTicketSummary>> _future = _service.getMyTickets();

  void _reload() => setState(() => _future = _service.getMyTickets());

  Future<void> _open(Widget screen) async {
    await Navigator.of(context).push(MaterialPageRoute(builder: (_) => screen));
    if (mounted) _reload();
  }

  Future<void> _openWebFallback() async {
    await launchUrl(Uri.parse(ApiConfig.supportWebUrl), mode: LaunchMode.externalApplication);
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
        title: const Text('Service client', style: TextStyle(fontWeight: FontWeight.bold)),
        actions: [
          IconButton(
            tooltip: 'Notifications',
            icon: const Icon(Icons.notifications_none_rounded),
            onPressed: () => _open(const ServiceClientNotificationsScreen()),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => _open(const ServiceClientNewTicketScreen()),
        icon: const Icon(Icons.add_rounded),
        label: const Text('Nouvelle demande'),
      ),
      body: SafeArea(
        child: FutureBuilder<List<SupportTicketSummary>>(
          future: _future,
          builder: (context, snapshot) {
            if (snapshot.connectionState == ConnectionState.waiting) {
              return const Center(child: CircularProgressIndicator());
            }
            if (snapshot.hasError) return _error(colors, snapshot.error);
            final tickets = snapshot.data ?? const [];
            if (tickets.isEmpty) {
              return Center(
                child: Padding(
                  padding: const EdgeInsets.all(28),
                  child: Column(mainAxisSize: MainAxisSize.min, children: [
                    Icon(Icons.support_agent_rounded, size: 56, color: colors.primary),
                    const SizedBox(height: 12),
                    Text('Aucune demande pour le moment', style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
                    const SizedBox(height: 6),
                    Text('Un problème avec une commande, une livraison ou un paiement ? Notre équipe vous répond.',
                        textAlign: TextAlign.center, style: TextStyle(color: colors.textMuted)),
                  ]),
                ),
              );
            }
            return RefreshIndicator(
              onRefresh: () async => _reload(),
              child: ListView.separated(
                padding: const EdgeInsets.fromLTRB(16, 16, 16, 96),
                itemCount: tickets.length,
                separatorBuilder: (_, _) => const SizedBox(height: 10),
                itemBuilder: (context, i) {
                  final t = tickets[i];
                  return InkWell(
                    borderRadius: BorderRadius.circular(14),
                    onTap: () => _open(ServiceClientTicketScreen(ticketId: t.id)),
                    child: Container(
                      padding: const EdgeInsets.all(14),
                      decoration: BoxDecoration(
                        color: colors.surface,
                        borderRadius: BorderRadius.circular(14),
                        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 8, offset: const Offset(0, 3))],
                      ),
                      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                        Row(children: [
                          Expanded(child: Text(t.ticketNumber, style: TextStyle(fontSize: 12, color: colors.textMuted))),
                          Chip(
                            label: Text(t.statusLabel, style: const TextStyle(fontSize: 11)),
                            visualDensity: VisualDensity.compact,
                            backgroundColor: t.isClosedLike ? colors.inputFill : colors.primary.withValues(alpha: 0.15),
                          ),
                        ]),
                        Text(t.subject, maxLines: 2, overflow: TextOverflow.ellipsis,
                            style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark)),
                        const SizedBox(height: 4),
                        Text('${t.categoryName} · ${formatRelativeDate(t.updatedAt)}',
                            style: TextStyle(fontSize: 12, color: colors.textMuted)),
                      ]),
                    ),
                  );
                },
              ),
            );
          },
        ),
      ),
    );
  }

  Widget _error(AppColorScheme colors, Object? error) {
    final message = error is ApiException ? error.message : 'Le service client est momentanément indisponible.';
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          Text(message, textAlign: TextAlign.center, style: TextStyle(color: colors.error)),
          const SizedBox(height: 12),
          FilledButton(onPressed: _reload, child: const Text('Réessayer')),
          TextButton(onPressed: _openWebFallback, child: const Text('Ouvrir le service client dans le navigateur')),
        ]),
      ),
    );
  }
}
