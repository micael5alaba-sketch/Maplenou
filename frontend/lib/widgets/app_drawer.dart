import 'package:flutter/material.dart';

import '../theme/app_colors.dart';
import 'app_logo.dart';

/// Side menu shared by every screen with a hamburger icon in its header.
class AppDrawer extends StatelessWidget {
  final ValueChanged<String> onComingSoon;

  const AppDrawer({super.key, required this.onComingSoon});

  @override
  Widget build(BuildContext context) {
    return Drawer(
      child: SafeArea(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Padding(
              padding: EdgeInsets.all(20),
              child: AppLogo(variant: AppLogoVariant.horizontalColor, size: 40),
            ),
            const Divider(height: 1),
            ListTile(
              leading: const Icon(Icons.receipt_long_outlined, color: AppColors.textDark),
              title: const Text('Mes commandes'),
              onTap: () {
                Navigator.of(context).pop();
                onComingSoon('Mes commandes');
              },
            ),
            ListTile(
              leading: const Icon(Icons.settings_outlined, color: AppColors.textDark),
              title: const Text('Paramètres'),
              onTap: () {
                Navigator.of(context).pop();
                onComingSoon('Paramètres');
              },
            ),
            ListTile(
              leading: const Icon(Icons.logout_rounded, color: AppColors.textDark),
              title: const Text('Déconnexion'),
              onTap: () {
                Navigator.of(context).pop();
                onComingSoon('La déconnexion');
              },
            ),
          ],
        ),
      ),
    );
  }
}
