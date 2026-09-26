import 'package:flutter/material.dart';

import '../services/api_client.dart';
import '../services/shop_service.dart';
import '../theme/app_color_scheme.dart';
import '../widgets/app_logo.dart';
import '../widgets/primary_button.dart';
import 'seller_dashboard_screen.dart';

/// Dernière étape avant d'accéder au dashboard vendeur : un profil vendeur
/// approuvé (`SellerProfile.status = APPROVED`) doit encore créer sa
/// boutique (`POST /api/shops`) — deux entités distinctes côté backend
/// (voir `CLAUDE.md` §3). Affiché quand `ShopService.getMine()` renvoie
/// `null` (404 : pas encore de boutique).
class CreateShopScreen extends StatefulWidget {
  const CreateShopScreen({super.key});

  @override
  State<CreateShopScreen> createState() => _CreateShopScreenState();
}

class _CreateShopScreenState extends State<CreateShopScreen> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _cityController = TextEditingController();
  final _districtController = TextEditingController();
  final _descriptionController = TextEditingController();
  final _shopService = ShopService();

  bool _isSubmitting = false;
  String? _errorMessage;

  @override
  void dispose() {
    _nameController.dispose();
    _cityController.dispose();
    _districtController.dispose();
    _descriptionController.dispose();
    super.dispose();
  }

  Future<void> _handleSubmit() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() {
      _isSubmitting = true;
      _errorMessage = null;
    });
    try {
      await _shopService.create(
        name: _nameController.text.trim(),
        description: _descriptionController.text.trim().isEmpty ? null : _descriptionController.text.trim(),
        city: _cityController.text.trim(),
        district: _districtController.text.trim().isEmpty ? null : _districtController.text.trim(),
      );
      if (!mounted) return;
      Navigator.of(context).pushAndRemoveUntil(
        MaterialPageRoute(builder: (_) => const SellerDashboardScreen()),
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
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
              child: Center(child: AppLogo(variant: AppLogoVariant.horizontalColor, size: 34)),
            ),
            Expanded(
              child: SingleChildScrollView(
                padding: const EdgeInsets.fromLTRB(20, 8, 20, 32),
                child: Form(
                  key: _formKey,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      const SizedBox(height: 12),
                      Text(
                        'Créez votre boutique',
                        textAlign: TextAlign.center,
                        style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: colors.primary),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        'Votre demande vendeur est approuvée — dernière étape avant votre '
                        'tableau de bord.',
                        textAlign: TextAlign.center,
                        style: TextStyle(fontSize: 13, color: colors.textMuted, height: 1.4),
                      ),
                      const SizedBox(height: 24),
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
                        controller: _nameController,
                        textCapitalization: TextCapitalization.words,
                        decoration: _inputDecoration(context, 'Nom de la boutique'),
                        validator: (v) => (v == null || v.trim().length < 2) ? 'Indique le nom de ta boutique.' : null,
                      ),
                      const SizedBox(height: 16),
                      TextFormField(
                        controller: _cityController,
                        decoration: _inputDecoration(context, 'Ville (ex : Lomé)'),
                        validator: (v) => (v == null || v.trim().isEmpty) ? 'Indique la ville.' : null,
                      ),
                      const SizedBox(height: 16),
                      TextFormField(
                        controller: _districtController,
                        decoration: _inputDecoration(context, 'Quartier (optionnel)'),
                      ),
                      const SizedBox(height: 16),
                      TextFormField(
                        controller: _descriptionController,
                        maxLines: 3,
                        decoration: _inputDecoration(context, 'Description (optionnel)'),
                      ),
                      const SizedBox(height: 24),
                      PrimaryButton(
                        label: 'Créer ma boutique',
                        isLoading: _isSubmitting,
                        onPressed: _handleSubmit,
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ],
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
      hintStyle: TextStyle(color: colors.textMuted, fontSize: 14),
      filled: true,
      fillColor: colors.inputFill,
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      border: border,
      enabledBorder: border,
      focusedBorder: border.copyWith(borderSide: BorderSide(color: colors.primary, width: 1.6)),
    );
  }
}
