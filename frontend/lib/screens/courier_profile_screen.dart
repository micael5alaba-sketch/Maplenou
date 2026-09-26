import 'package:flutter/material.dart';

import '../models/courier_profile_model.dart';
import '../services/api_client.dart';
import '../services/auth_service.dart';
import '../services/courier_profile_service.dart';
import '../theme/app_color_scheme.dart';
import '../utils/formatters.dart';
import '../widgets/app_drawer.dart';
import '../widgets/courier_bottom_navigation.dart';
import '../widgets/logout_button.dart';
import 'courier_dashboard_screen.dart';
import 'courier_deliveries_screen.dart';
import 'courier_documents_screen.dart';
import 'courier_history_screen.dart';
import 'courier_messages_screen.dart';
import 'courier_settings_screen.dart';
import 'courier_support_ticket_screen.dart';
import 'login_screen.dart';

/// "Profil" tab of the courier space: identity, quick stats, balance, and
/// links to history/documents/settings/support.
///
/// Backed by [CourierProfileService] (identité, solde, nombre de livraisons
/// et note réels ; l'ancienneté reste sans équivalent backend).
class CourierProfileScreen extends StatefulWidget {
  const CourierProfileScreen({super.key});

  @override
  State<CourierProfileScreen> createState() => _CourierProfileScreenState();
}

class _CourierProfileScreenState extends State<CourierProfileScreen> {
  late Future<CourierProfileModel> _profileFuture;

  @override
  void initState() {
    super.initState();
    _profileFuture = CourierProfileService().getProfile();
  }

  void _showComingSoon(String feature) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text('$feature bientôt disponible.')));
  }

  Future<void> _handleLogout() async {
    await AuthService().logout();
    if (!mounted) return;
    Navigator.of(context).pushAndRemoveUntil(
      MaterialPageRoute(builder: (_) => const LoginScreen()),
      (route) => false,
    );
  }

  void _onTabSelected(CourierTab tab) {
    if (tab == CourierTab.profile) return;
    switch (tab) {
      case CourierTab.dashboard:
        Navigator.of(context).pop();
        break;
      case CourierTab.deliveries:
        Navigator.of(context).push(MaterialPageRoute(builder: (_) => const CourierDeliveriesScreen()));
        break;
      case CourierTab.messages:
        Navigator.of(context).push(MaterialPageRoute(builder: (_) => const CourierMessagesScreen()));
        break;
      case CourierTab.profile:
        break;
    }
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return Scaffold(
      backgroundColor: colors.homeBackground,
      drawer: AppDrawer(onComingSoon: _showComingSoon),
      body: SafeArea(
        child: Column(
          children: [
            _buildHeader(context, colors),
            Expanded(
              child: FutureBuilder<CourierProfileModel>(
                future: _profileFuture,
                builder: (context, snapshot) {
                  if (snapshot.connectionState == ConnectionState.waiting) {
                    return const Center(child: CircularProgressIndicator());
                  }
                  if (snapshot.hasError || !snapshot.hasData) {
                    return _buildErrorState(colors, snapshot.error);
                  }
                  return _buildContent(context, colors, snapshot.data!);
                },
              ),
            ),
          ],
        ),
      ),
      bottomNavigationBar: CourierBottomNavigation(currentTab: CourierTab.profile, onTabSelected: _onTabSelected),
    );
  }

  Widget _buildErrorState(AppColorScheme colors, Object? error) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.error_outline_rounded, size: 48, color: colors.error),
            const SizedBox(height: 12),
            Text(
              error is ApiException ? error.message : 'Impossible de charger le profil.',
              textAlign: TextAlign.center,
              style: TextStyle(color: colors.textMuted),
            ),
            const SizedBox(height: 12),
            TextButton(
              onPressed: () => setState(() => _profileFuture = CourierProfileService().getProfile()),
              child: const Text('Réessayer'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildContent(BuildContext context, AppColorScheme colors, CourierProfileModel profile) {
    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
      children: [
        _buildIdentityCard(context, colors, profile),
        const SizedBox(height: 16),
        _buildStatsRow(colors, profile),
        const SizedBox(height: 16),
        _buildBalanceCard(context, colors, profile),
        const SizedBox(height: 20),
        Text('PARAMÈTRES & HISTORIQUE', style: TextStyle(fontSize: 12, fontWeight: FontWeight.w700, color: colors.textMuted)),
        const SizedBox(height: 10),
        _buildMenuRow(context, colors, Icons.history_rounded, 'Historique des courses',
            () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const CourierHistoryScreen()))),
        const SizedBox(height: 10),
        _buildMenuRow(context, colors, Icons.description_outlined, 'Mes Documents',
            () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const CourierDocumentsScreen()))),
        const SizedBox(height: 10),
        _buildMenuRow(context, colors, Icons.settings_outlined, "Paramètres de l'application",
            () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const CourierSettingsScreen()))),
        const SizedBox(height: 10),
        _buildMenuRow(context, colors, Icons.help_outline_rounded, 'Aide & Support',
            () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const CourierSupportTicketScreen()))),
        const SizedBox(height: 28),
        LogoutButton(onPressed: _handleLogout),
      ],
    );
  }

  Widget _buildHeader(BuildContext context, AppColorScheme colors) {
    return Container(
      color: colors.surface,
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
      child: Row(
        children: [
          IconButton(
            icon: Icon(Icons.arrow_back_rounded, color: colors.textDark),
            onPressed: () => Navigator.of(context).pushReplacement(MaterialPageRoute(builder: (_) => const CourierDashboardScreen())),
          ),
          Expanded(child: Text('Profil', textAlign: TextAlign.center, style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark))),
          const SizedBox(width: 48),
        ],
      ),
    );
  }

  Widget _buildIdentityCard(BuildContext context, AppColorScheme colors, CourierProfileModel profile) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        gradient: LinearGradient(colors: [colors.primary.withValues(alpha: 0.08), colors.surface]),
        borderRadius: BorderRadius.circular(20),
        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 12, offset: const Offset(0, 4))],
      ),
      child: Column(
        children: [
          CircleAvatar(
            radius: 36,
            backgroundColor: colors.inputFill,
            child: Text(
              profile.fullName.isNotEmpty ? profile.fullName[0] : '?',
              style: TextStyle(fontSize: 28, fontWeight: FontWeight.bold, color: colors.primary),
            ),
          ),
          const SizedBox(height: 10),
          Text(profile.fullName, style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: colors.textDark)),
          Text(profile.levelLabel, style: TextStyle(fontSize: 13, color: colors.textMuted)),
          if (profile.isVerified) ...[
            const SizedBox(height: 8),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 5),
              decoration: BoxDecoration(color: colors.primary.withValues(alpha: 0.1), borderRadius: BorderRadius.circular(20)),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(Icons.verified_rounded, size: 14, color: colors.primary),
                  const SizedBox(width: 6),
                  Text('Compte Vérifié', style: TextStyle(color: colors.primary, fontSize: 12, fontWeight: FontWeight.w600)),
                ],
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildStatsRow(AppColorScheme colors, CourierProfileModel profile) {
    return Row(
      children: [
        Expanded(
          child: _buildStatTile(
            colors,
            profile.rating != null ? profile.rating!.toStringAsFixed(1) : '—',
            'Note',
            icon: Icons.star_rounded,
            iconColor: Colors.amber,
          ),
        ),
        const SizedBox(width: 10),
        Expanded(child: _buildStatTile(colors, '${profile.deliveriesCount}', 'Livraisons')),
        const SizedBox(width: 10),
        Expanded(child: _buildStatTile(colors, profile.yearsExperience != null ? '${profile.yearsExperience}' : '—', 'Ans Exp.')),
      ],
    );
  }

  Widget _buildStatTile(AppColorScheme colors, String value, String label, {IconData? icon, Color? iconColor}) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 14),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 10, offset: const Offset(0, 3))],
      ),
      child: Column(
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              if (icon != null) ...[Icon(icon, size: 16, color: iconColor ?? colors.textDark), const SizedBox(width: 4)],
              Text(value, style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: colors.textDark)),
            ],
          ),
          const SizedBox(height: 4),
          Text(label, style: TextStyle(fontSize: 11, color: colors.textMuted)),
        ],
      ),
    );
  }

  Widget _buildBalanceCard(BuildContext context, AppColorScheme colors, CourierProfileModel profile) {
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 12, offset: const Offset(0, 4))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('SOLDE DISPONIBLE', style: TextStyle(fontSize: 12, fontWeight: FontWeight.w700, color: colors.textMuted)),
          const SizedBox(height: 6),
          Text(formatFcfa(profile.availableBalance), style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: colors.primary)),
          const SizedBox(height: 12),
          SizedBox(
            width: double.infinity,
            child: ElevatedButton.icon(
              onPressed: () => _showComingSoon('La demande de retrait'),
              style: ElevatedButton.styleFrom(
                backgroundColor: colors.primary,
                foregroundColor: Colors.white,
                elevation: 0,
                padding: const EdgeInsets.symmetric(vertical: 12),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
              ),
              icon: const Icon(Icons.account_balance_outlined, size: 18),
              label: const Text('Demander un retrait', style: TextStyle(fontWeight: FontWeight.w600)),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMenuRow(BuildContext context, AppColorScheme colors, IconData icon, String title, VoidCallback onTap) {
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
            Expanded(child: Text(title, style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark))),
            Icon(Icons.chevron_right_rounded, color: colors.textMuted),
          ],
        ),
      ),
    );
  }
}
