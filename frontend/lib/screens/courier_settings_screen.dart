import 'package:flutter/material.dart';

import '../services/theme_controller.dart';
import '../theme/app_color_scheme.dart';
import '../widgets/logout_button.dart';

/// "Paramètres" — delivery preferences, appearance, notifications,
/// language and PIN. Mocked for now: only the theme toggle is real
/// (shared [themeController]), the rest are local UI state / placeholders.
class CourierSettingsScreen extends StatefulWidget {
  const CourierSettingsScreen({super.key});

  @override
  State<CourierSettingsScreen> createState() => _CourierSettingsScreenState();
}

class _CourierSettingsScreenState extends State<CourierSettingsScreen> {
  bool _notificationsEnabled = true;

  void _showComingSoon(String feature) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text('$feature bientôt disponible.')));
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return Scaffold(
      backgroundColor: colors.homeBackground,
      body: SafeArea(
        child: Column(
          children: [
            _buildHeader(colors),
            Expanded(
              child: ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  Text('Paramètres', style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: colors.textDark)),
                  const SizedBox(height: 4),
                  Text('Gérez vos préférences de livraison et de compte.', style: TextStyle(fontSize: 13, color: colors.textMuted)),
                  const SizedBox(height: 20),
                  _buildSectionLabel(colors, 'PRÉFÉRENCES DE LIVRAISON'),
                  const SizedBox(height: 8),
                  _buildRow(colors, Icons.location_on_outlined, 'Zone de livraison', subtitle: 'Adidogomé, Agoè, Kégué', onTap: () => _showComingSoon('La zone de livraison')),
                  const SizedBox(height: 20),
                  _buildSectionLabel(colors, 'APPARENCE & SYSTÈME'),
                  const SizedBox(height: 8),
                  ValueListenableBuilder<ThemeMode>(
                    valueListenable: themeController,
                    builder: (context, mode, _) {
                      return _buildSwitchRow(
                        colors,
                        Icons.dark_mode_outlined,
                        'Mode sombre',
                        subtitle: "Thème de l'application",
                        value: mode == ThemeMode.dark,
                        onChanged: (v) => themeController.setThemeMode(v ? ThemeMode.dark : ThemeMode.light),
                      );
                    },
                  ),
                  const SizedBox(height: 10),
                  _buildSwitchRow(
                    colors,
                    Icons.notifications_none_rounded,
                    'Notifications',
                    subtitle: 'Nouvelles courses & alertes',
                    value: _notificationsEnabled,
                    onChanged: (v) => setState(() => _notificationsEnabled = v),
                  ),
                  const SizedBox(height: 10),
                  _buildRow(colors, Icons.language_rounded, 'Langue', subtitle: 'Français', onTap: () => _showComingSoon('Le changement de langue')),
                  const SizedBox(height: 20),
                  _buildSectionLabel(colors, 'SÉCURITÉ'),
                  const SizedBox(height: 8),
                  _buildRow(colors, Icons.dialpad_rounded, 'Changer le code PIN', subtitle: 'Sécurité de connexion', onTap: () => _showComingSoon('Le changement de code PIN')),
                  const SizedBox(height: 28),
                  LogoutButton(onPressed: () => _showComingSoon('La déconnexion')),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader(AppColorScheme colors) {
    return Container(
      color: colors.surface,
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
      child: Row(
        children: [
          IconButton(
            icon: Icon(Icons.arrow_back_rounded, color: colors.textDark),
            onPressed: () => Navigator.of(context).pop(),
          ),
          Expanded(child: Text('Paramètres', textAlign: TextAlign.center, style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark))),
          const SizedBox(width: 48),
        ],
      ),
    );
  }

  Widget _buildSectionLabel(AppColorScheme colors, String label) {
    return Text(label, style: TextStyle(fontSize: 11, fontWeight: FontWeight.w700, color: colors.textMuted));
  }

  Widget _buildRow(AppColorScheme colors, IconData icon, String title, {required String subtitle, required VoidCallback onTap}) {
    return InkWell(
      borderRadius: BorderRadius.circular(16),
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        decoration: BoxDecoration(
          color: colors.surface,
          borderRadius: BorderRadius.circular(16),
          boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 10, offset: const Offset(0, 3))],
        ),
        child: Row(
          children: [
            Icon(icon, color: colors.primary, size: 20),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title, style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark)),
                  Text(subtitle, style: TextStyle(fontSize: 12, color: colors.textMuted)),
                ],
              ),
            ),
            Icon(Icons.chevron_right_rounded, color: colors.textMuted),
          ],
        ),
      ),
    );
  }

  Widget _buildSwitchRow(AppColorScheme colors, IconData icon, String title, {required String subtitle, required bool value, required ValueChanged<bool> onChanged}) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 10, offset: const Offset(0, 3))],
      ),
      child: Row(
        children: [
          Icon(icon, color: colors.primary, size: 20),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark)),
                Text(subtitle, style: TextStyle(fontSize: 12, color: colors.textMuted)),
              ],
            ),
          ),
          Switch(value: value, onChanged: onChanged, activeThumbColor: colors.primary),
        ],
      ),
    );
  }
}
