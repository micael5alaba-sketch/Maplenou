import 'package:flutter/material.dart';

import '../theme/app_color_scheme.dart';
import '../utils/formatters.dart';
import '../widgets/primary_button.dart';
import 'home_screen.dart';
import 'order_tracking_screen.dart';

/// "Paiement réussi" — shown right after [CheckoutScreen] confirms.
class PaymentSuccessScreen extends StatelessWidget {
  final String orderNumber;
  final num total;

  const PaymentSuccessScreen({super.key, required this.orderNumber, required this.total});

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
              child: ListView(
                padding: const EdgeInsets.all(24),
                children: [
                  const SizedBox(height: 16),
                  Center(
                    child: Container(
                      padding: const EdgeInsets.all(20),
                      decoration: BoxDecoration(color: colors.primary.withValues(alpha: 0.1), shape: BoxShape.circle),
                      child: Icon(Icons.check_circle_rounded, size: 56, color: colors.primary),
                    ),
                  ),
                  const SizedBox(height: 20),
                  Text('Paiement Réussi !', textAlign: TextAlign.center, style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: colors.primary)),
                  const SizedBox(height: 8),
                  Text(
                    'Merci pour votre confiance. Votre commande est en cours de préparation.',
                    textAlign: TextAlign.center,
                    style: TextStyle(color: colors.textMuted, height: 1.4),
                  ),
                  const SizedBox(height: 24),
                  _buildDetailsCard(colors),
                  const SizedBox(height: 24),
                  _buildNextSteps(colors),
                  const SizedBox(height: 28),
                  PrimaryButton(
                    label: 'Suivre ma commande',
                    onPressed: () => Navigator.of(context).pushReplacement(
                      MaterialPageRoute(builder: (_) => OrderTrackingScreen(orderNumber: orderNumber)),
                    ),
                  ),
                  const SizedBox(height: 12),
                  SizedBox(
                    width: double.infinity,
                    child: OutlinedButton(
                      onPressed: () => Navigator.of(context).pushAndRemoveUntil(
                        MaterialPageRoute(builder: (_) => const HomeScreen()),
                        (route) => false,
                      ),
                      style: OutlinedButton.styleFrom(
                        foregroundColor: colors.textDark,
                        side: BorderSide(color: colors.border),
                        padding: const EdgeInsets.symmetric(vertical: 14),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(30)),
                      ),
                      child: const Text("Retour à l'accueil", style: TextStyle(fontWeight: FontWeight.w600)),
                    ),
                  ),
                ],
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
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      child: Text('Paiement réussi', style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
    );
  }

  Widget _buildDetailsCard(AppColorScheme colors) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 10, offset: const Offset(0, 3))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(Icons.receipt_long_outlined, size: 16, color: colors.textDark),
              const SizedBox(width: 8),
              Text('Détails de la commande', style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text('N° de commande', style: TextStyle(color: colors.textMuted)),
              Text('#$orderNumber', style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
            ],
          ),
          const SizedBox(height: 8),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text('Montant total', style: TextStyle(color: colors.textMuted)),
              Text(formatFcfa(total), style: TextStyle(fontWeight: FontWeight.bold, color: colors.accentOrange)),
            ],
          ),
          const SizedBox(height: 12),
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(color: colors.inputFill, borderRadius: BorderRadius.circular(10)),
            child: Row(
              children: [
                Icon(Icons.location_on_outlined, size: 16, color: colors.textMuted),
                const SizedBox(width: 8),
                const Expanded(child: Text('14 Rue des Ébénistes, Plateau, Abidjan, Côte d\'Ivoire', style: TextStyle(fontSize: 12))),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildNextSteps(AppColorScheme colors) {
    final steps = [
      ('Paiement confirmé', 'Votre transaction a été approuvée.', true),
      ('Préparation', 'Le vendeur prépare votre colis avec soin.', false),
      ('Expédition', 'Vous recevrez une notification dès que votre commande sera remise au livreur.', false),
    ];

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('PROCHAINES ÉTAPES', style: TextStyle(fontSize: 11, fontWeight: FontWeight.w700, color: colors.textMuted)),
        const SizedBox(height: 12),
        for (var i = 0; i < steps.length; i++)
          _buildStep(colors, steps[i].$1, steps[i].$2, steps[i].$3, isLast: i == steps.length - 1),
      ],
    );
  }

  Widget _buildStep(AppColorScheme colors, String title, String description, bool isDone, {required bool isLast}) {
    return IntrinsicHeight(
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Column(
            children: [
              Container(
                width: 14,
                height: 14,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: isDone ? colors.primary : Colors.transparent,
                  border: Border.all(color: colors.primary, width: 2),
                ),
              ),
              if (!isLast) Expanded(child: Container(width: 2, color: colors.primary.withValues(alpha: 0.3))),
            ],
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Padding(
              padding: const EdgeInsets.only(bottom: 16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title, style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark)),
                  Text(description, style: TextStyle(fontSize: 12, color: colors.textMuted)),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
