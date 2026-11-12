import 'package:flutter/material.dart';

import '../models/service_client_model.dart';
import '../services/api_client.dart';
import '../services/service_client_service.dart';
import '../theme/app_color_scheme.dart';
import '../utils/formatters.dart';
import '../widgets/support_ui.dart';
import 'service_client_ticket_screen.dart';

/// Notifications du service client (réponse de l'équipe, demande résolue...). Un appui ouvre la demande.
class ServiceClientNotificationsScreen extends StatefulWidget {
  const ServiceClientNotificationsScreen({super.key});

  @override
  State<ServiceClientNotificationsScreen> createState() => _ServiceClientNotificationsScreenState();
}

class _ServiceClientNotificationsScreenState extends State<ServiceClientNotificationsScreen> {
  final _service = ServiceClientService();
  late Future<List<SupportNotificationModel>> _future = _service.getNotifications();

  Future<void> _open(SupportNotificationModel n) async {
    try {
      if (!n.read) await _service.markRead(n.id);
    } on ApiException {
      // marquer comme lu n'est pas bloquant
    }
    if (n.ticketId == null || !mounted) return;
    await Navigator.of(context).push(MaterialPageRoute(builder: (_) => ServiceClientTicketScreen(ticketId: n.ticketId!)));
    if (mounted) setState(() => _future = _service.getNotifications());
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    return Scaffold(
      backgroundColor: colors.homeBackground,
      body: SafeArea(
        child: Column(children: [
          const SupportHeader(title: 'Notifications'),
          Expanded(child: FutureBuilder<List<SupportNotificationModel>>(
          future: _future,
          builder: (context, snapshot) {
            if (snapshot.connectionState == ConnectionState.waiting) {
              return const Center(child: CircularProgressIndicator());
            }
            if (snapshot.hasError) {
              final e = snapshot.error;
              return Center(child: Text(e is ApiException ? e.message : 'Service client indisponible.',
                  style: TextStyle(color: colors.error)));
            }
            final items = snapshot.data ?? const [];
            if (items.isEmpty) {
              return Center(child: Text('Aucune notification.', style: TextStyle(color: colors.textMuted)));
            }
            return ListView.separated(
              padding: const EdgeInsets.all(16),
              itemCount: items.length,
              separatorBuilder: (_, _) => const SizedBox(height: 8),
              itemBuilder: (context, i) {
                final n = items[i];
                return ListTile(
                  tileColor: colors.surface,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                  leading: Icon(n.read ? Icons.mark_email_read_outlined : Icons.mark_email_unread_rounded, color: colors.primary),
                  title: Text(n.title, style: TextStyle(fontWeight: n.read ? FontWeight.w500 : FontWeight.bold)),
                  subtitle: Text('${n.body}\n${formatRelativeDate(n.createdAt)}'),
                  isThreeLine: true,
                  onTap: () => _open(n),
                );
              },
            );
          },
        )),
        ]),
      ),
    );
  }
}
