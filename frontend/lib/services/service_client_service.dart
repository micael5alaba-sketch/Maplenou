import 'dart:convert';
import 'dart:math';

import 'package:http/http.dart' as http;

import '../config/api_config.dart';
import '../models/service_client_model.dart';
import 'api_client.dart';
import 'token_storage.dart';

/// Client du Service client (`ServiceClient/`, API `/api/v1/support`). L'application est déjà connectée à Maplenou :
/// elle échange son jeton Maplenou contre un jeton du service client (`POST /api/v1/auth/maplenou/exchange`), sans
/// redemander le mot de passe. Le jeton n'est gardé qu'en mémoire (jamais écrit sur le disque) et renouvelé au besoin.
class ServiceClientService {
  ServiceClientService._();
  static final ServiceClientService _instance = ServiceClientService._();
  factory ServiceClientService() => _instance;

  /// Le service gratuit s'endort : la première requête peut prendre une minute.
  static const _timeout = Duration(seconds: 70);

  String? _token;
  DateTime? _expiresAt;
  Future<String>? _exchangeInFlight;

  /// Oublie le jeton (déconnexion ou changement de compte).
  void reset() {
    _token = null;
    _expiresAt = null;
  }

  // ----- Jeton -----

  Future<String> _accessToken() {
    final token = _token;
    final expires = _expiresAt;
    if (token != null && expires != null && expires.isAfter(DateTime.now().add(const Duration(minutes: 2)))) {
      return Future.value(token);
    }
    return _exchangeInFlight ??= _exchange().whenComplete(() => _exchangeInFlight = null);
  }

  Future<String> _exchange() async {
    // Un appel Maplenou authentifié force le renouvellement du jeton Maplenou s'il a expiré.
    await ApiClient().get('/api/users/me');
    final core = await TokenStorage().getAccessToken();
    if (core == null) throw ApiException(401, 'Session expirée, veuillez vous reconnecter');
    final response = await _send(
      http.Request('POST', Uri.parse('${ApiConfig.supportBaseUrl}/api/v1/auth/maplenou/exchange'))
        ..headers['Content-Type'] = 'application/json'
        ..body = jsonEncode({'coreAccessToken': core}),
    );
    final data = _decode(response) as Map<String, dynamic>;
    _token = data['accessToken'] as String;
    _expiresAt = DateTime.parse(data['expiresAt'] as String);
    return _token!;
  }

  // ----- Requêtes -----

  Future<dynamic> _call(String method, String path,
      {Map<String, String>? query, Object? body, bool retried = false}) async {
    final token = await _accessToken();
    final uri = Uri.parse('${ApiConfig.supportBaseUrl}$path').replace(queryParameters: query);
    final request = http.Request(method, uri)
      ..headers['Content-Type'] = 'application/json'
      ..headers['Authorization'] = 'Bearer $token';
    if (body != null) request.body = jsonEncode(body);
    final response = await _send(request);
    if (response.statusCode == 401 && !retried) {
      reset();
      return _call(method, path, query: query, body: body, retried: true);
    }
    return _decode(response);
  }

  Future<http.Response> _send(http.Request request) async {
    try {
      final streamed = await request.send().timeout(_timeout);
      return await http.Response.fromStream(streamed);
    } on Exception {
      throw ApiException(0, 'Le service client ne répond pas. Réessayez dans un instant.');
    }
  }

  dynamic _decode(http.Response response) {
    if (response.statusCode >= 200 && response.statusCode < 300) {
      return response.body.isEmpty ? null : jsonDecode(response.body);
    }
    String? message;
    try {
      final decoded = jsonDecode(response.body);
      if (decoded is Map<String, dynamic>) {
        message = (decoded['detail'] ?? decoded['title']) as String?;
      }
    } catch (_) {
      // corps non JSON
    }
    throw ApiException(response.statusCode, message ?? 'Erreur du service client (${response.statusCode})');
  }

  // ----- Tickets -----

  Future<List<SupportCategoryModel>> getCategories() async {
    final data = await _call('GET', '/api/v1/support/categories') as List<dynamic>;
    return data.map((j) => SupportCategoryModel.fromJson(j as Map<String, dynamic>)).toList();
  }

  Future<List<SupportTicketSummary>> getMyTickets({int size = 50}) async {
    final data = await _call('GET', '/api/v1/support/tickets', query: {'size': '$size', 'sort': 'updatedAt,desc'})
        as Map<String, dynamic>;
    return (data['items'] as List<dynamic>)
        .map((j) => SupportTicketSummary.fromJson(j as Map<String, dynamic>))
        .toList();
  }

  Future<SupportTicketDetail> createTicket({
    required String subject,
    required String description,
    required String categoryCode,
    String? orderRef,
    bool urgent = false,
  }) async {
    final data = await _call('POST', '/api/v1/support/tickets', body: {
      'subject': subject,
      'description': description,
      'categoryCode': categoryCode,
      'urgent': urgent,
      if (orderRef != null && orderRef.isNotEmpty) 'orderRef': orderRef,
    }) as Map<String, dynamic>;
    return SupportTicketDetail.fromJson(data);
  }

  Future<SupportTicketDetail> getTicket(String id) async {
    final data = await _call('GET', '/api/v1/support/tickets/$id') as Map<String, dynamic>;
    return SupportTicketDetail.fromJson(data);
  }

  Future<List<SupportMessage>> getMessages(String id) async {
    final data = await _call('GET', '/api/v1/support/tickets/$id/messages') as List<dynamic>;
    return data.map((j) => SupportMessage.fromJson(j as Map<String, dynamic>)).toList();
  }

  /// [clientMessageId] rend le renvoi idempotent : un double appui ou une coupure réseau n'envoie le message qu'une fois.
  Future<void> sendMessage(String id, String body, {required String clientMessageId}) async {
    await _call('POST', '/api/v1/support/tickets/$id/messages',
        body: {'body': body, 'clientMessageId': clientMessageId});
  }

  /// Identifiant unique pour un message en cours d'envoi (à conserver pendant les renvois).
  static String newMessageId() {
    final random = Random.secure();
    final suffix = List.generate(12, (_) => random.nextInt(36).toRadixString(36)).join();
    return 'm-${DateTime.now().microsecondsSinceEpoch}-$suffix';
  }

  Future<void> requestReopen(String id, String reason) async {
    await _call('POST', '/api/v1/support/tickets/$id/reopen-requests', body: {'reason': reason});
  }

  Future<void> rate(String id, int rating, String? comment) async {
    await _call('POST', '/api/v1/support/tickets/$id/satisfaction',
        body: {'rating': rating, if (comment != null && comment.isNotEmpty) 'comment': comment});
  }

  // ----- Notifications -----

  Future<List<SupportNotificationModel>> getNotifications({int size = 30}) async {
    final data = await _call('GET', '/api/v1/support/notifications', query: {'size': '$size'}) as Map<String, dynamic>;
    return (data['items'] as List<dynamic>)
        .map((j) => SupportNotificationModel.fromJson(j as Map<String, dynamic>))
        .toList();
  }

  Future<int> unreadCount() async {
    final data = await _call('GET', '/api/v1/support/notifications/unread-count');
    if (data is Map<String, dynamic>) return (data['unread'] as num?)?.toInt() ?? 0;
    return data is num ? data.toInt() : 0;
  }

  Future<void> markRead(String notificationId) async {
    await _call('POST', '/api/v1/support/notifications/$notificationId/read');
  }
}
