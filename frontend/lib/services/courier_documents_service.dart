import '../models/courier_document_model.dart';

/// Provides the legal documents shown on [CourierDocumentsScreen]. Mocked
/// for now — no network call, and no backend entity to store/verify
/// courier documents yet (flagged as a gap).
class CourierDocumentsService {
  List<CourierDocumentModel> getDocuments() {
    return const [
      CourierDocumentModel(title: 'Permis de conduire', infoLine: 'Expire le 12 Oct 2028', status: DocumentStatus.valid),
      CourierDocumentModel(title: "Pièce d'identité", infoLine: 'Vérification requise', status: DocumentStatus.pending),
      CourierDocumentModel(title: 'Assurance véhicule', infoLine: 'Expire le 05 Jan 2027', status: DocumentStatus.valid),
      CourierDocumentModel(title: 'Carte grise', infoLine: 'Véhicule : AB-123-CD', status: DocumentStatus.valid),
    ];
  }
}
