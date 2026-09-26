import 'package:flutter/material.dart';

import '../models/role_model.dart';
import '../models/user_model.dart';
import '../services/role_service.dart';
import '../services/session_service.dart';
import '../theme/app_color_scheme.dart';
import '../theme/app_colors.dart';
import '../widgets/app_logo.dart';
import '../widgets/primary_button.dart';
import '../services/app_router.dart';
import '../widgets/role_card.dart';
import 'home_screen.dart';
import 'seller_registration_screen.dart';

/// Affichée juste après la connexion quand le compte a plusieurs interfaces
/// possibles (Acheteur + Vendeur approuvé) : l'utilisateur choisit celle
/// qu'il veut ouvrir maintenant, et ce choix est mémorisé par
/// [SessionService] pour les prochaines ouvertures de l'app (voir
/// [AppRouter]).
///
/// Le rôle Livreur n'apparaît jamais ici : c'est un compte dédié
/// (`role = DELIVERY_AGENT`, attribué par un admin), jamais une bascule sur
/// un compte acheteur/vendeur (voir `CLAUDE.md` §3).
class ProfileSelectionScreen extends StatefulWidget {
  final UserModel user;

  const ProfileSelectionScreen({super.key, required this.user});

  @override
  State<ProfileSelectionScreen> createState() => _ProfileSelectionScreenState();
}

class _ProfileSelectionScreenState extends State<ProfileSelectionScreen> {
  late final List<RoleModel> _roles = RoleService()
      .getRoles()
      .where((role) => role.id == 'buyer' || (role.id == 'seller' && widget.user.isApprovedSeller))
      .toList();

  String? _selectedRoleId;

  void _selectRole(String roleId) {
    setState(() => _selectedRoleId = roleId);
  }

  Future<void> _handleNext() async {
    final roleId = _selectedRoleId!;
    await SessionService().saveSelectedRoleId(roleId);
    if (!mounted) return;

    final target = roleId == 'seller' ? await AppRouter.resolveSellerEntryScreen() : const HomeScreen();
    if (!mounted) return;

    Navigator.of(context).pushReplacement(MaterialPageRoute(builder: (_) => target));
  }

  @override
  Widget build(BuildContext context) {
    final isRoleSelected = _selectedRoleId != null;

    return Scaffold(
      backgroundColor: context.colors.lightBackground,
      body: Column(
        children: [
          _buildHeader(context),
          Expanded(
            child: SafeArea(
              top: false,
              child: Column(
                children: [
                  const SizedBox(height: 20),
                  Text(
                    'Choisissez votre profil',
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      fontSize: 15,
                      fontWeight: FontWeight.bold,
                      color: context.colors.textDark,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 32),
                    child: Text(
                      'Votre compte peut utiliser Maplenou en Acheteur ou en Vendeur — choisissez '
                      'par lequel commencer, vous pourrez changer plus tard.',
                      textAlign: TextAlign.center,
                      style: TextStyle(fontSize: 12, color: context.colors.textMuted, height: 1.4),
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
                  if (!widget.user.isApprovedSeller && !widget.user.hasPendingSellerApplication)
                    Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 24),
                      child: TextButton(
                        onPressed: () => Navigator.of(context).push(
                          MaterialPageRoute(builder: (_) => const SellerRegistrationScreen()),
                        ),
                        child: const Text('Devenir vendeur'),
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
