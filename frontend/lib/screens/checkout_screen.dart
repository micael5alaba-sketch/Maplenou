import 'package:flutter/material.dart';

import '../models/payment_method_option_model.dart';
import '../services/cart_service.dart';
import '../services/checkout_service.dart';
import '../theme/app_color_scheme.dart';
import '../utils/formatters.dart';
import '../widgets/payment_method_option_tile.dart';
import 'payment_success_screen.dart';

/// "Paiement" — delivery address, payment method choice, order summary.
///
/// Fully mocked: no payment integration exists on the backend (see
/// [CheckoutService]'s doc comment). Confirming just simulates a delay,
/// clears the cart, and shows [PaymentSuccessScreen].
class CheckoutScreen extends StatefulWidget {
  const CheckoutScreen({super.key});

  @override
  State<CheckoutScreen> createState() => _CheckoutScreenState();
}

class _CheckoutScreenState extends State<CheckoutScreen> {
  final _cart = CartService();
  PaymentMethodType _selectedMethod = PaymentMethodType.tMoney;
  bool _isSubmitting = false;

  void _showComingSoon(String feature) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text('$feature bientôt disponible.')));
  }

  Future<void> _confirmPayment() async {
    setState(() => _isSubmitting = true);
    final orderNumber = await CheckoutService().placeOrder();
    if (!mounted) return;
    setState(() => _isSubmitting = false);

    final total = _cart.subtotal + CheckoutService.deliveryFee;
    _cart.clear();

    Navigator.of(context).pushReplacement(
      MaterialPageRoute(builder: (_) => PaymentSuccessScreen(orderNumber: orderNumber, total: total)),
    );
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    final subtotal = _cart.subtotal;
    final total = subtotal + CheckoutService.deliveryFee;

    return Scaffold(
      backgroundColor: colors.homeBackground,
      body: SafeArea(
        child: Column(
          children: [
            _buildSecurityBanner(colors),
            _buildHeader(colors),
            Expanded(
              child: ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  _buildSectionTitle(colors, Icons.local_shipping_outlined, 'Adresse de livraison'),
                  const SizedBox(height: 10),
                  _buildAddressCard(colors),
                  const SizedBox(height: 10),
                  OutlinedButton.icon(
                    onPressed: () => _showComingSoon('Le partage de localisation'),
                    style: OutlinedButton.styleFrom(
                      foregroundColor: colors.textDark,
                      side: BorderSide(color: colors.border),
                      minimumSize: const Size.fromHeight(48),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                    ),
                    icon: const Icon(Icons.my_location_rounded, size: 18),
                    label: const Text('Partager ma localisation directe'),
                  ),
                  const SizedBox(height: 24),
                  _buildSectionTitle(colors, Icons.account_balance_wallet_outlined, 'Choisissez un moyen de paiement'),
                  const SizedBox(height: 10),
                  for (final option in kCheckoutPaymentMethods) ...[
                    PaymentMethodOptionTile(
                      option: option,
                      isSelected: _selectedMethod == option.type,
                      onTap: () => setState(() => _selectedMethod = option.type),
                    ),
                    const SizedBox(height: 10),
                  ],
                  const SizedBox(height: 14),
                  _buildSummaryCard(colors, subtotal, total),
                ],
              ),
            ),
            _buildBottomBar(colors, total),
          ],
        ),
      ),
    );
  }

  Widget _buildSecurityBanner(AppColorScheme colors) {
    return Container(
      width: double.infinity,
      color: colors.primary.withValues(alpha: 0.08),
      padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 16),
      child: Row(
        children: [
          Icon(Icons.verified_user_rounded, size: 16, color: colors.primary),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              'Paiement 100% sécurisé. Vos données sont chiffrées (SSL).',
              style: TextStyle(fontSize: 11, color: colors.primary, fontWeight: FontWeight.w600),
            ),
          ),
        ],
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
          Expanded(child: Text('Paiement', textAlign: TextAlign.center, style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark))),
          const SizedBox(width: 48),
        ],
      ),
    );
  }

  Widget _buildSectionTitle(AppColorScheme colors, IconData icon, String title) {
    return Row(
      children: [
        Icon(icon, size: 18, color: colors.textDark),
        const SizedBox(width: 8),
        Text(title, style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
      ],
    );
  }

  Widget _buildAddressCard(AppColorScheme colors) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: colors.primary.withValues(alpha: 0.05),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: colors.primary.withValues(alpha: 0.4)),
      ),
      child: Row(
        children: [
          Icon(Icons.home_outlined, color: colors.primary),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Adresse enregistrée', style: TextStyle(fontSize: 11, color: colors.textMuted)),
                Text('Lomé, Quartier Adidogomé, Rue des Étoiles', style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark)),
              ],
            ),
          ),
          Icon(Icons.check_circle_rounded, color: colors.primary),
        ],
      ),
    );
  }

  Widget _buildSummaryCard(AppColorScheme colors, num subtotal, num total) {
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
              Icon(Icons.receipt_long_outlined, size: 16, color: colors.primary),
              const SizedBox(width: 8),
              Text('Résumé de la commande', style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
            ],
          ),
          const SizedBox(height: 12),
          _buildSummaryLine(colors, 'Sous-total (${_cart.itemCount} article${_cart.itemCount > 1 ? 's' : ''})', formatFcfa(subtotal)),
          const SizedBox(height: 6),
          _buildSummaryLine(colors, 'Livraison', formatFcfa(CheckoutService.deliveryFee)),
          const Divider(height: 20),
          _buildSummaryLine(colors, 'Total à payer', formatFcfa(total), emphasize: true),
          const SizedBox(height: 12),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: [
              _buildPerk(colors, Icons.local_shipping_outlined, 'Livraison\nRapide'),
              _buildPerk(colors, Icons.support_agent_rounded, 'Support\n24/7'),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildSummaryLine(AppColorScheme colors, String label, String value, {bool emphasize = false}) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(label, style: TextStyle(color: emphasize ? colors.textDark : colors.textMuted, fontWeight: emphasize ? FontWeight.bold : FontWeight.normal)),
        Text(value, style: TextStyle(color: emphasize ? colors.primary : colors.textDark, fontWeight: FontWeight.bold, fontSize: emphasize ? 18 : 14)),
      ],
    );
  }

  Widget _buildPerk(AppColorScheme colors, IconData icon, String label) {
    return Column(
      children: [
        Icon(icon, size: 18, color: colors.textMuted),
        const SizedBox(height: 4),
        Text(label, textAlign: TextAlign.center, style: TextStyle(fontSize: 10, color: colors.textMuted)),
      ],
    );
  }

  Widget _buildBottomBar(AppColorScheme colors, num total) {
    return Container(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
      decoration: BoxDecoration(color: colors.surface, border: Border(top: BorderSide(color: colors.border))),
      child: SafeArea(
        top: false,
        child: Column(
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text('Total à régler', style: TextStyle(color: colors.textMuted)),
                Text(formatFcfa(total), style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: colors.textDark)),
              ],
            ),
            const SizedBox(height: 10),
            SizedBox(
              width: double.infinity,
              height: 54,
              child: ElevatedButton.icon(
                onPressed: _isSubmitting ? null : _confirmPayment,
                style: ElevatedButton.styleFrom(
                  backgroundColor: colors.primary,
                  foregroundColor: Colors.white,
                  elevation: 0,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28)),
                ),
                icon: _isSubmitting
                    ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                    : const Icon(Icons.lock_outline_rounded, size: 18),
                label: const Text('Confirmer le paiement', style: TextStyle(fontWeight: FontWeight.w600)),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
