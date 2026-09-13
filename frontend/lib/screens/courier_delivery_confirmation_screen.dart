import 'package:flutter/material.dart';

import '../services/active_delivery_service.dart';
import '../theme/app_color_scheme.dart';
import '../utils/formatters.dart';
import 'courier_dashboard_screen.dart';

/// "Validation de livraison" — cash-collection checkbox, proof-of-delivery
/// placeholders (photo/signature/QR scan — no camera/scanner wired up
/// yet), and the final confirm action.
class CourierDeliveryConfirmationScreen extends StatefulWidget {
  const CourierDeliveryConfirmationScreen({super.key});

  @override
  State<CourierDeliveryConfirmationScreen> createState() => _CourierDeliveryConfirmationScreenState();
}

class _CourierDeliveryConfirmationScreenState extends State<CourierDeliveryConfirmationScreen> {
  bool _cashConfirmed = false;

  void _showComingSoon(String feature) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text('$feature bientôt disponible.')));
  }

  void _validateDelivery() {
    if (!_cashConfirmed) {
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(const SnackBar(content: Text('Confirme la réception du paiement avant de valider.')));
      return;
    }
    Navigator.of(context).pushAndRemoveUntil(
      MaterialPageRoute(builder: (_) => const CourierDashboardScreen()),
      (route) => false,
    );
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(const SnackBar(content: Text('Livraison validée !')));
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    final delivery = ActiveDeliveryService().getActiveDelivery('current');
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
                  _buildPaymentCard(colors, delivery.amountToCollect),
                  const SizedBox(height: 20),
                  Text('PREUVES DE REMISE', style: TextStyle(fontSize: 12, fontWeight: FontWeight.w700, color: colors.textMuted)),
                  const SizedBox(height: 10),
                  Row(
                    children: [
                      Expanded(child: _buildProofTile(colors, Icons.camera_alt_outlined, 'Prendre une photo')),
                      const SizedBox(width: 12),
                      Expanded(child: _buildProofTile(colors, Icons.draw_outlined, 'Signature client')),
                    ],
                  ),
                  const SizedBox(height: 12),
                  _buildScanRow(colors),
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

  Widget _buildPaymentCard(AppColorScheme colors, num amount) {
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(18),
        border: Border(top: BorderSide(color: colors.accentOrange, width: 3)),
        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 10, offset: const Offset(0, 3))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(Icons.account_balance_wallet_outlined, size: 16, color: colors.textDark),
              const SizedBox(width: 8),
              Text('GESTION DU PAIEMENT', style: TextStyle(fontSize: 12, fontWeight: FontWeight.w700, color: colors.textMuted)),
            ],
          ),
          const SizedBox(height: 14),
          Container(
            width: double.infinity,
            padding: const EdgeInsets.symmetric(vertical: 16),
            decoration: BoxDecoration(color: colors.inputFill, borderRadius: BorderRadius.circular(14)),
            child: Column(
              children: [
                Text('Montant à encaisser', style: TextStyle(fontSize: 12, color: colors.textMuted)),
                const SizedBox(height: 4),
                Text(formatFcfa(amount), style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: colors.primary)),
              ],
            ),
          ),
          const SizedBox(height: 14),
          InkWell(
            onTap: () => setState(() => _cashConfirmed = !_cashConfirmed),
            child: Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(borderRadius: BorderRadius.circular(12), border: Border.all(color: colors.border)),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Checkbox(value: _cashConfirmed, onChanged: (v) => setState(() => _cashConfirmed = v ?? false), activeColor: colors.primary),
                  Expanded(
                    child: Padding(
                      padding: const EdgeInsets.only(top: 12),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text('Paiement espèces perçu et vérifié', style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark)),
                          Text('Le montant exact a été remis par le client.', style: TextStyle(fontSize: 12, color: colors.textMuted)),
                        ],
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildProofTile(AppColorScheme colors, IconData icon, String label) {
    return InkWell(
      borderRadius: BorderRadius.circular(16),
      onTap: () => _showComingSoon(label),
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 20),
        decoration: BoxDecoration(
          color: colors.surface,
          borderRadius: BorderRadius.circular(16),
          boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 10, offset: const Offset(0, 3))],
        ),
        child: Column(
          children: [
            CircleAvatar(radius: 22, backgroundColor: colors.inputFill, child: Icon(icon, color: colors.textDark)),
            const SizedBox(height: 10),
            Text(label, textAlign: TextAlign.center, style: TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: colors.textDark)),
          ],
        ),
      ),
    );
  }

  Widget _buildScanRow(AppColorScheme colors) {
    return InkWell(
      borderRadius: BorderRadius.circular(14),
      onTap: () => _showComingSoon('Le scan du QR code'),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        decoration: BoxDecoration(color: colors.inputFill, borderRadius: BorderRadius.circular(14)),
        child: Row(
          children: [
            Icon(Icons.qr_code_scanner_rounded, color: colors.textDark),
            const SizedBox(width: 10),
            Text('Scanner le QR code de commande', style: TextStyle(color: colors.textDark, fontWeight: FontWeight.w600)),
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
                onPressed: _validateDelivery,
                style: ElevatedButton.styleFrom(
                  backgroundColor: colors.primary,
                  foregroundColor: Colors.white,
                  elevation: 0,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28)),
                ),
                icon: const Icon(Icons.check_circle_outline_rounded),
                label: const Text('Valider la livraison', style: TextStyle(fontWeight: FontWeight.w600)),
              ),
            ),
            const SizedBox(height: 8),
            TextButton(
              onPressed: () => _showComingSoon('Le signalement de problème'),
              child: Text('Signaler un problème', style: TextStyle(color: colors.error, fontSize: 13)),
            ),
          ],
        ),
      ),
    );
  }
}
