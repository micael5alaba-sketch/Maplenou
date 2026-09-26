import 'package:flutter/material.dart';

import '../services/api_client.dart';
import '../services/app_router.dart';
import '../services/auth_service.dart';
import '../theme/app_color_scheme.dart';
import '../widgets/app_logo.dart';
import '../widgets/primary_button.dart';
import 'seller_registration_screen.dart';

/// Inscription (`POST /api/auth/register`, `auth/AuthController.java`).
/// Le compte est toujours créé Acheteur par défaut — pas de rôle séparé
/// côté backend (voir `CLAUDE.md` §3). [intendedRole] ne change que la
/// navigation juste après l'inscription : 'seller' enchaîne directement sur
/// la candidature vendeur ([SellerRegistrationScreen]) plutôt que d'obliger
/// à la retrouver plus tard dans les paramètres du profil.
class RegisterScreen extends StatefulWidget {
  final String intendedRole;

  const RegisterScreen({super.key, this.intendedRole = 'buyer'});

  @override
  State<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends State<RegisterScreen> {
  final _formKey = GlobalKey<FormState>();
  final _fullNameController = TextEditingController();
  final _phoneController = TextEditingController();
  final _passwordController = TextEditingController();
  final _authService = AuthService();

  bool _isSubmitting = false;
  bool _obscurePassword = true;
  String? _errorMessage;

  @override
  void dispose() {
    _fullNameController.dispose();
    _phoneController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _handleSubmit() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() {
      _isSubmitting = true;
      _errorMessage = null;
    });

    try {
      final user = await _authService.register(
        fullName: _fullNameController.text.trim(),
        phoneNumber: _phoneController.text.trim(),
        password: _passwordController.text,
      );
      if (!mounted) return;

      final Widget next;
      if (widget.intendedRole == 'seller') {
        next = const SellerRegistrationScreen(standalone: true);
      } else {
        next = await AppRouter.resolveHomeScreen(user);
      }
      if (!mounted) return;
      Navigator.of(context).pushAndRemoveUntil(
        MaterialPageRoute(builder: (_) => next),
        (route) => false,
      );
    } on ApiException catch (e) {
      setState(() => _errorMessage = e.message);
    } finally {
      if (mounted) setState(() => _isSubmitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return Scaffold(
      backgroundColor: colors.lightBackground,
      appBar: AppBar(backgroundColor: colors.lightBackground, elevation: 0, foregroundColor: colors.textDark),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.symmetric(horizontal: 28, vertical: 12),
            child: Form(
              key: _formKey,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const Center(child: AppLogo(variant: AppLogoVariant.stackedColor, size: 110)),
                  const SizedBox(height: 24),
                  Text(
                    'Créer un compte',
                    textAlign: TextAlign.center,
                    style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: colors.textDark),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Rejoignez Maplenou en quelques secondes.',
                    textAlign: TextAlign.center,
                    style: TextStyle(fontSize: 13, color: colors.textMuted, height: 1.4),
                  ),
                  const SizedBox(height: 28),
                  if (_errorMessage != null) ...[
                    Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: colors.error.withValues(alpha: 0.08),
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(color: colors.error.withValues(alpha: 0.3)),
                      ),
                      child: Text(_errorMessage!, style: TextStyle(color: colors.error, fontSize: 13)),
                    ),
                    const SizedBox(height: 16),
                  ],
                  TextFormField(
                    controller: _fullNameController,
                    textCapitalization: TextCapitalization.words,
                    decoration: _inputDecoration(context, 'Nom complet', Icons.person_outline_rounded),
                    validator: (value) =>
                        (value == null || value.trim().length < 2) ? 'Nom complet requis.' : null,
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    controller: _phoneController,
                    keyboardType: TextInputType.phone,
                    decoration: _inputDecoration(context, 'Numéro de téléphone', Icons.phone_outlined),
                    validator: (value) {
                      final digits = (value ?? '').replaceAll(RegExp(r'[^0-9]'), '');
                      if (digits.length < 8) return 'Numéro de téléphone invalide.';
                      return null;
                    },
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    controller: _passwordController,
                    obscureText: _obscurePassword,
                    decoration: _inputDecoration(context, 'Mot de passe', Icons.lock_outline_rounded).copyWith(
                      suffixIcon: IconButton(
                        icon: Icon(
                          _obscurePassword ? Icons.visibility_outlined : Icons.visibility_off_outlined,
                          color: colors.textMuted,
                        ),
                        onPressed: () => setState(() => _obscurePassword = !_obscurePassword),
                      ),
                    ),
                    validator: (value) {
                      if (value == null || value.length < 8) {
                        return 'Au moins 8 caractères, une majuscule et un chiffre.';
                      }
                      if (!RegExp(r'^(?=.*[A-Z])(?=.*[0-9]).{8,}$').hasMatch(value)) {
                        return 'Au moins 8 caractères, une majuscule et un chiffre.';
                      }
                      return null;
                    },
                  ),
                  const SizedBox(height: 24),
                  PrimaryButton(
                    label: "S'inscrire",
                    isLoading: _isSubmitting,
                    onPressed: _handleSubmit,
                  ),
                  const SizedBox(height: 12),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  InputDecoration _inputDecoration(BuildContext context, String hint, IconData icon) {
    final colors = context.colors;
    final border = OutlineInputBorder(
      borderRadius: BorderRadius.circular(14),
      borderSide: BorderSide(color: colors.border),
    );

    return InputDecoration(
      hintText: hint,
      prefixIcon: Icon(icon, color: colors.textMuted),
      hintStyle: TextStyle(color: colors.textMuted, fontSize: 14),
      filled: true,
      fillColor: colors.inputFill,
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      border: border,
      enabledBorder: border,
      focusedBorder: border.copyWith(borderSide: BorderSide(color: colors.primary, width: 1.6)),
      errorBorder: border.copyWith(borderSide: BorderSide(color: colors.error)),
      focusedErrorBorder: border.copyWith(borderSide: BorderSide(color: colors.error, width: 1.6)),
    );
  }
}
