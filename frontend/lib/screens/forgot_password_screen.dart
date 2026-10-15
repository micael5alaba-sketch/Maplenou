import 'package:flutter/material.dart';

import '../l10n/app_localizations.dart';
import '../services/api_client.dart';
import '../services/auth_service.dart';
import '../theme/app_color_scheme.dart';
import '../widgets/app_logo.dart';
import '../widgets/primary_button.dart';
import 'reset_password_screen.dart';

/// Étape 1 du flux mot de passe oublié (`POST /api/auth/forgot-password`,
/// `auth/AuthController.java`) : saisie du numéro de téléphone, envoie un
/// code à 6 chiffres par email, puis enchaîne sur [ResetPasswordScreen].
class ForgotPasswordScreen extends StatefulWidget {
  const ForgotPasswordScreen({super.key});

  @override
  State<ForgotPasswordScreen> createState() => _ForgotPasswordScreenState();
}

class _ForgotPasswordScreenState extends State<ForgotPasswordScreen> {
  final _formKey = GlobalKey<FormState>();
  final _phoneController = TextEditingController();
  final _authService = AuthService();

  bool _isSubmitting = false;
  String? _errorMessage;

  @override
  void dispose() {
    _phoneController.dispose();
    super.dispose();
  }

  Future<void> _handleSubmit() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() {
      _isSubmitting = true;
      _errorMessage = null;
    });

    try {
      final phoneNumber = _phoneController.text.trim();
      await _authService.forgotPassword(phoneNumber: phoneNumber);
      if (!mounted) return;
      Navigator.of(context).push(
        MaterialPageRoute(builder: (_) => ResetPasswordScreen(phoneNumber: phoneNumber)),
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
    final l10n = AppLocalizations.of(context)!;

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
                  const Center(child: AppLogo(variant: AppLogoVariant.stackedColor, size: 120)),
                  const SizedBox(height: 28),
                  Text(
                    l10n.forgotPasswordTitle,
                    textAlign: TextAlign.center,
                    style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: colors.textDark),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    l10n.forgotPasswordSubtitle,
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
                    controller: _phoneController,
                    keyboardType: TextInputType.phone,
                    decoration: _inputDecoration(context, l10n.phoneNumberHint, Icons.phone_outlined),
                    validator: (value) =>
                        (value == null || value.trim().isEmpty) ? l10n.phoneNumberRequired : null,
                  ),
                  const SizedBox(height: 24),
                  PrimaryButton(
                    label: l10n.forgotPasswordSendCodeButton,
                    isLoading: _isSubmitting,
                    onPressed: _handleSubmit,
                  ),
                  const SizedBox(height: 20),
                  Center(
                    child: TextButton(
                      onPressed: () => Navigator.of(context).pop(),
                      child: Text(
                        l10n.backToLoginLink,
                        style: TextStyle(color: colors.primary, fontWeight: FontWeight.w600, fontSize: 13),
                      ),
                    ),
                  ),
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
