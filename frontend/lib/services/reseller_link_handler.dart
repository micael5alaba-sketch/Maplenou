import 'package:flutter/material.dart';

import '../models/product_model.dart';
import '../navigation.dart';
import '../screens/product_details_screen.dart';
import 'product_service.dart';
import 'reseller_service.dart';

/// Lien partenaire ouvert sur le web (`/?ref=code`, voir `GET /r/{code}` côté backend) : enregistre le
/// clic (le jeton d'attribution est conservé pour la commande), puis ouvre la fiche du produit une fois
/// l'application démarrée. Toute erreur est ignorée : un lien cassé ne doit jamais gêner l'ouverture.
Future<void> handleResellerRef(String code) async {
  try {
    final slug = await ResellerService().recordClick(code);
    if (slug == null) return;
    // Laisse l'écran de démarrage (session, onboarding) terminer avant d'ouvrir le produit.
    await Future<void>.delayed(const Duration(seconds: 3));
    final detail = await ProductService().getProductDetail(slug);
    final navigator = navigatorKey.currentState;
    if (navigator == null) return;
    navigator.push(MaterialPageRoute(
      builder: (_) => ProductDetailsScreen(product: ProductModel.fromSummaryJson(detail)),
    ));
  } catch (_) {
    // Lien invalide, hors ligne ou produit retiré : on reste sur l'écran courant.
  }
}
