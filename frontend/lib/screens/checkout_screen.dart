import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

import '../models/address_model.dart';
import '../models/cart_model.dart';
import '../models/payment_country_model.dart';
import '../models/payment_method_option_model.dart';
import '../services/address_service.dart';
import '../services/api_client.dart';
import '../services/cart_service.dart';
import '../services/delivery_zone_service.dart';
import '../services/order_service.dart';
import '../theme/app_color_scheme.dart';
import '../utils/formatters.dart';
import '../widgets/payment_method_option_tile.dart';
import 'addresses_screen.dart';
import 'order_tracking_screen.dart';
import 'payment_success_screen.dart';

/// "Paiement" — delivery address, payment method choice, order summary.
///
/// `Confirmer` appelle le vrai `POST /api/orders` (`OrderService`) : la
/// commande est créée (statut `CREATED`). Si FedaPay est activé côté backend
/// (`app.fedapay.enabled=true`), la réponse contient une `paymentUrl` réelle
/// ouverte dans le navigateur, puis l'app bascule sur [OrderTrackingScreen]
/// (le vrai statut PAID n'arrive qu'via le webhook, de façon asynchrone —
/// impossible d'annoncer un succès immédiat ici). Sinon (cas par défaut hors
/// production), le comportement historique est conservé :
/// [PaymentSuccessScreen] s'affiche directement, sans paiement réel.
/// Le choix du moyen de paiement reste illustratif (FedaPay décide lui-même
/// du canal Mobile Money/carte sur sa page hébergée).
class CheckoutScreen extends StatefulWidget {
  const CheckoutScreen({super.key});

  @override
  State<CheckoutScreen> createState() => _CheckoutScreenState();
}

class _CheckoutScreenState extends State<CheckoutScreen> {
  final _cartService = CartService();
  final _addressService = AddressService();
  final _orderService = OrderService();
  final _deliveryZoneService = DeliveryZoneService();

  // Utilisé uniquement si /api/delivery-zones est injoignable, pour ne pas
  // bloquer l'affichage du checkout.
  static const num _fallbackDeliveryFee = 1500;

  PaymentMethodType _selectedMethod = PaymentMethodType.tMoney;
  PaymentCountry _selectedCountry = PaymentCountry.togo;
  bool _isSubmitting = false;

  late Future<(CartModel, List<AddressModel>, num)> _loadFuture;
  AddressModel? _selectedAddress;

  @override
  void initState() {
    super.initState();
    _loadFuture = _load();
  }

  Future<(CartModel, List<AddressModel>, num)> _load() async {
    final cart = await _cartService.getCart();
    final addresses = await _addressService.getAddresses();
    if (_selectedAddress == null && addresses.isNotEmpty) {
      _selectedAddress = addresses.firstWhere((a) => a.isDefault, orElse: () => addresses.first);
    }

    final deliveryFee = await _resolveDeliveryFee();
    return (cart, addresses, deliveryFee);
  }

  Future<num> _resolveDeliveryFee() async {
    num deliveryFee = _fallbackDeliveryFee;
    try {
      final zone = await _deliveryZoneService.resolveZone(
        district: _selectedAddress?.district,
        city: _selectedAddress?.city,
      );
      if (zone != null) deliveryFee = zone.deliveryFee;
    } catch (_) {
      // Zones de livraison injoignables : on garde le repli plutôt que de
      // bloquer le checkout.
    }
    return deliveryFee;
  }

  Future<void> _pickAddress(AppColorScheme colors, List<AddressModel> addresses) async {
    final chosen = await showModalBottomSheet<AddressModel>(
      context: context,
      backgroundColor: colors.surface,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (sheetContext) {
        return SafeArea(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Padding(
                padding: const EdgeInsets.all(16),
                child: Text('Choisir une adresse', style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
              ),
              for (final address in addresses)
                ListTile(
                  leading: Icon(
                    address.id == _selectedAddress?.id ? Icons.radio_button_checked_rounded : Icons.radio_button_off_rounded,
                    color: colors.primary,
                  ),
                  title: Text(address.label, style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark)),
                  subtitle: Text([address.summary, if (address.details != null) address.details!].join(', ')),
                  onTap: () => Navigator.of(sheetContext).pop(address),
                ),
              const SizedBox(height: 8),
            ],
          ),
        );
      },
    );

    if (chosen == null || chosen.id == _selectedAddress?.id || !mounted) return;
    setState(() {
      _selectedAddress = chosen;
      _loadFuture = _load();
    });
  }

  void _showMessage(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(message)));
  }

  Future<void> _confirmOrder() async {
    final address = _selectedAddress;
    if (address == null) {
      _showMessage('Ajoute une adresse de livraison avant de commander.');
      return;
    }

    setState(() => _isSubmitting = true);
    try {
      final order = await _orderService.placeOrder(
        addressId: address.id,
        paymentCountry: _selectedCountry.isoCode,
      );
      if (!mounted) return;

      final paymentUrl = order.paymentUrl;
      if (paymentUrl != null) {
        final opened = await launchUrl(Uri.parse(paymentUrl), mode: LaunchMode.externalApplication);
        if (!mounted) return;
        if (!opened) {
          _showMessage("Impossible d'ouvrir la page de paiement. Réessaie depuis \"Mes commandes\".");
        }
        Navigator.of(context).pushReplacement(
          MaterialPageRoute(builder: (_) => OrderTrackingScreen(orderId: order.id)),
        );
        return;
      }

      Navigator.of(context).pushReplacement(
        MaterialPageRoute(
          builder: (_) => PaymentSuccessScreen(
            orderId: order.id,
            orderNumber: order.orderNumber,
            total: order.totalAmount,
            deliveryAddressSummary: [address.summary, if (address.details != null) address.details!].join(', '),
          ),
        ),
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

    return Scaffold(
      backgroundColor: colors.homeBackground,
      body: SafeArea(
        child: FutureBuilder<(CartModel, List<AddressModel>, num)>(
          future: _loadFuture,
          builder: (context, snapshot) {
            if (snapshot.connectionState == ConnectionState.waiting) {
              return const Center(child: CircularProgressIndicator());
            }
            if (snapshot.hasError || !snapshot.hasData) {
              return _buildErrorState(colors, snapshot.error);
            }
            final (cart, addresses, deliveryFee) = snapshot.data!;
            return _buildContent(colors, cart, addresses, deliveryFee);
          },
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
              error is ApiException ? error.message : 'Impossible de préparer la commande.',
              textAlign: TextAlign.center,
              style: TextStyle(color: colors.textMuted),
            ),
            const SizedBox(height: 12),
            TextButton(onPressed: () => setState(() => _loadFuture = _load()), child: const Text('Réessayer')),
          ],
        ),
      ),
    );
  }

  Widget _buildContent(AppColorScheme colors, CartModel cart, List<AddressModel> addresses, num deliveryFee) {
    final subtotal = cart.totalAmount;
    final total = subtotal + deliveryFee;

    return Column(
      children: [
        _buildSecurityBanner(colors),
        _buildHeader(colors),
        Expanded(
          child: ListView(
            padding: const EdgeInsets.all(16),
            children: [
              _buildSectionTitle(colors, Icons.local_shipping_outlined, 'Adresse de livraison'),
              const SizedBox(height: 10),
              _buildAddressCard(colors, addresses),
              const SizedBox(height: 24),
              _buildSectionTitle(colors, Icons.public_rounded, 'Pays du paiement mobile money'),
              const SizedBox(height: 10),
              _buildCountryPicker(colors),
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
              _buildSummaryCard(colors, cart, subtotal, deliveryFee, total),
            ],
          ),
        ),
        _buildBottomBar(colors, total),
      ],
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

  Widget _buildAddressCard(AppColorScheme colors, List<AddressModel> addresses) {
    if (addresses.isEmpty) {
      return OutlinedButton.icon(
        onPressed: () async {
          await Navigator.of(context).push(MaterialPageRoute(builder: (_) => const AddressesScreen()));
          setState(() => _loadFuture = _load());
        },
        style: OutlinedButton.styleFrom(
          foregroundColor: colors.primary,
          side: BorderSide(color: colors.primary),
          minimumSize: const Size.fromHeight(48),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
        ),
        icon: const Icon(Icons.add_location_alt_outlined),
        label: const Text('Ajouter une adresse de livraison'),
      );
    }

    final address = _selectedAddress ?? addresses.first;
    return GestureDetector(
      onTap: addresses.length <= 1
          ? null
          : () => _pickAddress(colors, addresses),
      child: Container(
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
                  Text(address.label, style: TextStyle(fontSize: 11, color: colors.textMuted)),
                  Text(
                    [address.summary, if (address.details != null) address.details!].join(', '),
                    style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark),
                  ),
                ],
              ),
            ),
            Icon(Icons.check_circle_rounded, color: colors.primary),
          ],
        ),
      ),
    );
  }

  Widget _buildCountryPicker(AppColorScheme colors) {
    return Row(
      children: [
        for (final country in PaymentCountry.values) ...[
          Expanded(
            child: GestureDetector(
              onTap: () => setState(() => _selectedCountry = country),
              child: Container(
                padding: const EdgeInsets.symmetric(vertical: 10),
                decoration: BoxDecoration(
                  color: _selectedCountry == country ? colors.primary.withValues(alpha: 0.08) : colors.surface,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(
                    color: _selectedCountry == country ? colors.primary : colors.border,
                    width: _selectedCountry == country ? 1.6 : 1,
                  ),
                ),
                child: Column(
                  children: [
                    Text(country.flag, style: const TextStyle(fontSize: 18)),
                    const SizedBox(height: 2),
                    Text(
                      country.label,
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontSize: 11,
                        fontWeight: _selectedCountry == country ? FontWeight.bold : FontWeight.normal,
                        color: _selectedCountry == country ? colors.primary : colors.textDark,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
          if (country != PaymentCountry.values.last) const SizedBox(width: 8),
        ],
      ],
    );
  }

  Widget _buildSummaryCard(AppColorScheme colors, CartModel cart, num subtotal, num deliveryFee, num total) {
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
          _buildSummaryLine(colors, 'Sous-total (${cart.itemCount} article${cart.itemCount > 1 ? 's' : ''})', formatFcfa(subtotal)),
          const SizedBox(height: 6),
          _buildSummaryLine(colors, 'Livraison (estimation)', formatFcfa(deliveryFee)),
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
                onPressed: _isSubmitting ? null : _confirmOrder,
                style: ElevatedButton.styleFrom(
                  backgroundColor: colors.primary,
                  foregroundColor: Colors.white,
                  elevation: 0,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28)),
                ),
                icon: _isSubmitting
                    ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                    : const Icon(Icons.lock_outline_rounded, size: 18),
                label: const Text('Confirmer la commande', style: TextStyle(fontWeight: FontWeight.w600)),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
