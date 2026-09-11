import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';

import '../theme/app_color_scheme.dart';
import '../widgets/app_logo.dart';
import '../widgets/primary_button.dart';
import 'seller_dashboard_screen.dart';

/// "Devenir Vendeur" — the form a buyer fills in to apply for a seller
/// account. Fully mocked for now: submitting just simulates a network
/// delay, then goes straight to [SellerDashboardScreen].
///
/// In the real flow, an admin is supposed to review the application before
/// it's approved — that review step doesn't exist yet (no admin app), so
/// for now submitting simulates instant approval. Swap this for the real
/// `POST /api/sellers/apply` + an actual wait-for-approval state once the
/// admin side exists (see `DEMANDES_MODIFICATIONS_BACKEND.md`).
class SellerRegistrationScreen extends StatefulWidget {
  const SellerRegistrationScreen({super.key});

  @override
  State<SellerRegistrationScreen> createState() => _SellerRegistrationScreenState();
}

class _SellerRegistrationScreenState extends State<SellerRegistrationScreen> {
  static const _defaultProductType = 'Toutes catégories acceptées';
  static const _productTypes = [
    _defaultProductType,
    'Mode',
    'Beauté',
    'Maison',
    'Électronique',
    'Alimentation',
    'Bijoux',
    'Chaussures',
    'Accessoires',
  ];

  final _formKey = GlobalKey<FormState>();
  final _shopNameController = TextEditingController();
  final _phoneController = TextEditingController();
  final _addressController = TextEditingController();

  String _productType = _defaultProductType;
  bool _isSubmitting = false;

  @override
  void dispose() {
    _shopNameController.dispose();
    _phoneController.dispose();
    _addressController.dispose();
    super.dispose();
  }

  Future<void> _handleSubmit() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() => _isSubmitting = true);
    // Simule l'appel réseau — pas encore branché sur POST /api/sellers/apply.
    // Simule aussi l'approbation admin (pas de back-office admin pour
    // l'instant) : normalement il y aurait une attente ici avant l'accès au
    // dashboard.
    await Future.delayed(const Duration(seconds: 2));
    if (!mounted) return;
    setState(() => _isSubmitting = false);

    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(const SnackBar(content: Text('Boutique créée ! Bienvenue dans votre espace vendeur.')));
    Navigator.of(context).pushReplacement(
      MaterialPageRoute(builder: (_) => const SellerDashboardScreen()),
    );
  }

  void _showTermsComingSoon() {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(const SnackBar(content: Text('Conditions Générales de Vente bientôt disponibles.')));
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return Scaffold(
      backgroundColor: colors.lightBackground,
      body: SafeArea(
        child: Column(
          children: [
            _buildHeader(context),
            Expanded(
              child: SingleChildScrollView(
                padding: const EdgeInsets.fromLTRB(20, 8, 20, 32),
                child: Form(
                  key: _formKey,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      const SizedBox(height: 12),
                      _buildTitleSection(context),
                      const SizedBox(height: 28),
                      _buildFormCard(context),
                      const SizedBox(height: 20),
                      _buildInfoCard(context),
                      const SizedBox(height: 28),
                      PrimaryButton(
                        label: 'Créer ma boutique',
                        isLoading: _isSubmitting,
                        onPressed: _handleSubmit,
                      ),
                      const SizedBox(height: 16),
                      _buildTermsText(context),
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

  Widget _buildHeader(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      child: Row(
        children: [
          IconButton(
            icon: Icon(Icons.arrow_back_rounded, color: context.colors.textDark),
            onPressed: () => Navigator.of(context).pop(),
          ),
          const Expanded(
            child: Center(
              child: AppLogo(variant: AppLogoVariant.horizontalColor, size: 34),
            ),
          ),
          const SizedBox(width: 48),
        ],
      ),
    );
  }

  Widget _buildTitleSection(BuildContext context) {
    final colors = context.colors;

    return Column(
      children: [
        Text(
          'Devenir Vendeur',
          textAlign: TextAlign.center,
          style: TextStyle(fontSize: 26, fontWeight: FontWeight.bold, color: colors.primary),
        ),
        const SizedBox(height: 10),
        Text(
          'Rejoignez notre plateforme et commencez à vendre vos produits de qualité.',
          textAlign: TextAlign.center,
          style: TextStyle(fontSize: 14, color: colors.textMuted, height: 1.4),
        ),
      ],
    );
  }

  Widget _buildFormCard(BuildContext context) {
    final colors = context.colors;

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(22),
        boxShadow: [
          BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 14, offset: const Offset(0, 6)),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildLabel(context, 'Nom de la boutique *'),
          const SizedBox(height: 8),
          TextFormField(
            controller: _shopNameController,
            decoration: _inputDecoration(context, "Ex : Les Merveilles d'Afrique"),
            textCapitalization: TextCapitalization.words,
            validator: (value) =>
                (value == null || value.trim().length < 2) ? 'Indique le nom de ta boutique.' : null,
          ),
          const SizedBox(height: 20),
          _buildLabel(context, "Numéro de téléphone de l'entreprise *"),
          const SizedBox(height: 8),
          TextFormField(
            controller: _phoneController,
            keyboardType: TextInputType.phone,
            decoration: _inputDecoration(context, 'Ex : +228 01 00 00 00'),
            validator: (value) {
              final digits = (value ?? '').replaceAll(RegExp(r'[^0-9]'), '');
              if (digits.length < 8) return 'Numéro de téléphone invalide.';
              return null;
            },
          ),
          const SizedBox(height: 20),
          _buildLabel(context, 'Type de produits *'),
          const SizedBox(height: 8),
          DropdownButtonFormField<String>(
            initialValue: _productType,
            decoration: _inputDecoration(context, null),
            items: _productTypes
                .map((type) => DropdownMenuItem(value: type, child: Text(type)))
                .toList(),
            onChanged: (value) {
              if (value != null) setState(() => _productType = value);
            },
          ),
          const SizedBox(height: 20),
          _buildLabel(context, 'Adresse de la boutique *'),
          const SizedBox(height: 8),
          TextFormField(
            controller: _addressController,
            maxLines: 3,
            decoration: _inputDecoration(context, 'Où se trouve votre boutique principale ?'),
            validator: (value) =>
                (value == null || value.trim().isEmpty) ? "Indique l'adresse de ta boutique." : null,
          ),
        ],
      ),
    );
  }

  Widget _buildLabel(BuildContext context, String label) {
    return Text(
      label,
      style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: context.colors.textDark),
    );
  }

  InputDecoration _inputDecoration(BuildContext context, String? hint) {
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
      errorBorder: border.copyWith(borderSide: BorderSide(color: colors.error)),
      focusedErrorBorder: border.copyWith(borderSide: BorderSide(color: colors.error, width: 1.6)),
    );
  }

  Widget _buildInfoCard(BuildContext context) {
    final colors = context.colors;

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: colors.primary.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: colors.primary.withValues(alpha: 0.2)),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.info_outline_rounded, color: colors.primary),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Frais de plateforme',
                  style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark),
                ),
                const SizedBox(height: 6),
                Text(
                  'Maplenou prélève une commission transparente sur chaque vente réalisée afin '
                  'de couvrir les frais de transaction, le support client et la mise en avant '
                  'de votre boutique.',
                  style: TextStyle(fontSize: 13, color: colors.textMuted, height: 1.4),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTermsText(BuildContext context) {
    final colors = context.colors;

    return RichText(
      textAlign: TextAlign.center,
      text: TextSpan(
        style: TextStyle(fontSize: 12, color: colors.textMuted, height: 1.4),
        children: [
          const TextSpan(text: 'En créant votre boutique, vous acceptez nos '),
          TextSpan(
            text: 'Conditions Générales de Vente',
            style: TextStyle(color: colors.primary, fontWeight: FontWeight.w600),
            recognizer: TapGestureRecognizer()..onTap = _showTermsComingSoon,
          ),
          const TextSpan(text: '.'),
        ],
      ),
    );
  }
}
