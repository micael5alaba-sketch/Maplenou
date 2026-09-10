import 'package:flutter/material.dart';

import '../models/role_model.dart';
import '../services/role_service.dart';
import '../services/session_service.dart';
import '../theme/app_colors.dart';
import '../widgets/app_logo.dart';
import '../widgets/primary_button.dart';
import '../widgets/role_card.dart';
import 'home_screen.dart';

/// Shown on first launch (right after the splash screen): lets the user
/// pick which profile (Acheteur / Vendeur / Livreur) they want to use the
/// app as. The choice is persisted via [SessionService] so the next time
/// the app opens, [SplashScreen] can skip straight to that profile's home
/// screen instead of showing this screen again.
class ProfileSelectionScreen extends StatefulWidget {
  const ProfileSelectionScreen({super.key});

  @override
  State<ProfileSelectionScreen> createState() => _ProfileSelectionScreenState();
}

class _ProfileSelectionScreenState extends State<ProfileSelectionScreen> {
  final List<RoleModel> _roles = RoleService().getRoles();

  // Id of the currently selected role, or null if none was picked yet.
  String? _selectedRoleId;

  void _selectRole(String roleId) {
    setState(() => _selectedRoleId = roleId);
  }

  Future<void> _handleNext() async {
    final roleId = _selectedRoleId!;
    await SessionService().saveSelectedRoleId(roleId);
    if (!mounted) return;

    // Only the "Acheteur" profile has its home screen built so far.
    if (roleId == 'buyer') {
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(builder: (_) => const HomeScreen()),
      );
      return;
    }

    final selectedRole = _roles.firstWhere((role) => role.id == roleId);
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text('Espace "${selectedRole.title}" bientôt disponible.')));
  }

  @override
  Widget build(BuildContext context) {
    final isRoleSelected = _selectedRoleId != null;

    return Scaffold(
      backgroundColor: AppColors.lightBackground,
      body: Column(
        children: [
          _buildHeader(context),
          Expanded(
            child: SafeArea(
              top: false,
              child: Column(
                children: [
                  const SizedBox(height: 20),
                  const Text(
                    'Choisissez votre profil',
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      fontSize: 15,
                      fontWeight: FontWeight.bold,
                      color: AppColors.textDark,
                    ),
                  ),
                  const SizedBox(height: 8),
                  const Padding(
                    padding: EdgeInsets.symmetric(horizontal: 32),
                    child: Text(
                      'Sélectionnez le rôle qui correspond le mieux à votre utilisation de Maplenou.',
                      textAlign: TextAlign.center,
                      style: TextStyle(fontSize: 12, color: AppColors.textMuted, height: 1.4),
                    ),
                  ),
                  const SizedBox(height: 24),
                  Expanded(
                    child: ListView.separated(
                      padding: const EdgeInsets.symmetric(horizontal: 24),
                      itemCount: _roles.length,
                      separatorBuilder: (_, _) => const SizedBox(height: 16),
                      itemBuilder: (context, index) {
                        final role = _roles[index];
                        return RoleCard(
                          role: role,
                          isSelected: role.id == _selectedRoleId,
                          onTap: () => _selectRole(role.id),
                        );
                      },
                    ),
                  ),
                  Padding(
                    padding: const EdgeInsets.fromLTRB(24, 16, 24, 20),
                    child: PrimaryButton(
                      label: 'Suivant',
                      backgroundColor: const Color.fromARGB(255, 24, 58, 14),
                      onPressed: isRoleSelected ? _handleNext : null,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  /// Gradient brand-green band, edge-to-edge behind the status bar,
  /// showing the white horizontal logo. Also carries a soft decorative
  /// circle for a bit of visual polish.
  Widget _buildHeader(BuildContext context) {
    return DecoratedBox(
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [AppColors.primary, AppColors.primaryDark],
        ),
        borderRadius: BorderRadius.only(
          bottomLeft: Radius.circular(32),
          bottomRight: Radius.circular(32),
        ),
      ),
      child: ClipRRect(
        borderRadius: const BorderRadius.only(
          bottomLeft: Radius.circular(32),
          bottomRight: Radius.circular(32),
        ),
        child: Stack(
          children: [
            Positioned(
              top: -30,
              right: -30,
              child: Container(
                width: 140,
                height: 140,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: Colors.white.withValues(alpha: 0.08),
                ),
              ),
            ),
            SafeArea(
              bottom: false,
              child: Padding(
                padding: const EdgeInsets.symmetric(vertical: 14),
                child: Center(
                  child: AppLogo(variant: AppLogoVariant.horizontalWhite, size: 72),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
