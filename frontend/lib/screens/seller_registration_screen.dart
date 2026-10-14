import 'package:flutter/material.dart';

import '../services/api_client.dart';
import '../services/seller_application_service.dart';
import '../theme/app_color_scheme.dart';
import '../widgets/app_logo.dart';
import '../widgets/primary_button.dart';
import 'home_screen.dart';

/// "Devenir Vendeur" — le formulaire qu'un acheteur remplit pour demander
/// l'activation de son profil vendeur (`POST /api/sellers/apply`, sur ce
/// même compte — voir `CLAUDE.md` §3).
///
/// Un admin doit ensuite approuver la demande (`PATCH
/// /api/sellers/admin/{id}/status`) — pas d'approbation instantanée : il
/// n'y a pas encore de back-office admin pour ça, donc l'écran affiche
/// juste une confirmation d'envoi, pas un accès direct au dashboard.
class SellerRegistrationScreen extends StatefulWidget {
  /// true quand on arrive ici juste après l'inscription (voir
  /// [RegisterScreen]) : il n'y a alors rien à "pop" derrière, donc le
  /// bouton final ouvre l'accueil Acheteur au lieu de revenir en arrière.
  final bool standalone;

  const SellerRegistrationScreen({super.key, this.standalone = false});

  @override
  State<SellerRegistrationScreen> createState() => _SellerRegistrationScreenState();
}

class _SellerRegistrationScreenState extends State<SellerRegistrationScreen> {
  final _formKey = GlobalKey<FormState>();
  final _shopNameController = TextEditingController();
  final _applicationService = SellerApplicationService();

  String _feeCategory = 'STANDARD';
  bool _isSubmitting = false;
  bool _isSubmitted = false;
  String? _errorMessage;

  @override
  void dispose() {
    _shopNameController.dispose();
    super.dispose();
  }

  Future<void> _handleSubmit() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() {
      _isSubmitting = true;
      _errorMessage = null;
    });
    try {
      await _applicationService.apply(shopName: _shopNameController.text.trim(), feeCategory: _feeCategory);
      if (!mounted) return;
      setState(() => _isSubmitted = true);
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
            _buildHeader(context),
            Expanded(
              child: SingleChildScrollView(
                padding: const EdgeInsets.fromLTRB(20, 8, 20, 32),
                child: _isSubmitted ? _buildSuccessState(context) : _buildForm(context),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildForm(BuildContext context) {
    final colors = context.colors;

    return Form(
      key: _formKey,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const SizedBox(height: 12),
          _buildTitleSection(context),
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
          _buildFormCard(context),
          const SizedBox(height: 20),
          _buildInfoCard(context),
          const SizedBox(height: 28),
          PrimaryButton(
            label: 'Envoyer ma demande',
            isLoading: _isSubmitting,
            onPressed: _handleSubmit,
          ),
        ],
      ),
    );
  }

  Widget _buildSuccessState(BuildContext context) {
    final colors = context.colors;

    return Column(
      children: [
        const SizedBox(height: 24),
        Center(
          child: Container(
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(color: colors.primary.withValues(alpha: 0.1), shape: BoxShape.circle),
            child: Icon(Icons.hourglass_top_rounded, size: 48, color: colors.primary),
          ),
        ),
        const SizedBox(height: 20),
        Text(
          'Demande envoyée !',
          textAlign: TextAlign.center,
          style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: colors.primary),
        ),
        const SizedBox(height: 10),
        Text(
          "Votre demande pour \"${_shopNameController.text.trim()}\" est en cours d'examen par "
          "notre équipe. Vous pourrez créer votre boutique dès qu'elle sera approuvée.",
          textAlign: TextAlign.center,
          style: TextStyle(fontSize: 14, color: colors.textMuted, height: 1.4),
        ),
        const SizedBox(height: 28),
        SizedBox(
          width: double.infinity,
          child: OutlinedButton(
            onPressed: () {
              if (widget.standalone) {
                Navigator.of(context).pushAndRemoveUntil(
                  MaterialPageRoute(builder: (_) => const HomeScreen()),
                  (route) => false,
                );
              } else {
                Navigator.of(context).pop();
              }
            },
            style: OutlinedButton.styleFrom(
              foregroundColor: colors.textDark,
              side: BorderSide(color: colors.border),
              padding: const EdgeInsets.symmetric(vertical: 14),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(30)),
            ),
            child: Text(widget.standalone ? "Aller à l'accueil" : 'Retour',
                style: const TextStyle(fontWeight: FontWeight.w600)),
          ),
        ),
      ],
    );
  }

  Widget _buildHeader(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      child: Row(
        children: [
          IconButton(
            icon: Icon(Icons.arrow_back_rounded, color: context.colors.textDark),
            onPressed: () {
              if (widget.standalone) {
                Navigator.of(context).pushAndRemoveUntil(
                  MaterialPageRoute(builder: (_) => const HomeScreen()),
                  (route) => false,
                );
              } else {
                Navigator.of(context).pop();
              }
            },
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
          _buildLabel(context, 'Catégorie de prix de tes produits *'),
          const SizedBox(height: 8),
          _buildCategoryOption(
            context,
            value: 'STANDARD',
            title: 'Standard',
            subtitle: 'Produits entre 500 et 10 000 FCFA — 500 FCFA/mois',
          ),
          const SizedBox(height: 10),
          _buildCategoryOption(
            context,
            value: 'HIGH_PRICE',
            title: 'Prix élevés',
            subtitle: 'Au moins un produit à plus de 10 000 FCFA — 2 000 FCFA/mois',
          ),
        ],
      ),
    );
  }

  Widget _buildCategoryOption(
    BuildContext context, {
    required String value,
    required String title,
    required String subtitle,
  }) {
    final colors = context.colors;
    final isSelected = _feeCategory == value;

    return InkWell(
      borderRadius: BorderRadius.circular(14),
      onTap: () => setState(() => _feeCategory = value),
      child: Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: isSelected ? colors.primary.withValues(alpha: 0.08) : colors.inputFill,
          borderRadius: BorderRadius.circular(14),
          border: Border.all(color: isSelected ? colors.primary : colors.border, width: isSelected ? 1.6 : 1),
        ),
        child: Row(
          children: [
            Icon(
              isSelected ? Icons.radio_button_checked_rounded : Icons.radio_button_off_rounded,
              color: isSelected ? colors.primary : colors.textMuted,
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title, style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark)),
                  Text(subtitle, style: TextStyle(fontSize: 12, color: colors.textMuted)),
                ],
              ),
            ),
          ],
        ),
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
                  'Maplenou prélève une commission transparente sur chaque vente réalisée. '
                  'En plus, tes 3 premières ventes sont sans frais mensuel ; à partir de la 4ᵉ, '
                  'un petit frais d\'activité (selon ta catégorie ci-dessus) est déduit '
                  'automatiquement de ton reversement chaque mois où tu as vendu.',
                  style: TextStyle(fontSize: 13, color: colors.textMuted, height: 1.4),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
