import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../services/api_client.dart';
import '../services/auth_service.dart';
import '../services/locale_controller.dart';
import '../services/theme_controller.dart';
import '../services/user_service.dart';
import '../theme/app_color_scheme.dart';
import '../widgets/logout_button.dart';
import 'courier_pin_settings_screen.dart';
import 'login_screen.dart';

/// "Paramètres" — delivery preferences, appearance, notifications,
/// language and PIN. Le thème, la langue, les notifications et le code PIN
/// sont réels (préférences locales à l'appareil — voir `ThemeController`,
/// `LocaleController`, `CourierPinService`) ; seule la zone de livraison
/// reste un espace réservé (pas de notion de zone assignée au livreur côté
/// backend pour l'instant).
class CourierSettingsScreen extends StatefulWidget {
  const CourierSettingsScreen({super.key});

  @override
  State<CourierSettingsScreen> createState() => _CourierSettingsScreenState();
}

class _CourierSettingsScreenState extends State<CourierSettingsScreen> {
  static const _notificationsPrefKey = 'courier_notifications_enabled';

  bool _notificationsEnabled = true;

  @override
  void initState() {
    super.initState();
    _loadNotificationPreference();
  }

  Future<void> _loadNotificationPreference() async {
    final prefs = await SharedPreferences.getInstance();
    if (!mounted) return;
    setState(() => _notificationsEnabled = prefs.getBool(_notificationsPrefKey) ?? true);
  }

  Future<void> _setNotificationsEnabled(bool value) async {
    setState(() => _notificationsEnabled = value);
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_notificationsPrefKey, value);
  }

  void _showComingSoon(String feature) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text('$feature bientôt disponible.')));
  }

  Future<void> _pickLanguage() async {
    final colors = context.colors;
    await showModalBottomSheet<void>(
      context: context,
      backgroundColor: colors.surface,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (sheetContext) {
        return SafeArea(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const SizedBox(height: 12),
              Text('Langue', style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
              const SizedBox(height: 8),
              for (final language in AppLanguage.values)
                ValueListenableBuilder<AppLanguage>(
                  valueListenable: localeController,
                  builder: (context, current, _) {
                    return ListTile(
                      title: Text(language.label, style: TextStyle(color: colors.textDark)),
                      subtitle: language.isAvailable ? null : Text('Pas encore disponible', style: TextStyle(color: colors.textMuted, fontSize: 12)),
                      trailing: current == language ? Icon(Icons.check_rounded, color: colors.primary) : null,
                      onTap: () async {
                        Navigator.of(sheetContext).pop();
                        if (!language.isAvailable) {
                          _showComingSoon('Le passage en ${language.label}');
                          return;
                        }
                        await localeController.setLanguage(language);
                      },
                    );
                  },
                ),
              const SizedBox(height: 8),
            ],
          ),
        );
      },
    );
  }

  void _openPinSettings() {
    Navigator.of(context).push(MaterialPageRoute(builder: (_) => const CourierPinSettingsScreen()));
  }

  Future<void> _handleLogout() async {
    await AuthService().logout();
    if (!mounted) return;
    Navigator.of(context).pushAndRemoveUntil(
      MaterialPageRoute(builder: (_) => const LoginScreen()),
      (route) => false,
    );
  }

  Future<void> _handleDeleteAccount() async {
    final colors = context.colors;
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Supprimer mon compte'),
        content: const Text(
          'Cette action est définitive : vous perdrez l\'accès à votre compte livreur. Continuer ?',
        ),
        actions: [
          TextButton(onPressed: () => Navigator.of(dialogContext).pop(false), child: const Text('Annuler')),
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(true),
            child: Text('Supprimer', style: TextStyle(color: colors.error)),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;

    try {
      await UserService().deleteAccount();
    } on ApiException catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(SnackBar(content: Text(e.message)));
      return;
    }

    await AuthService().logout();
    if (!mounted) return;
    Navigator.of(context).pushAndRemoveUntil(
      MaterialPageRoute(builder: (_) => const LoginScreen()),
      (route) => false,
    );
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
                    onChanged: _setNotificationsEnabled,
                  ),
                  const SizedBox(height: 10),
                  ValueListenableBuilder<AppLanguage>(
                    valueListenable: localeController,
                    builder: (context, language, _) {
                      return _buildRow(colors, Icons.language_rounded, 'Langue', subtitle: language.label, onTap: _pickLanguage);
                    },
                  ),
                  const SizedBox(height: 20),
                  _buildSectionLabel(colors, 'SÉCURITÉ'),
                  const SizedBox(height: 8),
                  _buildRow(colors, Icons.dialpad_rounded, 'Changer le code PIN', subtitle: 'Sécurité de connexion', onTap: _openPinSettings),
                  const SizedBox(height: 20),
                  _buildSectionLabel(colors, 'COMPTE'),
                  const SizedBox(height: 8),
                  _buildRow(
                    colors,
                    Icons.delete_outline_rounded,
                    'Supprimer mon compte',
                    subtitle: 'Suppression définitive de l\'accès à votre compte',
                    accentColor: colors.error,
                    onTap: _handleDeleteAccount,
                  ),
                  const SizedBox(height: 28),
                  LogoutButton(onPressed: _handleLogout),
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

  Widget _buildRow(AppColorScheme colors, IconData icon, String title, {required String subtitle, required VoidCallback onTap, Color? accentColor}) {
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
            Icon(icon, color: accentColor ?? colors.primary, size: 20),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title, style: TextStyle(fontWeight: FontWeight.w600, color: accentColor ?? colors.textDark)),
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
