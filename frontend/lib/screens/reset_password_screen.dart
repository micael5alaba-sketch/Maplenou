import 'package:flutter/material.dart';

import '../l10n/app_localizations.dart';
import '../services/api_client.dart';
import '../services/auth_service.dart';
import '../theme/app_color_scheme.dart';
import '../widgets/app_logo.dart';
import '../widgets/primary_button.dart';

/// Étape 2 du flux mot de passe oublié (`POST /api/auth/reset-password`,
/// `auth/AuthController.java`) : saisie du code reçu par email et du
/// nouveau mot de passe. [phoneNumber] vient de [ForgotPasswordScreen].
class ResetPasswordScreen extends StatefulWidget {
  final String phoneNumber;

  const ResetPasswordScreen({super.key, required this.phoneNumber});

  @override
  State<ResetPasswordScreen> createState() => _ResetPasswordScreenState();
}

class _ResetPasswordScreenState extends State<ResetPasswordScreen> {
  final _formKey = GlobalKey<FormState>();
  final _codeController = TextEditingController();
  final _newPasswordController = TextEditingController();
  final _authService = AuthService();

  bool _isSubmitting = false;
  bool _obscurePassword = true;
  bool _success = false;
  String? _errorMessage;

  @override
  void dispose() {
    _codeController.dispose();
    _newPasswordController.dispose();
    super.dispose();
  }

  Future<void> _handleSubmit() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() {
      _isSubmitting = true;
      _errorMessage = null;
    });

    try {
      await _authService.resetPassword(
        phoneNumber: widget.phoneNumber,
        code: _codeController.text.trim(),
        newPassword: _newPasswordController.text,
      );
      if (!mounted) return;
      setState(() => _success = true);
    } on ApiException catch (e) {
      setState(() => _errorMessage = e.message);
    } finally {
      if (mounted) setState(() => _isSubmitting = false);
    }
  }

  void _backToLogin() {
    // Login -> ForgotPassword -> ResetPassword : on dépile les deux derniers
    // pour revenir exactement à l'écran de connexion.
    Navigator.of(context).pop();
    Navigator.of(context).pop();
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
            child: _success ? _buildSuccess(colors, l10n) : _buildForm(colors, l10n),
          ),
        ),
      ),
    );
  }

  Widget _buildSuccess(AppColorScheme colors, AppLocalizations l10n) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Center(
          child: Icon(Icons.check_circle_rounded, color: colors.primary, size: 72),
        ),
        const SizedBox(height: 20),
        Text(
          l10n.resetPasswordSuccessTitle,
          textAlign: TextAlign.center,
          style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: colors.textDark),
        ),
        const SizedBox(height: 8),
        Text(
          l10n.resetPasswordSuccessSubtitle,
          textAlign: TextAlign.center,
          style: TextStyle(fontSize: 13, color: colors.textMuted, height: 1.4),
        ),
        const SizedBox(height: 28),
        PrimaryButton(label: l10n.backToLoginLink, onPressed: _backToLogin),
      ],
    );
  }

  Widget _buildForm(AppColorScheme colors, AppLocalizations l10n) {
    return Form(
      key: _formKey,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Center(child: AppLogo(variant: AppLogoVariant.stackedColor, size: 120)),
          const SizedBox(height: 28),
          Text(
            l10n.resetPasswordTitle,
            textAlign: TextAlign.center,
            style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: colors.textDark),
          ),
          const SizedBox(height: 8),
          Text(
            l10n.resetPasswordSubtitle,
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
            controller: _codeController,
            keyboardType: TextInputType.number,
            maxLength: 6,
            decoration: _inputDecoration(context, l10n.otpCodeHint, Icons.pin_outlined),
            validator: (value) => (value == null || value.trim().isEmpty) ? l10n.otpCodeRequired : null,
          ),
          const SizedBox(height: 16),
          TextFormField(
            controller: _newPasswordController,
            obscureText: _obscurePassword,
            decoration: _inputDecoration(context, l10n.newPasswordHint, Icons.lock_outline_rounded).copyWith(
              helperText: l10n.newPasswordRequirements,
              helperStyle: TextStyle(color: colors.textMuted, fontSize: 11),
              suffixIcon: IconButton(
                icon: Icon(
                  _obscurePassword ? Icons.visibility_outlined : Icons.visibility_off_outlined,
                  color: colors.textMuted,
                ),
                onPressed: () => setState(() => _obscurePassword = !_obscurePassword),
              ),
            ),
            validator: (value) => (value == null || value.isEmpty) ? l10n.passwordRequired : null,
          ),
          const SizedBox(height: 24),
          PrimaryButton(
            label: l10n.resetPasswordButton,
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
