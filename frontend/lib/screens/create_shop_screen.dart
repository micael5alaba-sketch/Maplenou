import 'package:flutter/material.dart';

import '../services/api_client.dart';
import '../services/location_service.dart';
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
  final _locationService = LocationService();

  double? _latitude;
  double? _longitude;
  bool _isLocating = false;
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

  Future<bool> _confirmLocationUsage() async {
    final colors = context.colors;
    final accepted = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Utiliser ta position actuelle ?'),
        content: const Text(
          "Maplenou va demander accès à ta position GPS pour pré-remplir automatiquement la "
          "ville et le quartier de ta boutique, et calculer un prix de livraison précis en "
          "fonction de la distance jusqu'à chaque acheteur. Sans ça, seul le tarif de base de "
          "la zone sera appliqué.",
        ),
        actions: [
          TextButton(onPressed: () => Navigator.of(dialogContext).pop(false), child: const Text('Non merci')),
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(true),
            child: Text('Autoriser', style: TextStyle(color: colors.primary, fontWeight: FontWeight.bold)),
          ),
        ],
      ),
    );
    return accepted ?? false;
  }

  Future<void> _useCurrentLocation() async {
    if (!await _confirmLocationUsage()) return;
    if (!mounted) return;

    setState(() => _isLocating = true);
    try {
      final position = await _locationService.getCurrentPosition();
      setState(() {
        _latitude = position.latitude;
        _longitude = position.longitude;
      });

      final geocoded = await _locationService.reverseGeocode(position.latitude, position.longitude);
      if (geocoded != null && mounted) {
        setState(() {
          if (_cityController.text.trim().isEmpty && geocoded.city != null) {
            _cityController.text = geocoded.city!;
          }
          if (_districtController.text.trim().isEmpty && geocoded.district != null) {
            _districtController.text = geocoded.district!;
          }
        });
      }
    } on LocationException catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context)
          ..hideCurrentSnackBar()
          ..showSnackBar(SnackBar(content: Text(e.message)));
      }
    } finally {
      if (mounted) setState(() => _isLocating = false);
    }
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
        latitude: _latitude,
        longitude: _longitude,
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
                      const SizedBox(height: 16),
                      OutlinedButton.icon(
                        onPressed: _isLocating ? null : _useCurrentLocation,
                        icon: _isLocating
                            ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2))
                            : Icon(_latitude != null ? Icons.check_circle_rounded : Icons.my_location_rounded),
                        label: Text(
                          _latitude != null
                              ? 'Position capturée — livraison calculée par distance'
                              : 'Utiliser ma position (recommandé)',
                        ),
                        style: OutlinedButton.styleFrom(
                          foregroundColor: _latitude != null ? colors.primary : colors.textDark,
                          side: BorderSide(color: _latitude != null ? colors.primary : colors.border),
                          padding: const EdgeInsets.symmetric(vertical: 14),
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                        ),
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
