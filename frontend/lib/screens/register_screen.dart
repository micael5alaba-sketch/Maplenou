import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../l10n/app_localizations.dart';
import '../services/api_client.dart';
import '../services/app_router.dart';
import '../services/auth_service.dart';
import '../theme/app_color_scheme.dart';
import '../widgets/app_logo.dart';
import '../widgets/primary_button.dart';
import 'seller_registration_screen.dart';

/// Indicatif + longueur exacte attendue pour les deux marchés cibles
/// (cahier des charges §1 : Togo, Bénin) — un numéro à 9 chiffres sans
/// indicatif passait auparavant sans aucun contrôle (signalé dans le
/// rapport de test du 21/10/2026). Togo : +228, 8 chiffres. Bénin : +229,
/// 10 chiffres (nouveau plan de numérotation depuis le 30/11/2024, préfixe
/// "01" obligatoire inclus dans ces 10 chiffres).
class _PhoneCountry {
  final String flag;
  final String dialCode;
  final String name;
  final int digitCount;

  const _PhoneCountry({required this.flag, required this.dialCode, required this.name, required this.digitCount});
}

const List<_PhoneCountry> _phoneCountries = [
  _PhoneCountry(flag: '🇹🇬', dialCode: '+228', name: 'Togo', digitCount: 8),
  _PhoneCountry(flag: '🇧🇯', dialCode: '+229', name: 'Bénin', digitCount: 10),
];

/// Inscription (`POST /api/auth/register`, `auth/AuthController.java`).
/// Le compte est Acheteur par défaut, sauf si [intendedRole] vaut 'courier' :
/// dans ce cas le compte est créé directement avec le rôle `DELIVERY_AGENT`
/// (`accountType` envoyé au backend, cahier des charges §3.3 — livreur
/// indépendant). 'seller' enchaîne directement sur la candidature vendeur
/// ([SellerRegistrationScreen]) plutôt que d'obliger à la retrouver plus
/// tard dans les paramètres du profil.
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
  final _confirmPasswordController = TextEditingController();
  final _authService = AuthService();

  _PhoneCountry _selectedCountry = _phoneCountries.first;
  bool _isSubmitting = false;
  bool _obscurePassword = true;
  bool _obscureConfirmPassword = true;
  String? _errorMessage;

  @override
  void dispose() {
    _fullNameController.dispose();
    _phoneController.dispose();
    _passwordController.dispose();
    _confirmPasswordController.dispose();
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
        phoneNumber: _selectedCountry.dialCode + _phoneController.text.trim(),
        password: _passwordController.text,
        accountType: widget.intendedRole == 'courier' ? 'DELIVERY_AGENT' : null,
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
    final l10n = AppLocalizations.of(context)!;

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
                    l10n.registerTitle,
                    textAlign: TextAlign.center,
                    style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: colors.textDark),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    l10n.registerSubtitle,
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
                    decoration: _inputDecoration(context, l10n.fullNameHint, Icons.person_outline_rounded),
                    validator: (value) =>
                        (value == null || value.trim().length < 2) ? l10n.fullNameRequired : null,
                  ),
                  const SizedBox(height: 16),
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Container(
                        height: 52,
                        padding: const EdgeInsets.symmetric(horizontal: 10),
                        decoration: BoxDecoration(
                          color: colors.inputFill,
                          borderRadius: BorderRadius.circular(14),
                          border: Border.all(color: colors.border),
                        ),
                        child: DropdownButtonHideUnderline(
                          child: DropdownButton<_PhoneCountry>(
                            value: _selectedCountry,
                            items: _phoneCountries
                                .map((c) => DropdownMenuItem(
                                      value: c,
                                      child: Text('${c.flag} ${c.dialCode}', style: TextStyle(color: colors.textDark)),
                                    ))
                                .toList(),
                            onChanged: (country) {
                              if (country == null) return;
                              setState(() => _selectedCountry = country);
                              _formKey.currentState?.validate();
                            },
                          ),
                        ),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: TextFormField(
                          controller: _phoneController,
                          keyboardType: TextInputType.phone,
                          inputFormatters: [
                            FilteringTextInputFormatter.digitsOnly,
                            LengthLimitingTextInputFormatter(_selectedCountry.digitCount),
                          ],
                          decoration: _inputDecoration(context, l10n.phoneNumberHint, Icons.phone_outlined),
                          validator: (value) {
                            final digits = (value ?? '').replaceAll(RegExp(r'[^0-9]'), '');
                            if (digits.length != _selectedCountry.digitCount) {
                              return l10n.phoneNumberDigitsRequired(_selectedCountry.name, _selectedCountry.digitCount);
                            }
                            return null;
                          },
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    controller: _passwordController,
                    obscureText: _obscurePassword,
                    decoration: _inputDecoration(context, l10n.passwordHint, Icons.lock_outline_rounded).copyWith(
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
                        return l10n.passwordRule;
                      }
                      if (!RegExp(r'^(?=.*[A-Z])(?=.*[0-9]).{8,}$').hasMatch(value)) {
                        return l10n.passwordRule;
                      }
                      return null;
                    },
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    controller: _confirmPasswordController,
                    obscureText: _obscureConfirmPassword,
                    decoration: _inputDecoration(context, l10n.confirmPasswordHint, Icons.lock_outline_rounded).copyWith(
                      suffixIcon: IconButton(
                        icon: Icon(
                          _obscureConfirmPassword ? Icons.visibility_outlined : Icons.visibility_off_outlined,
                          color: colors.textMuted,
                        ),
                        onPressed: () => setState(() => _obscureConfirmPassword = !_obscureConfirmPassword),
                      ),
                    ),
                    validator: (value) {
                      if (value != _passwordController.text) {
                        return l10n.confirmPasswordMismatch;
                      }
                      return null;
                    },
                  ),
                  const SizedBox(height: 24),
                  PrimaryButton(
                    label: l10n.registerButton,
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
