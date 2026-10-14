import 'package:flutter/material.dart';

import '../models/support_ticket_model.dart';
import '../services/api_client.dart';
import '../services/support_ticket_service.dart';
import '../theme/app_color_scheme.dart';

/// "Mes tickets" — historique réel des tickets de support soumis par le
/// compte connecté (livreur ou acheteur).
///
/// Backed by `GET /api/support-tickets/mine` via [SupportTicketService].
class MySupportTicketsScreen extends StatefulWidget {
  const MySupportTicketsScreen({super.key});

  @override
  State<MySupportTicketsScreen> createState() => _MySupportTicketsScreenState();
}

class _MySupportTicketsScreenState extends State<MySupportTicketsScreen> {
  late Future<List<SupportTicketModel>> _ticketsFuture;

  @override
  void initState() {
    super.initState();
    _ticketsFuture = SupportTicketService().getMyTickets();
  }

  void _reload() {
    setState(() => _ticketsFuture = SupportTicketService().getMyTickets());
  }

  Color _statusColor(AppColorScheme colors, SupportTicketStatus status) {
    switch (status) {
      case SupportTicketStatus.open:
        return colors.accentOrange;
      case SupportTicketStatus.inProgress:
        return colors.primary;
      case SupportTicketStatus.resolved:
        return Colors.green;
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
            Container(
              color: colors.surface,
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
              child: Row(
                children: [
                  IconButton(icon: Icon(Icons.arrow_back_rounded, color: colors.textDark), onPressed: () => Navigator.of(context).pop()),
                  Expanded(child: Text('Mes tickets', textAlign: TextAlign.center, style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark))),
                  const SizedBox(width: 48),
                ],
              ),
            ),
            Expanded(
              child: FutureBuilder<List<SupportTicketModel>>(
                future: _ticketsFuture,
                builder: (context, snapshot) {
                  if (snapshot.connectionState == ConnectionState.waiting) {
                    return const Center(child: CircularProgressIndicator());
                  }
                  if (snapshot.hasError) {
                    return Center(
                      child: Padding(
                        padding: const EdgeInsets.all(32),
                        child: Column(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Text(
                              snapshot.error is ApiException ? (snapshot.error as ApiException).message : 'Impossible de charger vos tickets.',
                              textAlign: TextAlign.center,
                              style: TextStyle(color: colors.textMuted),
                            ),
                            const SizedBox(height: 12),
                            TextButton(onPressed: _reload, child: const Text('Réessayer')),
                          ],
                        ),
                      ),
                    );
                  }
                  final tickets = snapshot.data ?? const [];
                  if (tickets.isEmpty) {
                    return Center(
                      child: Padding(
                        padding: const EdgeInsets.all(32),
                        child: Column(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Icon(Icons.confirmation_number_outlined, size: 48, color: colors.textMuted),
                            const SizedBox(height: 12),
                            Text("Vous n'avez soumis aucun ticket.", style: TextStyle(color: colors.textMuted)),
                          ],
                        ),
                      ),
                    );
                  }
                  return ListView.separated(
                    padding: const EdgeInsets.all(16),
                    itemCount: tickets.length,
                    separatorBuilder: (_, _) => const SizedBox(height: 12),
                    itemBuilder: (context, index) => _buildTicketCard(colors, tickets[index]),
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTicketCard(AppColorScheme colors, SupportTicketModel ticket) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(14),
        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 8, offset: const Offset(0, 3))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text('#${ticket.ticketNumber}', style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(color: _statusColor(colors, ticket.status).withValues(alpha: 0.12), borderRadius: BorderRadius.circular(20)),
                child: Text(ticket.status.label, style: TextStyle(color: _statusColor(colors, ticket.status), fontWeight: FontWeight.w600, fontSize: 11)),
              ),
            ],
          ),
          const SizedBox(height: 6),
          Text(ticket.subject, style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark)),
          const SizedBox(height: 4),
          Text(ticket.category, style: TextStyle(fontSize: 12, color: colors.textMuted)),
          if (ticket.status == SupportTicketStatus.resolved && ticket.resolutionNotes != null) ...[
            const SizedBox(height: 8),
            Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(color: colors.inputFill, borderRadius: BorderRadius.circular(8)),
              child: Text('Réponse : ${ticket.resolutionNotes}', style: TextStyle(fontSize: 12, color: colors.textDark)),
            ),
          ],
        ],
      ),
    );
  }
}
