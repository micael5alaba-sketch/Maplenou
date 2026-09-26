import 'package:flutter/material.dart';

import '../services/api_client.dart';
import '../services/courier_delivery_service.dart';
import '../services/media_upload_service.dart';
import '../theme/app_color_scheme.dart';
import 'courier_dashboard_screen.dart';

/// "Validation de livraison" — capture une photo de preuve (upload signé
/// Cloudinary, voir `MediaUploadService`), obligatoire côté backend pour
/// passer une livraison à `DELIVERED` (`UpdateDeliveryRequest.proofUrl`).
/// Pas de collecte d'espèces ici : aucun flux de paiement à la livraison
/// n'existe dans ce système (paiement en ligne, explicitement reporté —
/// `CLAUDE.md` §7).
class CourierDeliveryConfirmationScreen extends StatefulWidget {
  final String deliveryId;

  const CourierDeliveryConfirmationScreen({super.key, required this.deliveryId});

  @override
  State<CourierDeliveryConfirmationScreen> createState() => _CourierDeliveryConfirmationScreenState();
}

class _CourierDeliveryConfirmationScreenState extends State<CourierDeliveryConfirmationScreen> {
  final _deliveryService = CourierDeliveryService();
  final _mediaUploadService = MediaUploadService();

  String? _proofUrl;
  bool _isCapturing = false;
  bool _isSubmitting = false;

  void _showMessage(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(message)));
  }

  Future<void> _capturePhoto() async {
    setState(() => _isCapturing = true);
    try {
      final url = await _mediaUploadService.capturePhotoAndUpload(folder: 'delivery-proofs');
      if (url != null) setState(() => _proofUrl = url);
    } on ApiException catch (e) {
      _showMessage(e.message);
    } finally {
      if (mounted) setState(() => _isCapturing = false);
    }
  }

  Future<void> _validateDelivery() async {
    if (_proofUrl == null) {
      _showMessage('Prends une photo de preuve avant de valider.');
      return;
    }

    setState(() => _isSubmitting = true);
    try {
      await _deliveryService.confirmDelivery(widget.deliveryId, proofUrl: _proofUrl!);
      if (!mounted) return;
      Navigator.of(context).pushAndRemoveUntil(
        MaterialPageRoute(builder: (_) => const CourierDashboardScreen()),
        (route) => false,
      );
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(const SnackBar(content: Text('Livraison validée !')));
    } on ApiException catch (e) {
      _showMessage(e.message);
    } finally {
      if (mounted) setState(() => _isSubmitting = false);
    }
  }

  Future<void> _reportFailure() async {
    setState(() => _isSubmitting = true);
    try {
      await _deliveryService.reportFailure(widget.deliveryId, reason: 'Signalé par le livreur depuis l\'application');
      if (!mounted) return;
      Navigator.of(context).pushAndRemoveUntil(
        MaterialPageRoute(builder: (_) => const CourierDashboardScreen()),
        (route) => false,
      );
    } on ApiException catch (e) {
      _showMessage(e.message);
    } finally {
      if (mounted) setState(() => _isSubmitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    final now = TimeOfDay.now();

    return Scaffold(
      backgroundColor: colors.homeBackground,
      body: SafeArea(
        child: Column(
          children: [
            _buildHeader(colors, now),
            Expanded(
              child: ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  Text('PREUVE DE REMISE', style: TextStyle(fontSize: 12, fontWeight: FontWeight.w700, color: colors.textMuted)),
                  const SizedBox(height: 10),
                  _buildProofTile(colors),
                ],
              ),
            ),
            _buildBottomActions(colors),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader(AppColorScheme colors, TimeOfDay now) {
    return Container(
      color: colors.surface,
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
      child: Column(
        children: [
          Row(
            children: [
              IconButton(
                icon: Icon(Icons.arrow_back_rounded, color: colors.textDark),
                onPressed: () => Navigator.of(context).pop(),
              ),
              Expanded(
                child: Text('Validation de livraison', textAlign: TextAlign.center, style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
              ),
              const SizedBox(width: 48),
            ],
          ),
          Text(now.format(context), style: TextStyle(fontSize: 12, color: colors.textMuted)),
          const SizedBox(height: 4),
        ],
      ),
    );
  }

  Widget _buildProofTile(AppColorScheme colors) {
    final hasProof = _proofUrl != null;

    return InkWell(
      borderRadius: BorderRadius.circular(16),
      onTap: _isCapturing ? null : _capturePhoto,
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.symmetric(vertical: 28),
        decoration: BoxDecoration(
          color: colors.surface,
          borderRadius: BorderRadius.circular(16),
          border: hasProof ? Border.all(color: colors.primary, width: 1.6) : null,
          boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 10, offset: const Offset(0, 3))],
        ),
        child: Column(
          children: [
            if (hasProof) ...[
              ClipRRect(
                borderRadius: BorderRadius.circular(12),
                child: Image.network(_proofUrl!, width: 120, height: 120, fit: BoxFit.cover),
              ),
              const SizedBox(height: 10),
              Text('Photo capturée', style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: colors.primary)),
              const SizedBox(height: 4),
              Text('Toucher pour reprendre', style: TextStyle(fontSize: 11, color: colors.textMuted)),
            ] else ...[
              CircleAvatar(
                radius: 26,
                backgroundColor: colors.inputFill,
                child: _isCapturing
                    ? SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: colors.primary))
                    : Icon(Icons.camera_alt_outlined, color: colors.textDark),
              ),
              const SizedBox(height: 10),
              Text('Prendre une photo de preuve', textAlign: TextAlign.center, style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: colors.textDark)),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildBottomActions(AppColorScheme colors) {
    return SafeArea(
      top: false,
      child: Container(
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
        decoration: BoxDecoration(color: colors.surface, border: Border(top: BorderSide(color: colors.border))),
        child: Column(
          children: [
            SizedBox(
              width: double.infinity,
              height: 54,
              child: ElevatedButton.icon(
                onPressed: _isSubmitting ? null : _validateDelivery,
                style: ElevatedButton.styleFrom(
                  backgroundColor: colors.primary,
                  foregroundColor: Colors.white,
                  elevation: 0,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28)),
                ),
                icon: _isSubmitting
                    ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                    : const Icon(Icons.check_circle_outline_rounded),
                label: const Text('Valider la livraison', style: TextStyle(fontWeight: FontWeight.w600)),
              ),
            ),
            const SizedBox(height: 8),
            TextButton(
              onPressed: _isSubmitting ? null : _reportFailure,
              child: Text('Signaler un échec de livraison', style: TextStyle(color: colors.error, fontSize: 13)),
            ),
          ],
        ),
      ),
    );
  }
}
