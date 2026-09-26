import '../models/courier_document_model.dart';
import 'api_client.dart';

/// `courier/CourierDocumentController.java` — documents légaux du livreur
/// connecté (`GET/POST /api/delivery-agents/me/documents`).
class CourierDocumentsService {
  final _client = ApiClient();

  /// Toujours les 4 types dans le même ordre, même quand le livreur n'a pas
  /// encore envoyé un document donné (le backend ne renvoie alors aucune
  /// ligne pour ce type — complété ici par [CourierDocumentModel.notSubmitted]).
  Future<List<CourierDocumentModel>> getDocuments() async {
    final data = await _client.get('/api/delivery-agents/me/documents') as List<dynamic>;
    final submitted = data.map((json) => CourierDocumentModel.fromJson(json as Map<String, dynamic>)).toList();

    return CourierDocumentType.values.map((type) {
      return submitted.firstWhere(
        (d) => d.type == type,
        orElse: () => CourierDocumentModel.notSubmitted(type),
      );
    }).toList();
  }

  Future<CourierDocumentModel> submit({
    required CourierDocumentType type,
    required String fileUrl,
    DateTime? expiresAt,
  }) async {
    final data = await _client.post('/api/delivery-agents/me/documents', body: {
      'type': type.apiValue,
      'fileUrl': fileUrl,
      if (expiresAt != null) 'expiresAt': expiresAt.toIso8601String().substring(0, 10),
    }) as Map<String, dynamic>;
    return CourierDocumentModel.fromJson(data);
  }
}
