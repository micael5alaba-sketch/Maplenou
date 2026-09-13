enum DocumentStatus { valid, pending }

/// One legal document row in [CourierDocumentsScreen] ("Permis de conduire",
/// "Pièce d'identité"...).
class CourierDocumentModel {
  final String title;
  final String infoLine;
  final DocumentStatus status;

  const CourierDocumentModel({
    required this.title,
    required this.infoLine,
    required this.status,
  });
}
