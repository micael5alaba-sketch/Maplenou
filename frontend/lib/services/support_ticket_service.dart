import '../models/support_ticket_model.dart';
import 'api_client.dart';

/// `support/SupportTicketController.java` — tickets de support structurés
/// (livreur/acheteur), avec un vrai numéro de suivi généré côté backend
/// (`support_ticket_seq`) : remplace l'ancien système qui envoyait juste un
/// message de messagerie avec un numéro inventé côté app, jamais
/// retrouvable ensuite.
class SupportTicketService {
  final _client = ApiClient();

  Future<SupportTicketModel> submitTicket({
    required String category,
    String? relatedDeliveryId,
    required String subject,
    required String description,
    String? photoUrl,
  }) async {
    final data = await _client.post('/api/support-tickets', body: {
      'category': category,
      'subject': subject,
      'description': description,
      if (relatedDeliveryId != null) 'relatedDeliveryId': relatedDeliveryId,
      if (photoUrl != null) 'photoUrl': photoUrl,
    }) as Map<String, dynamic>;
    return SupportTicketModel.fromJson(data);
  }

  Future<List<SupportTicketModel>> getMyTickets({int size = 50}) async {
    final data = await _client.get('/api/support-tickets/mine', query: {'size': '$size'}) as Map<String, dynamic>;
    final content = data['content'] as List<dynamic>? ?? [];
    return content.map((json) => SupportTicketModel.fromJson(json as Map<String, dynamic>)).toList();
  }
}
