import 'package:flutter/material.dart';

import '../models/address_model.dart';
import '../services/address_service.dart';
import '../services/api_client.dart';
import '../services/location_service.dart';
import '../theme/app_color_scheme.dart';

/// "Adresses enregistrées" — reached from [ProfileScreen]'s account
/// settings section.
///
/// Backed by [AddressService] (`GET/POST/PATCH/DELETE /api/users/me/addresses`).
class AddressesScreen extends StatefulWidget {
  const AddressesScreen({super.key});

  @override
  State<AddressesScreen> createState() => _AddressesScreenState();
}

class _AddressesScreenState extends State<AddressesScreen> {
  final _service = AddressService();
  late Future<List<AddressModel>> _addressesFuture;

  @override
  void initState() {
    super.initState();
    _addressesFuture = _service.getAddresses();
  }

  void _reload() {
    setState(() => _addressesFuture = _service.getAddresses());
  }

  Future<void> _openAddAddressSheet() async {
    final added = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (_) => const _AddAddressSheet(),
    );
    if (added == true) _reload();
  }

  Future<void> _setDefault(AddressModel address) async {
    try {
      await _service.setDefault(address.id);
      _reload();
    } on ApiException catch (e) {
      _showError(e.message);
    }
  }

  Future<void> _remove(AddressModel address) async {
    try {
      await _service.remove(address.id);
      _reload();
    } on ApiException catch (e) {
      _showError(e.message);
    }
  }

  void _showError(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(message)));
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return Scaffold(
      backgroundColor: colors.homeBackground,
      body: SafeArea(
        child: Column(
          children: [
            _buildHeader(colors),
            Expanded(
              child: FutureBuilder<List<AddressModel>>(
                future: _addressesFuture,
                builder: (context, snapshot) {
                  if (snapshot.connectionState == ConnectionState.waiting) {
                    return const Center(child: CircularProgressIndicator());
                  }
                  if (snapshot.hasError) {
                    return _buildErrorState(colors, snapshot.error);
                  }
                  final addresses = snapshot.data ?? const [];
                  if (addresses.isEmpty) return _buildEmptyState(colors);
                  return ListView.separated(
                    padding: const EdgeInsets.all(16),
                    itemCount: addresses.length,
                    separatorBuilder: (_, _) => const SizedBox(height: 12),
                    itemBuilder: (context, index) => _buildAddressCard(colors, addresses[index]),
                  );
                },
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
              child: SizedBox(
                width: double.infinity,
                child: OutlinedButton.icon(
                  onPressed: _openAddAddressSheet,
                  style: OutlinedButton.styleFrom(
                    foregroundColor: colors.primary,
                    side: BorderSide(color: colors.primary),
                    padding: const EdgeInsets.symmetric(vertical: 14),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                  ),
                  icon: const Icon(Icons.add_location_alt_outlined),
                  label: const Text('Ajouter une adresse', style: TextStyle(fontWeight: FontWeight.w600)),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader(AppColorScheme colors) {
    return Container(
      color: colors.surface,
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
      child: Row(
        children: [
          IconButton(icon: Icon(Icons.arrow_back_rounded, color: colors.textDark), onPressed: () => Navigator.of(context).pop()),
          Expanded(child: Text('Adresses enregistrées', textAlign: TextAlign.center, style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark))),
          const SizedBox(width: 48),
        ],
      ),
    );
  }

  Widget _buildAddressCard(AppColorScheme colors, AddressModel address) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: address.isDefault ? colors.primary.withValues(alpha: 0.05) : colors.surface,
        borderRadius: BorderRadius.circular(16),
        border: address.isDefault ? Border.all(color: colors.primary.withValues(alpha: 0.4)) : null,
        boxShadow: address.isDefault ? null : [BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 10, offset: const Offset(0, 3))],
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.home_outlined, color: colors.primary),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Text(address.label, style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
                    if (address.isDefault) ...[
                      const SizedBox(width: 8),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                        decoration: BoxDecoration(color: colors.primary, borderRadius: BorderRadius.circular(20)),
                        child: const Text('Par défaut', style: TextStyle(color: Colors.white, fontSize: 10, fontWeight: FontWeight.w700)),
                      ),
                    ],
                    if (!address.hasLocation) ...[
                      const SizedBox(width: 8),
                      Icon(Icons.location_off_outlined, size: 14, color: colors.textMuted),
                    ],
                  ],
                ),
                const SizedBox(height: 2),
                Text(address.summary, style: TextStyle(color: colors.textDark)),
                if (address.details != null && address.details!.isNotEmpty)
                  Text(address.details!, style: TextStyle(fontSize: 12, color: colors.textMuted)),
              ],
            ),
          ),
          PopupMenuButton<String>(
            icon: Icon(Icons.more_vert_rounded, color: colors.textMuted),
            onSelected: (action) {
              if (action == 'default') _setDefault(address);
              if (action == 'delete') _remove(address);
            },
            itemBuilder: (context) => [
              if (!address.isDefault) const PopupMenuItem(value: 'default', child: Text('Définir par défaut')),
              const PopupMenuItem(value: 'delete', child: Text('Supprimer')),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildEmptyState(AppColorScheme colors) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.location_off_outlined, size: 48, color: colors.textMuted),
            const SizedBox(height: 12),
            Text('Aucune adresse enregistrée.', style: TextStyle(color: colors.textMuted, fontWeight: FontWeight.w500)),
          ],
        ),
      ),
    );
  }

  Widget _buildErrorState(AppColorScheme colors, Object? error) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.error_outline_rounded, size: 48, color: colors.error),
            const SizedBox(height: 12),
            Text(
              error is ApiException ? error.message : 'Impossible de charger les adresses.',
              textAlign: TextAlign.center,
              style: TextStyle(color: colors.textMuted),
            ),
            const SizedBox(height: 12),
            TextButton(onPressed: _reload, child: const Text('Réessayer')),
          ],
        ),
      ),
    );
  }
}

/// Bottom sheet form to add a new address.
class _AddAddressSheet extends StatefulWidget {
  const _AddAddressSheet();

  @override
  State<_AddAddressSheet> createState() => _AddAddressSheetState();
}

class _AddAddressSheetState extends State<_AddAddressSheet> {
  final _formKey = GlobalKey<FormState>();
  final _labelController = TextEditingController();
  final _cityController = TextEditingController();
  final _districtController = TextEditingController();
  final _detailsController = TextEditingController();
  final _locationService = LocationService();

  double? _latitude;
  double? _longitude;
  bool _isLocating = false;
  bool _isSaving = false;

  @override
  void dispose() {
    _labelController.dispose();
    _cityController.dispose();
    _districtController.dispose();
    _detailsController.dispose();
    super.dispose();
  }

  Future<void> _useCurrentLocation() async {
    setState(() => _isLocating = true);
    try {
      final position = await _locationService.getCurrentPosition();
      setState(() {
        _latitude = position.latitude;
        _longitude = position.longitude;
      });
    } on LocationException catch (e) {
      _showMessage(e.message);
    } finally {
      if (mounted) setState(() => _isLocating = false);
    }
  }

  Future<void> _handleSave() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() => _isSaving = true);
    try {
      await AddressService().add(
        label: _labelController.text.trim(),
        city: _cityController.text.trim(),
        district: _districtController.text.trim().isEmpty ? null : _districtController.text.trim(),
        details: _detailsController.text.trim().isEmpty ? null : _detailsController.text.trim(),
        latitude: _latitude,
        longitude: _longitude,
      );
      if (!mounted) return;
      Navigator.of(context).pop(true);
    } on ApiException catch (e) {
      _showMessage(e.message);
    } finally {
      if (mounted) setState(() => _isSaving = false);
    }
  }

  void _showMessage(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(message)));
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return Padding(
      padding: EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom),
      child: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Form(
            key: _formKey,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Nouvelle adresse', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: colors.textDark)),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _labelController,
                  decoration: const InputDecoration(labelText: 'Libellé (ex: Maison, Bureau)'),
                  validator: (v) => (v == null || v.trim().isEmpty) ? 'Indique un libellé.' : null,
                ),
                const SizedBox(height: 12),
                TextFormField(
                  controller: _cityController,
                  decoration: const InputDecoration(labelText: 'Ville'),
                  validator: (v) => (v == null || v.trim().isEmpty) ? 'Indique la ville.' : null,
                ),
                const SizedBox(height: 12),
                TextFormField(controller: _districtController, decoration: const InputDecoration(labelText: 'Quartier (optionnel)')),
                const SizedBox(height: 12),
                TextFormField(controller: _detailsController, decoration: const InputDecoration(labelText: 'Détails / repère (optionnel)'), maxLines: 2),
                const SizedBox(height: 12),
                OutlinedButton.icon(
                  onPressed: _isLocating ? null : _useCurrentLocation,
                  style: OutlinedButton.styleFrom(
                    foregroundColor: _latitude != null ? colors.primary : colors.textDark,
                    side: BorderSide(color: _latitude != null ? colors.primary : colors.border),
                  ),
                  icon: _isLocating
                      ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2))
                      : Icon(_latitude != null ? Icons.check_circle_rounded : Icons.my_location_rounded),
                  label: Text(_latitude != null ? 'Position actuelle enregistrée' : 'Utiliser ma position actuelle'),
                ),
                const SizedBox(height: 4),
                Text(
                  "Permet au livreur de te localiser précisément — sinon il se basera uniquement sur le texte de l'adresse.",
                  style: TextStyle(fontSize: 11, color: colors.textMuted, height: 1.3),
                ),
                const SizedBox(height: 20),
                SizedBox(
                  width: double.infinity,
                  child: ElevatedButton(
                    onPressed: _isSaving ? null : _handleSave,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: colors.primary,
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(vertical: 14),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                    ),
                    child: _isSaving
                        ? const SizedBox(
                            width: 20,
                            height: 20,
                            child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                          )
                        : const Text('Enregistrer', style: TextStyle(fontWeight: FontWeight.w600)),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
