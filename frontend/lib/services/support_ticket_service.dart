/// Handles submitting a courier support ticket from
/// [CourierSupportTicketScreen].
///
/// Mocked for now — no network call. There is no structured support-ticket
/// entity on the backend yet: only a generic `SUPPORT`-type `Conversation`
/// (see `MODELE_DONNEES.md`), which has no category, linked order, or
/// ticket number. Flagged as a gap.
class SupportTicketService {
  Future<String> submitTicket({
    required String category,
    String? relatedOrder,
    required String subject,
    required String description,
  }) async {
    await Future.delayed(const Duration(seconds: 1, milliseconds: 500));
    final year = DateTime.now().year;
    final sequence = (DateTime.now().millisecondsSinceEpoch % 9000) + 1000;
    return 'TKT-$year-$sequence';
  }
}
