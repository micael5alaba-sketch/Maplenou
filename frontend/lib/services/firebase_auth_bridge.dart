import 'package:firebase_auth/firebase_auth.dart';

import 'api_client.dart';

/// Établit une session Firebase Auth pour le compte backend connecté, à
/// partir d'un jeton personnalisé émis par `GET /api/auth/firebase-token`
/// (voir `FirebaseTokenService` côté backend) — nécessaire pour que les
/// règles de sécurité de Firebase Realtime Database (tracking GPS livreur)
/// acceptent les lectures/écritures du client, qui ne s'authentifie
/// normalement qu'auprès du backend (JWT), jamais directement à Firebase.
class FirebaseAuthBridge {
  FirebaseAuthBridge._internal();
  static final FirebaseAuthBridge _instance = FirebaseAuthBridge._internal();
  factory FirebaseAuthBridge() => _instance;

  Future<void>? _pending;

  Future<void> ensureSignedIn() {
    if (FirebaseAuth.instance.currentUser != null) return Future.value();
    return _pending ??= _signIn().whenComplete(() => _pending = null);
  }

  Future<void> _signIn() async {
    final data = await ApiClient().get('/api/auth/firebase-token') as Map<String, dynamic>;
    final token = data['token'] as String;
    await FirebaseAuth.instance.signInWithCustomToken(token);
  }

  /// À appeler à la déconnexion du compte backend, pour ne pas garder une
  /// session Firebase liée à l'ancien utilisateur.
  Future<void> signOut() async {
    try {
      await FirebaseAuth.instance.signOut();
    } catch (_) {
      // Pas grave si Firebase n'était pas initialisé/connecté.
    }
  }
}
