import 'package:flutter/material.dart';

import '../theme/app_color_scheme.dart';
import '../widgets/app_logo.dart';

/// "À propos de Maplenou" — reached from [ProfileScreen]'s support section.
class AboutScreen extends StatelessWidget {
  const AboutScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return Scaffold(
      backgroundColor: colors.homeBackground,
      body: SafeArea(
        child: Column(
          children: [
            Container(
              color: colors.surface,
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
              child: Row(
                children: [
                  IconButton(icon: Icon(Icons.arrow_back_rounded, color: colors.textDark), onPressed: () => Navigator.of(context).pop()),
                  Expanded(child: Text('À propos', textAlign: TextAlign.center, style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark))),
                  const SizedBox(width: 48),
                ],
              ),
            ),
            Expanded(
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(24),
                child: Column(
                  children: [
                    const AppLogo(variant: AppLogoVariant.horizontalColor, size: 48),
                    const SizedBox(height: 24),
                    Text(
                      "Maplenou connecte acheteurs et vendeurs locaux au Togo et au Bénin, avec "
                      "livraison interne assurée par nos propres livreurs. Chaque boutique est "
                      "gérée par un artisan ou commerçant réel, vérifié avant de pouvoir vendre "
                      "sur la plateforme.",
                      textAlign: TextAlign.center,
                      style: TextStyle(color: colors.textDark, height: 1.5),
                    ),
                    const SizedBox(height: 32),
                    _buildRow(colors, Icons.storefront_outlined, 'Marketplace multi-vendeurs', 'Bijoux, bois, céramique, textile et plus, faits main localement.'),
                    _buildRow(colors, Icons.local_shipping_outlined, 'Livraison interne', 'Suivi de commande en temps réel, sans transporteur tiers.'),
                    _buildRow(colors, Icons.payments_outlined, 'Paiement Mobile Money', 'T-Money, Flooz et carte bancaire via FedaPay.'),
                    _buildRow(colors, Icons.shield_outlined, 'Sécurité du compte', 'Authentification à deux facteurs disponible sur tout compte.'),
                    const SizedBox(height: 24),
                    Text('© ${DateTime.now().year} Maplenou. Tous droits réservés.', style: TextStyle(color: colors.textMuted, fontSize: 12)),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildRow(AppColorScheme colors, IconData icon, String title, String subtitle) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 18),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, color: colors.primary),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
                const SizedBox(height: 2),
                Text(subtitle, style: TextStyle(fontSize: 12, color: colors.textMuted, height: 1.3)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
