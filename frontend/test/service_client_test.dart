import 'package:flutter_test/flutter_test.dart';
import 'package:frontend/models/service_client_model.dart';
import 'package:frontend/services/notification_router.dart';
import 'package:frontend/services/service_client_service.dart';

void main() {
  test('un ticket du service client est lu depuis la reponse de l API', () {
    final t = SupportTicketSummary.fromJson({
      'id': 't1',
      'ticketNumber': 'SUP-000042',
      'subject': 'Colis non recu',
      'status': 'WAITING_FOR_CUSTOMER',
      'category': {'code': 'DELIVERY', 'name': 'Livraison'},
      'createdAt': '2026-11-12T10:00:00Z',
      'updatedAt': '2026-11-12T11:00:00Z',
    });
    expect(t.ticketNumber, 'SUP-000042');
    expect(t.categoryName, 'Livraison');
    expect(t.statusLabel, 'En attente de votre réponse');
    expect(t.isClosedLike, isFalse);
    // un statut ajouté par l'administration s'affiche tel quel
    expect(supportStatusLabel('A_RAPPELER'), 'A_RAPPELER');
  });

  test('le detail expose les actions autorisees et les references', () {
    final d = SupportTicketDetail.fromJson({
      'id': 't1',
      'ticketNumber': 'SUP-000042',
      'subject': 's',
      'description': 'd',
      'status': 'RESOLVED',
      'category': {'code': 'ORDER', 'name': 'Commande'},
      'references': {'orderRef': 'CMD-000123', 'deliveryRef': null},
      'allowedActions': ['REQUEST_REOPEN', 'RATE'],
      'pendingReopenRequest': null,
      'satisfaction': null,
      'createdAt': '2026-11-12T10:00:00Z',
    });
    expect(d.orderRef, 'CMD-000123');
    expect(d.can('REQUEST_REOPEN'), isTrue);
    expect(d.can('SEND_MESSAGE'), isFalse);
    expect(d.reopenPending, isFalse);
    expect(d.rating, isNull);
  });

  test('un message de l equipe est distingue de celui du client', () {
    SupportMessage msg(String role) => SupportMessage.fromJson({
          'id': 'm',
          'author': {'id': 'u', 'name': 'Esi', 'role': role},
          'body': 'Bonjour',
          'createdAt': '2026-11-12T10:00:00Z',
        });
    expect(msg('SUPPORT_AGENT').fromStaff, isTrue);
    expect(msg('CUSTOMER').fromStaff, isFalse);
  });

  test('chaque envoi de message a un identifiant unique et valide (idempotence)', () {
    final ids = {for (var i = 0; i < 200; i++) ServiceClientService.newMessageId()};
    expect(ids.length, 200);
    for (final id in ids) {
      expect(RegExp(r'^[A-Za-z0-9_-]{1,64}$').hasMatch(id), isTrue, reason: id);
    }
  });

  test('une notification du service client ouvre la demande', () {
    expect(
      resolveNotificationTarget({'type': 'AGENT_REPLIED', 'source': 'support', 'ticketId': 'abc'}),
      const NotificationTarget(NotificationTargetKind.supportTicket, 'abc'),
    );
    // sans identifiant de demande : pas de destination precise
    expect(resolveNotificationTarget({'type': 'AGENT_REPLIED', 'source': 'support'}), isNull);
    // les notifications de Maplenou gardent leur comportement
    expect(
      resolveNotificationTarget({'type': 'NEW_MESSAGE', 'conversationId': 'c1'}),
      const NotificationTarget(NotificationTargetKind.conversation, 'c1'),
    );
  });
}
