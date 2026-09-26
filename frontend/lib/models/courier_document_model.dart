import '../utils/formatters.dart';

/// Miroir de `CourierDocumentType` (`courier/CourierDocumentType.java`).
enum CourierDocumentType { drivingLicense, idCard, vehicleInsurance, vehicleRegistration }

extension CourierDocumentTypeLabel on CourierDocumentType {
  String get label {
    switch (this) {
      case CourierDocumentType.drivingLicense:
        return 'Permis de conduire';
      case CourierDocumentType.idCard:
        return "Pièce d'identité";
      case CourierDocumentType.vehicleInsurance:
        return 'Assurance véhicule';
      case CourierDocumentType.vehicleRegistration:
        return 'Carte grise';
    }
  }

  String get apiValue {
    switch (this) {
      case CourierDocumentType.drivingLicense:
        return 'DRIVING_LICENSE';
      case CourierDocumentType.idCard:
        return 'ID_CARD';
      case CourierDocumentType.vehicleInsurance:
        return 'VEHICLE_INSURANCE';
      case CourierDocumentType.vehicleRegistration:
        return 'VEHICLE_REGISTRATION';
    }
  }

  static CourierDocumentType fromApiValue(String value) {
    return CourierDocumentType.values.firstWhere((t) => t.apiValue == value);
  }
}

/// Miroir de `CourierDocumentStatus` (`courier/CourierDocumentStatus.java`), plus
/// [notSubmitted] côté app pour les types dont le livreur n'a encore rien envoyé
/// (le backend ne renvoie tout simplement pas de ligne pour eux).
enum DocumentStatus { notSubmitted, pending, valid, rejected, expired }

/// One legal document row in [CourierDocumentsScreen] ("Permis de conduire",
/// "Pièce d'identité"...). Miroir de `CourierDocumentResponse`
/// (`courier/dto/CourierDocumentResponse.java`), avec une entrée "non envoyé"
/// générée côté app pour les types absents de la réponse backend.
class CourierDocumentModel {
  final String? id;
  final CourierDocumentType type;
  final String? fileUrl;
  final DocumentStatus status;
  final DateTime? expiresAt;
  final String? rejectionReason;

  const CourierDocumentModel({
    this.id,
    required this.type,
    this.fileUrl,
    required this.status,
    this.expiresAt,
    this.rejectionReason,
  });

  String get title => type.label;

  String get infoLine {
    switch (status) {
      case DocumentStatus.notSubmitted:
        return 'Aucun document envoyé';
      case DocumentStatus.pending:
        return 'Vérification en cours';
      case DocumentStatus.valid:
        return expiresAt != null ? 'Expire le ${formatShortDate(expiresAt!)}' : 'Validé';
      case DocumentStatus.rejected:
        return rejectionReason != null ? 'Refusé : $rejectionReason' : 'Refusé';
      case DocumentStatus.expired:
        return expiresAt != null ? 'Expiré le ${formatShortDate(expiresAt!)}' : 'Expiré';
    }
  }

  factory CourierDocumentModel.notSubmitted(CourierDocumentType type) {
    return CourierDocumentModel(type: type, status: DocumentStatus.notSubmitted);
  }

  factory CourierDocumentModel.fromJson(Map<String, dynamic> json) {
    return CourierDocumentModel(
      id: json['id'] as String,
      type: CourierDocumentTypeLabel.fromApiValue(json['type'] as String),
      fileUrl: json['fileUrl'] as String?,
      status: DocumentStatus.values.byName((json['status'] as String).toLowerCase()),
      expiresAt: json['expiresAt'] != null ? DateTime.parse(json['expiresAt'] as String) : null,
      rejectionReason: json['rejectionReason'] as String?,
    );
  }
}
