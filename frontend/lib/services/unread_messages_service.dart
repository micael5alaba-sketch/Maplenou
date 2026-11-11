import 'dart:async';

import 'package:flutter/foundation.dart';

import 'api_client.dart';

/// Nombre de messages non lus de l'utilisateur connecté (`GET /api/conversations/unread-count`),
/// partagé par toute l'app : sert aux pastilles rouges de la messagerie (profil acheteur/vendeur,
/// onglet Messages du livreur). Mis à jour toutes les 30 s, à l'arrivée d'une notification de
/// message et après la lecture d'une conversation — un vendeur voit tout de suite qu'un client
/// attend sa réponse.
class UnreadMessagesService extends ChangeNotifier {
  UnreadMessagesService._();

  static final UnreadMessagesService instance = UnreadMessagesService._();

  static const _interval = Duration(seconds: 30);

  int _count = 0;
  Timer? _timer;

  int get count => _count;

  /// Démarre la mise à jour périodique (idempotent). À appeler une fois connecté.
  void start() {
    if (_timer != null) return;
    refresh();
    _timer = Timer.periodic(_interval, (_) => refresh());
  }

  /// Arrête la mise à jour et remet le compteur à zéro (déconnexion).
  void stop() {
    _timer?.cancel();
    _timer = null;
    if (_count != 0) {
      _count = 0;
      notifyListeners();
    }
  }

  Future<void> refresh() async {
    try {
      final data = await ApiClient().get('/api/conversations/unread-count') as Map<String, dynamic>;
      final count = (data['count'] as num?)?.toInt() ?? 0;
      if (count != _count) {
        _count = count;
        notifyListeners();
      }
    } catch (_) {
      // Silencieux : le compteur se met à jour au prochain passage.
    }
  }
}
