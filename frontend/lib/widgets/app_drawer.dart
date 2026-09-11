import 'package:flutter/material.dart';

import '../theme/app_color_scheme.dart';
import 'app_logo.dart';

/// Side menu shared by every screen with a hamburger icon in its header.
class AppDrawer extends StatelessWidget {
  final ValueChanged<String> onComingSoon;

  const AppDrawer({super.key, required this.onComingSoon});

  @override
  Widget build(BuildContext context) {
    final textColor = context.colors.textDark;

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
              leading: Icon(Icons.receipt_long_outlined, color: textColor),
              title: const Text('Mes commandes'),
              onTap: () {
                Navigator.of(context).pop();
                onComingSoon('Mes commandes');
              },
            ),
            ListTile(
              leading: Icon(Icons.settings_outlined, color: textColor),
              title: const Text('Paramètres'),
              onTap: () {
                Navigator.of(context).pop();
                onComingSoon('Paramètres');
              },
            ),
            ListTile(
              leading: Icon(Icons.logout_rounded, color: textColor),
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
