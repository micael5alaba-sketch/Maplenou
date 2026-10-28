import 'package:flutter/material.dart';

import '../models/shop_model.dart';
import '../services/api_client.dart';
import '../services/shop_service.dart';
import '../theme/app_color_scheme.dart';

/// Configuration des horaires d'ouverture par le vendeur — point 1 du rapport
/// de modification du 28/10/2026 (lecture côté acheteur : [ShopDetailScreen]).
class SellerOpeningHoursScreen extends StatefulWidget {
  const SellerOpeningHoursScreen({super.key});

  @override
  State<SellerOpeningHoursScreen> createState() => _SellerOpeningHoursScreenState();
}

class _SellerOpeningHoursScreenState extends State<SellerOpeningHoursScreen> {
  final _shopService = ShopService();
  late Future<void> _loadFuture;
  bool _isSaving = false;

  /// Toujours 7 entrées, une par jour (index 0 = lundi ... 6 = dimanche),
  /// même si le vendeur n'a encore rien configuré.
  List<OpeningHourModel> _hours = List.generate(
    7,
    (i) => OpeningHourModel(dayOfWeek: i + 1, closed: true),
  );

  @override
  void initState() {
    super.initState();
    _loadFuture = _load();
  }

  Future<void> _load() async {
    final shop = await _shopService.getMine();
    if (shop == null) return;
    final byDay = {for (final h in shop.openingHours) h.dayOfWeek: h};
    _hours = List.generate(7, (i) => byDay[i + 1] ?? OpeningHourModel(dayOfWeek: i + 1, closed: true));
  }

  Future<void> _save() async {
    setState(() => _isSaving = true);
    try {
      await _shopService.updateOpeningHours(_hours);
      if (!mounted) return;
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(const SnackBar(content: Text('Horaires enregistrés.')));
    } on ApiException catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(SnackBar(content: Text(e.message)));
    } finally {
      if (mounted) setState(() => _isSaving = false);
    }
  }

  Future<void> _pickTime(int index, {required bool isOpening}) async {
    final current = isOpening ? _hours[index].opensAt : _hours[index].closesAt;
    final initial = _parseTimeOfDay(current) ?? const TimeOfDay(hour: 8, minute: 0);
    final picked = await showTimePicker(context: context, initialTime: initial);
    if (picked == null) return;
    final formatted = '${picked.hour.toString().padLeft(2, '0')}:${picked.minute.toString().padLeft(2, '0')}';
    setState(() {
      _hours[index] = OpeningHourModel(
        dayOfWeek: _hours[index].dayOfWeek,
        opensAt: isOpening ? formatted : _hours[index].opensAt,
        closesAt: isOpening ? _hours[index].closesAt : formatted,
        closed: false,
      );
    });
  }

  TimeOfDay? _parseTimeOfDay(String? value) {
    if (value == null) return null;
    final parts = value.split(':');
    if (parts.length < 2) return null;
    final h = int.tryParse(parts[0]);
    final m = int.tryParse(parts[1]);
    if (h == null || m == null) return null;
    return TimeOfDay(hour: h, minute: m);
  }

  void _toggleClosed(int index, bool open) {
    setState(() {
      _hours[index] = OpeningHourModel(
        dayOfWeek: _hours[index].dayOfWeek,
        opensAt: open ? (_hours[index].opensAt ?? '08:00') : null,
        closesAt: open ? (_hours[index].closesAt ?? '18:00') : null,
        closed: !open,
      );
    });
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
              child: FutureBuilder<void>(
                future: _loadFuture,
                builder: (context, snapshot) {
                  if (snapshot.connectionState == ConnectionState.waiting) {
                    return const Center(child: CircularProgressIndicator());
                  }
                  if (snapshot.hasError) {
                    return Center(
                      child: Text(
                        snapshot.error is ApiException ? (snapshot.error as ApiException).message : 'Erreur de chargement.',
                        style: TextStyle(color: colors.textMuted),
                      ),
                    );
                  }
                  return _buildContent(colors);
                },
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
          Expanded(
            child: Text(
              "Horaires d'ouverture",
              textAlign: TextAlign.center,
              style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark),
            ),
          ),
          const SizedBox(width: 48),
        ],
      ),
    );
  }

  Widget _buildContent(AppColorScheme colors) {
    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
      children: [
        Text(
          'Indiquez vos jours et heures d\'ouverture — visible publiquement sur votre boutique.',
          style: TextStyle(color: colors.textMuted, fontSize: 13),
        ),
        const SizedBox(height: 16),
        for (int i = 0; i < _hours.length; i++) _buildDayRow(colors, i),
        const SizedBox(height: 16),
        SizedBox(
          width: double.infinity,
          child: ElevatedButton(
            onPressed: _isSaving ? null : _save,
            style: ElevatedButton.styleFrom(
              backgroundColor: colors.primary,
              foregroundColor: Colors.white,
              elevation: 0,
              padding: const EdgeInsets.symmetric(vertical: 14),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
            ),
            child: _isSaving
                ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                : const Text('Enregistrer', style: TextStyle(fontWeight: FontWeight.w600)),
          ),
        ),
      ],
    );
  }

  Widget _buildDayRow(AppColorScheme colors, int index) {
    final hour = _hours[index];
    final isOpen = !hour.closed;
    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(14),
        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.04), blurRadius: 8, offset: const Offset(0, 2))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(hour.dayLabel, style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark)),
              Switch(value: isOpen, activeThumbColor: colors.primary, onChanged: (v) => _toggleClosed(index, v)),
            ],
          ),
          if (isOpen)
            Padding(
              padding: const EdgeInsets.only(top: 4),
              child: Row(
                children: [
                  Expanded(child: _buildTimeButton(colors, 'Ouverture', hour.opensAt, () => _pickTime(index, isOpening: true))),
                  const SizedBox(width: 10),
                  Expanded(child: _buildTimeButton(colors, 'Fermeture', hour.closesAt, () => _pickTime(index, isOpening: false))),
                ],
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildTimeButton(AppColorScheme colors, String label, String? value, VoidCallback onTap) {
    return OutlinedButton(
      onPressed: onTap,
      style: OutlinedButton.styleFrom(
        foregroundColor: colors.textDark,
        side: BorderSide(color: colors.border),
        padding: const EdgeInsets.symmetric(vertical: 10),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
      ),
      child: Text('$label : ${value ?? '--:--'}', style: const TextStyle(fontSize: 12)),
    );
  }
}
