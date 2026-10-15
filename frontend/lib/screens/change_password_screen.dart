import 'package:flutter/material.dart';

import '../services/api_client.dart';
import '../theme/app_color_scheme.dart';
import '../widgets/primary_button.dart';

/// `PATCH /api/users/me/password` (`user/UserController.java`) : changement
/// de mot de passe pour un compte déjà connecté (distinct du flux "mot de
/// passe oublié", qui lui passe par un code email — voir ForgotPasswordScreen).
class ChangePasswordScreen extends StatefulWidget {
  const ChangePasswordScreen({super.key});

  @override
  State<ChangePasswordScreen> createState() => _ChangePasswordScreenState();
}

class _ChangePasswordScreenState extends State<ChangePasswordScreen> {
  final _formKey = GlobalKey<FormState>();
  final _currentPasswordController = TextEditingController();
  final _newPasswordController = TextEditingController();
  final _apiClient = ApiClient();

  bool _isSubmitting = false;
  bool _obscureCurrent = true;
  bool _obscureNew = true;
  String? _errorMessage;

  @override
  void dispose() {
    _currentPasswordController.dispose();
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
      await _apiClient.patch('/api/users/me/password', body: {
        'currentPassword': _currentPasswordController.text,
        'newPassword': _newPasswordController.text,
      });
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Mot de passe modifié avec succès.')),
      );
      Navigator.of(context).pop();
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
      appBar: AppBar(
        backgroundColor: colors.surface,
        elevation: 0,
        title: Text('Changer le mot de passe', style: TextStyle(color: colors.textDark)),
        iconTheme: IconThemeData(color: colors.textDark),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
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
                  controller: _currentPasswordController,
                  obscureText: _obscureCurrent,
                  decoration: _inputDecoration(context, 'Mot de passe actuel').copyWith(
                    suffixIcon: IconButton(
                      icon: Icon(
                        _obscureCurrent ? Icons.visibility_outlined : Icons.visibility_off_outlined,
                        color: colors.textMuted,
                      ),
                      onPressed: () => setState(() => _obscureCurrent = !_obscureCurrent),
                    ),
                  ),
                  validator: (value) => (value == null || value.isEmpty) ? 'Mot de passe requis.' : null,
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _newPasswordController,
                  obscureText: _obscureNew,
                  decoration: _inputDecoration(context, 'Nouveau mot de passe').copyWith(
                    helperText: 'Au moins 8 caractères, une majuscule et un chiffre.',
                    helperStyle: TextStyle(color: colors.textMuted, fontSize: 11),
                    suffixIcon: IconButton(
                      icon: Icon(
                        _obscureNew ? Icons.visibility_outlined : Icons.visibility_off_outlined,
                        color: colors.textMuted,
                      ),
                      onPressed: () => setState(() => _obscureNew = !_obscureNew),
                    ),
                  ),
                  validator: (value) => (value == null || value.isEmpty) ? 'Mot de passe requis.' : null,
                ),
                const SizedBox(height: 24),
                PrimaryButton(
                  label: 'Enregistrer',
                  isLoading: _isSubmitting,
                  onPressed: _handleSubmit,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  InputDecoration _inputDecoration(BuildContext context, String hint) {
    final colors = context.colors;
    final border = OutlineInputBorder(
      borderRadius: BorderRadius.circular(14),
      borderSide: BorderSide(color: colors.border),
    );

    return InputDecoration(
      hintText: hint,
      prefixIcon: Icon(Icons.lock_outline_rounded, color: colors.textMuted),
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
