import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';

import '../services/api_client.dart';
import '../services/app_router.dart';
import '../services/auth_service.dart';
import '../theme/app_color_scheme.dart';
import '../widgets/app_logo.dart';
import '../widgets/primary_button.dart';
import 'signup_intent_screen.dart';

/// Connexion (`POST /api/auth/login`, `auth/AuthController.java`). Gère
/// aussi le défi 2FA TOTP (`LoginResponse.mfaRequired`) quand le compte l'a
/// activée : un second champ apparaît pour saisir le code, validé via
/// `POST /api/auth/2fa/verify-login`.
class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _phoneController = TextEditingController();
  final _passwordController = TextEditingController();
  final _totpController = TextEditingController();
  final _authService = AuthService();

  bool _isSubmitting = false;
  bool _obscurePassword = true;
  String? _mfaToken;
  String? _errorMessage;

  @override
  void dispose() {
    _phoneController.dispose();
    _passwordController.dispose();
    _totpController.dispose();
    super.dispose();
  }

  Future<void> _handleSubmit() async {
    if (_mfaToken == null && !_formKey.currentState!.validate()) return;

    setState(() {
      _isSubmitting = true;
      _errorMessage = null;
    });

    try {
      if (_mfaToken != null) {
        final user = await _authService.verifyMfaLogin(
          mfaToken: _mfaToken!,
          code: _totpController.text.trim(),
        );
        if (!mounted) return;
        final home = await AppRouter.resolveHomeScreen(user);
        if (!mounted) return;
        Navigator.of(context).pushAndRemoveUntil(
          MaterialPageRoute(builder: (_) => home),
          (route) => false,
        );
        return;
      }

      final result = await _authService.login(
        phoneNumber: _phoneController.text.trim(),
        password: _passwordController.text,
      );

      if (result.mfaRequired) {
        setState(() => _mfaToken = result.mfaToken);
        return;
      }

      if (!mounted) return;
      final home = await AppRouter.resolveHomeScreen(result.user!);
      if (!mounted) return;
      Navigator.of(context).pushAndRemoveUntil(
        MaterialPageRoute(builder: (_) => home),
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
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.symmetric(horizontal: 28, vertical: 24),
            child: Form(
              key: _formKey,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const Center(child: AppLogo(variant: AppLogoVariant.stackedColor, size: 140)),
                  const SizedBox(height: 32),
                  Text(
                    _mfaToken == null ? 'Connexion' : 'Vérification en deux étapes',
                    textAlign: TextAlign.center,
                    style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: colors.textDark),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    _mfaToken == null
                        ? 'Connectez-vous avec votre numéro de téléphone.'
                        : "Entrez le code à 6 chiffres de votre application d'authentification.",
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
                      child: Text(
                        _errorMessage!,
                        style: TextStyle(color: colors.error, fontSize: 13),
                      ),
                    ),
                    const SizedBox(height: 16),
                  ],
                  if (_mfaToken == null) ...[
                    TextFormField(
                      controller: _phoneController,
                      keyboardType: TextInputType.phone,
                      decoration: _inputDecoration(context, 'Numéro de téléphone', Icons.phone_outlined),
                      validator: (value) =>
                          (value == null || value.trim().isEmpty) ? 'Numéro de téléphone requis.' : null,
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
                      validator: (value) =>
                          (value == null || value.isEmpty) ? 'Mot de passe requis.' : null,
                    ),
                  ] else
                    TextFormField(
                      controller: _totpController,
                      keyboardType: TextInputType.number,
                      maxLength: 6,
                      decoration: _inputDecoration(context, 'Code à 6 chiffres', Icons.pin_outlined),
                    ),
                  const SizedBox(height: 24),
                  PrimaryButton(
                    label: _mfaToken == null ? 'Se connecter' : 'Vérifier',
                    isLoading: _isSubmitting,
                    onPressed: _handleSubmit,
                  ),
                  if (_mfaToken == null) ...[
                    const SizedBox(height: 20),
                    _buildRegisterLink(context),
                  ],
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildRegisterLink(BuildContext context) {
    final colors = context.colors;
    return RichText(
      textAlign: TextAlign.center,
      text: TextSpan(
        style: TextStyle(fontSize: 13, color: colors.textMuted),
        children: [
          const TextSpan(text: "Pas encore de compte ? "),
          TextSpan(
            text: "S'inscrire",
            style: TextStyle(color: colors.primary, fontWeight: FontWeight.w600),
            recognizer: TapGestureRecognizer()
              ..onTap = () => Navigator.of(context).push(
                    MaterialPageRoute(builder: (_) => const SignupIntentScreen()),
                  ),
          ),
        ],
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
