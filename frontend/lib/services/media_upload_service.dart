import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:image_picker/image_picker.dart';

import 'api_client.dart';

/// Upload signé Cloudinary : le backend ne reçoit jamais les octets de
/// l'image (`media/MediaService.java`) — on récupère une signature via
/// `POST /api/media/upload-signature`, puis on envoie le fichier
/// directement à Cloudinary avec cette signature.
class MediaUploadService {
  final _client = ApiClient();
  final _picker = ImagePicker();

  /// Ouvre l'appareil photo, puis upload le cliché sur Cloudinary.
  /// Renvoie `null` si l'utilisateur annule la prise de photo.
  Future<String?> capturePhotoAndUpload({String? folder}) async {
    final photo = await _picker.pickImage(source: ImageSource.camera, imageQuality: 85);
    if (photo == null) return null;
    return _uploadFile(photo, folder: folder);
  }

  /// Ouvre la galerie, puis upload la photo choisie sur Cloudinary.
  /// Renvoie `null` si l'utilisateur annule la sélection.
  Future<String?> pickImageAndUpload({String? folder}) async {
    final photo = await _picker.pickImage(source: ImageSource.gallery, imageQuality: 85);
    if (photo == null) return null;
    return _uploadFile(photo, folder: folder);
  }

  Future<String> _uploadFile(XFile photo, {String? folder}) async {
    final signatureData = await _client.post(
      '/api/media/upload-signature',
      body: folder != null ? {'folder': folder} : null,
    ) as Map<String, dynamic>;

    final cloudName = signatureData['cloudName'] as String;
    final apiKey = signatureData['apiKey'] as String;
    final timestamp = signatureData['timestamp'];
    final signature = signatureData['signature'] as String;
    final signedFolder = signatureData['folder'] as String?;

    final uploadUri = Uri.parse('https://api.cloudinary.com/v1_1/$cloudName/image/upload');
    final request = http.MultipartRequest('POST', uploadUri)
      ..fields['api_key'] = apiKey
      ..fields['timestamp'] = '$timestamp'
      ..fields['signature'] = signature
      ..fields.addAll(signedFolder != null ? {'folder': signedFolder} : {})
      ..files.add(await http.MultipartFile.fromPath('file', photo.path));

    final streamedResponse = await request.send().timeout(const Duration(seconds: 30));
    final response = await http.Response.fromStream(streamedResponse);

    if (response.statusCode != 200) {
      throw ApiException(response.statusCode, "Échec de l'envoi de la photo à Cloudinary");
    }

    final uploadResult = jsonDecode(response.body) as Map<String, dynamic>;
    return uploadResult['secure_url'] as String;
  }
}
