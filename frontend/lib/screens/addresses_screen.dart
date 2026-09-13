import 'package:flutter/material.dart';

import '../models/address_model.dart';
import '../services/address_service.dart';
import '../theme/app_color_scheme.dart';

/// "Adresses enregistrées" — reached from [ProfileScreen]'s account
/// settings section.
///
/// Backed by [AddressService], an in-memory singleton shared across the
/// app session — maps onto the real, already working `AddressController`
/// once auth is wired up.
class AddressesScreen extends StatefulWidget {
  const AddressesScreen({super.key});

  @override
  State<AddressesScreen> createState() => _AddressesScreenState();
}

class _AddressesScreenState extends State<AddressesScreen> {
  final _service = AddressService();

  Future<void> _openAddAddressSheet() async {
    final added = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (_) => const _AddAddressSheet(),
    );
    if (added == true) setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    final addresses = _service.addresses;

    return Scaffold(
      backgroundColor: colors.homeBackground,
      body: SafeArea(
        child: Column(
          children: [
            _buildHeader(colors),
            Expanded(
              child: addresses.isEmpty
                  ? _buildEmptyState(colors)
                  : ListView.separated(
                      padding: const EdgeInsets.all(16),
                      itemCount: addresses.length,
                      separatorBuilder: (_, _) => const SizedBox(height: 12),
                      itemBuilder: (context, index) => _buildAddressCard(colors, addresses[index]),
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
              if (action == 'default') setState(() => _service.setDefault(address.id));
              if (action == 'delete') setState(() => _service.remove(address.id));
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

  @override
  void dispose() {
    _labelController.dispose();
    _cityController.dispose();
    _districtController.dispose();
    _detailsController.dispose();
    super.dispose();
  }

  void _handleSave() {
    if (!_formKey.currentState!.validate()) return;
    AddressService().add(
      label: _labelController.text.trim(),
      city: _cityController.text.trim(),
      district: _districtController.text.trim().isEmpty ? null : _districtController.text.trim(),
      details: _detailsController.text.trim().isEmpty ? null : _detailsController.text.trim(),
    );
    Navigator.of(context).pop(true);
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
                const SizedBox(height: 20),
                SizedBox(
                  width: double.infinity,
                  child: ElevatedButton(
                    onPressed: _handleSave,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: colors.primary,
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(vertical: 14),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                    ),
                    child: const Text('Enregistrer', style: TextStyle(fontWeight: FontWeight.w600)),
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
