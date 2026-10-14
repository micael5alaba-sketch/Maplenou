import 'package:flutter/material.dart';

import '../l10n/app_localizations.dart';
import '../screens/login_screen.dart';
import '../screens/my_orders_screen.dart';
import '../screens/profile_screen.dart';
import '../services/auth_service.dart';
import '../theme/app_color_scheme.dart';
import 'app_logo.dart';

/// Side menu shared by every screen with a hamburger icon in its header.
class AppDrawer extends StatelessWidget {
  final ValueChanged<String> onComingSoon;

  const AppDrawer({super.key, required this.onComingSoon});

  @override
  Widget build(BuildContext context) {
    final textColor = context.colors.textDark;
    final l10n = AppLocalizations.of(context)!;

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
              title: Text(l10n.drawerMyOrders),
              onTap: () {
                Navigator.of(context).pop();
                Navigator.of(context).push(MaterialPageRoute(builder: (_) => const MyOrdersScreen()));
              },
            ),
            ListTile(
              leading: Icon(Icons.settings_outlined, color: textColor),
              title: Text(l10n.drawerSettings),
              onTap: () {
                Navigator.of(context).pop();
                Navigator.of(context).push(MaterialPageRoute(builder: (_) => const ProfileScreen()));
              },
            ),
            ListTile(
              leading: Icon(Icons.logout_rounded, color: textColor),
              title: Text(l10n.drawerLogout),
              onTap: () => _handleLogout(context),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _handleLogout(BuildContext context) async {
    Navigator.of(context).pop();
    await AuthService().logout();
    if (!context.mounted) return;
    Navigator.of(context).pushAndRemoveUntil(
      MaterialPageRoute(builder: (_) => const LoginScreen()),
      (route) => false,
    );
  }
}
