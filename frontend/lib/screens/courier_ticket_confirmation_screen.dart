import 'package:flutter/material.dart';

import '../theme/app_color_scheme.dart';
import '../widgets/primary_button.dart';
import 'courier_dashboard_screen.dart';

/// "Ticket envoyé avec succès" — shown after
/// [CourierSupportTicketScreen] submits.
class CourierTicketConfirmationScreen extends StatelessWidget {
  final String ticketNumber;

  const CourierTicketConfirmationScreen({super.key, required this.ticketNumber});

  void _showComingSoon(BuildContext context, String feature) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text('$feature bientôt disponible.')));
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return Scaffold(
      backgroundColor: colors.homeBackground,
      body: SafeArea(
        child: Center(
          child: Padding(
            padding: const EdgeInsets.all(32),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Container(
                  padding: const EdgeInsets.all(20),
                  decoration: BoxDecoration(color: colors.primary.withValues(alpha: 0.1), shape: BoxShape.circle),
                  child: Icon(Icons.check_circle_rounded, size: 56, color: colors.primary),
                ),
                const SizedBox(height: 20),
                Text(
                  'Ticket envoyé avec succès',
                  textAlign: TextAlign.center,
                  style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: colors.textDark),
                ),
                const SizedBox(height: 10),
                Text.rich(
                  TextSpan(
                    style: TextStyle(color: colors.textMuted, height: 1.4),
                    children: [
                      const TextSpan(text: 'Votre ticket a été enregistré sous le numéro '),
                      TextSpan(text: '#$ticketNumber', style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
                      const TextSpan(text: ". Notre équipe support l'étudie et vous répondra dans les plus brefs délais."),
                    ],
                  ),
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 28),
                PrimaryButton(
                  label: 'Retour au tableau de bord',
                  onPressed: () => Navigator.of(context).pushAndRemoveUntil(
                    MaterialPageRoute(builder: (_) => const CourierDashboardScreen()),
                    (route) => false,
                  ),
                ),
                const SizedBox(height: 12),
                SizedBox(
                  width: double.infinity,
                  child: OutlinedButton(
                    onPressed: () => _showComingSoon(context, 'La liste de mes tickets'),
                    style: OutlinedButton.styleFrom(
                      foregroundColor: colors.textDark,
                      side: BorderSide(color: colors.border),
                      padding: const EdgeInsets.symmetric(vertical: 14),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(30)),
                    ),
                    child: const Text('Voir mes tickets', style: TextStyle(fontWeight: FontWeight.w600)),
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
