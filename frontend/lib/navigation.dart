import 'package:flutter/material.dart';

/// Clé de navigation globale, utilisée par [ApiClient] pour rediriger vers
/// l'écran de connexion depuis un service (sans BuildContext) quand une
/// session expire (401 après échec du rafraîchissement du token).
final GlobalKey<NavigatorState> navigatorKey = GlobalKey<NavigatorState>();
