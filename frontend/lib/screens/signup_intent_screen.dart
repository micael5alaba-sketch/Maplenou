import 'package:flutter/material.dart';

import '../models/role_model.dart';
import '../services/role_service.dart';
import '../theme/app_color_scheme.dart';
import '../theme/app_colors.dart';
import '../widgets/app_logo.dart';
import '../widgets/primary_button.dart';
import '../widgets/role_card.dart';
import 'register_screen.dart';

/// Première étape de l'inscription : la personne indique si elle vient
/// acheter, vendre ou livrer — dans les trois cas un compte est créé
/// ensuite sur [RegisterScreen], ce choix décide ce qui se passe juste après
/// l'inscription (accueil Acheteur, candidature Vendeur, ou dashboard
/// Livreur en attente de validation de ses documents).
///
/// Un livreur indépendant peut donc s'inscrire lui-même ici (cahier des
/// charges §3.3) — ce n'est qu'une fois ses documents validés par l'admin
/// qu'il reçoit de vraies livraisons (voir `CourierDashboardScreen`). Les
/// livreurs internes à l'entreprise restent créés directement par l'admin
/// (back-office), sans passer par cet écran.
class SignupIntentScreen extends StatefulWidget {
  const SignupIntentScreen({super.key});

  @override
  State<SignupIntentScreen> createState() => _SignupIntentScreenState();
}

class _SignupIntentScreenState extends State<SignupIntentScreen> {
  late final List<RoleModel> _roles = RoleService().getRoles();

  String? _selectedRoleId;

  void _selectRole(String roleId) {
    setState(() => _selectedRoleId = roleId);
  }

  void _handleNext() {
    Navigator.of(context).push(
      MaterialPageRoute(builder: (_) => RegisterScreen(intendedRole: _selectedRoleId!)),
    );
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
                    'Pourquoi venez-vous sur Maplenou ?',
                    textAlign: TextAlign.center,
                    style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: context.colors.textDark),
                  ),
                  const SizedBox(height: 8),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 32),
                    child: Text(
                      'Vous pourrez toujours faire les deux plus tard depuis le même compte — '
                      'ce choix sert juste à préparer les prochaines étapes.',
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

  Widget _buildHeader(BuildContext context) {
    return DecoratedBox(
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [AppColors.primary, AppColors.primaryDark],
        ),
        borderRadius: BorderRadius.only(bottomLeft: Radius.circular(32), bottomRight: Radius.circular(32)),
      ),
      child: ClipRRect(
        borderRadius: const BorderRadius.only(bottomLeft: Radius.circular(32), bottomRight: Radius.circular(32)),
        child: Stack(
          children: [
            Positioned(
              top: -30,
              right: -30,
              child: Container(
                width: 140,
                height: 140,
                decoration: BoxDecoration(shape: BoxShape.circle, color: Colors.white.withValues(alpha: 0.08)),
              ),
            ),
            SafeArea(
              bottom: false,
              child: Padding(
                padding: const EdgeInsets.symmetric(vertical: 14),
                child: Row(
                  children: [
                    IconButton(
                      icon: const Icon(Icons.arrow_back_rounded, color: Colors.white),
                      onPressed: () => Navigator.of(context).pop(),
                    ),
                    const Expanded(
                      child: Center(child: AppLogo(variant: AppLogoVariant.horizontalWhite, size: 60)),
                    ),
                    const SizedBox(width: 48),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
