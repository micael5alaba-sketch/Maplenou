import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:image_picker/image_picker.dart';

import 'api_client.dart';

/// Upload signé Cloudinary : le backend ne reçoit jamais les octets de
/// l'image (`media/MediaService.java`) — on récupère une signature via
/// `POST /api/media/upload-signature`, puis on envoie le fichier
/// directement à Cloudinary avec cette signature.
///
/// Deux garde-fous appliqués à chaque envoi (robustesse réseau mobile ouest-
/// africain, voir cahier des charges §3.4 "poids de l'application") :
///   - redimensionnement max 1920px + compression 85% dès la sélection
///     (`maxWidth`/`maxHeight`/`imageQuality` d'`image_picker`), pour ne
///     jamais envoyer une photo brute de 10+ Mo prise par l'appareil ;
///   - rejet avant envoi si le fichier dépasse [_maxFileSizeBytes] malgré
///     tout (ex: galerie avec une image déjà très lourde) plutôt que de
///     laisser l'upload échouer à mi-chemin sur une connexion lente.
class MediaUploadService {
  static const int _maxFileSizeBytes = 5 * 1024 * 1024; // 5 Mo

  final _client = ApiClient();
  final _picker = ImagePicker();

  /// Ouvre l'appareil photo, puis upload le cliché sur Cloudinary.
  /// Renvoie `null` si l'utilisateur annule la prise de photo.
  Future<String?> capturePhotoAndUpload({String? folder}) async {
    final photo = await _picker.pickImage(
      source: ImageSource.camera,
      imageQuality: 85,
      maxWidth: 1920,
      maxHeight: 1920,
    );
    if (photo == null) return null;
    return _uploadFile(photo, folder: folder);
  }

  /// Ouvre la galerie, puis upload la photo choisie sur Cloudinary.
  /// Renvoie `null` si l'utilisateur annule la sélection.
  Future<String?> pickImageAndUpload({String? folder}) async {
    final photo = await _picker.pickImage(
      source: ImageSource.gallery,
      imageQuality: 85,
      maxWidth: 1920,
      maxHeight: 1920,
    );
    if (photo == null) return null;
    return _uploadFile(photo, folder: folder);
  }

  Future<String> _uploadFile(XFile photo, {String? folder}) async {
    // `dart:io`'s File n'existe pas sur Flutter Web : on passe exclusivement par
    // les méthodes cross-platform de XFile (length/readAsBytes), seules supportées
    // à la fois sur mobile natif et web.
    final sizeBytes = await photo.length();
    if (sizeBytes > _maxFileSizeBytes) {
      throw ApiException(413, 'Photo trop lourde (max 5 Mo) — réessaie avec une autre image.');
    }

    final signatureData = await _client.post(
      '/api/media/upload-signature',
      body: folder != null ? {'folder': folder} : null,
    ) as Map<String, dynamic>;

    final cloudName = signatureData['cloudName'] as String;
    final apiKey = signatureData['apiKey'] as String;
    final timestamp = signatureData['timestamp'];
    final signature = signatureData['signature'] as String;
    final signedFolder = signatureData['folder'] as String?;

    final fileBytes = await photo.readAsBytes();

    final uploadUri = Uri.parse('https://api.cloudinary.com/v1_1/$cloudName/image/upload');
    final request = http.MultipartRequest('POST', uploadUri)
      ..fields['api_key'] = apiKey
      ..fields['timestamp'] = '$timestamp'
      ..fields['signature'] = signature
      ..fields.addAll(signedFolder != null ? {'folder': signedFolder} : {})
      ..files.add(http.MultipartFile.fromBytes('file', fileBytes, filename: photo.name));

    final streamedResponse = await request.send().timeout(const Duration(seconds: 30));
    final response = await http.Response.fromStream(streamedResponse);

    if (response.statusCode != 200) {
      throw ApiException(response.statusCode, "Échec de l'envoi de la photo à Cloudinary");
    }

    final uploadResult = jsonDecode(response.body) as Map<String, dynamic>;
    return uploadResult['secure_url'] as String;
  }
}
