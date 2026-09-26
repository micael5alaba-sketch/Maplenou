import 'package:flutter/material.dart';

import '../models/cart_model.dart';
import '../services/api_client.dart';
import '../services/cart_service.dart';
import '../theme/app_color_scheme.dart';
import '../utils/formatters.dart';
import '../widgets/app_logo.dart';
import '../widgets/cart_item_row.dart';
import '../widgets/custom_bottom_navigation.dart';
import '../widgets/primary_button.dart';
import 'categories_screen.dart';
import 'checkout_screen.dart';
import 'profile_screen.dart';

/// Buyer's cart — reached from the bottom navigation's "Panier" tab.
///
/// Backed by [CartService] (`GET/POST/PATCH/DELETE /api/cart[/items]`).
class CartScreen extends StatefulWidget {
  const CartScreen({super.key});

  @override
  State<CartScreen> createState() => _CartScreenState();
}

class _CartScreenState extends State<CartScreen> {
  final _cartService = CartService();
  late Future<CartModel> _cartFuture;

  @override
  void initState() {
    super.initState();
    _cartFuture = _cartService.getCart();
  }

  void _reload() {
    setState(() => _cartFuture = _cartService.getCart());
  }

  Future<void> _updateQuantity(String itemId, int quantity) async {
    try {
      final cart = await _cartService.updateQuantity(itemId, quantity);
      setState(() => _cartFuture = Future.value(cart));
    } on ApiException catch (e) {
      _showError(e.message);
    }
  }

  Future<void> _removeItem(String itemId) async {
    try {
      final cart = await _cartService.removeItem(itemId);
      setState(() => _cartFuture = Future.value(cart));
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

  void _onTabSelected(HomeTab tab) {
    if (tab == HomeTab.cart) return;
    switch (tab) {
      case HomeTab.home:
        Navigator.of(context).popUntil((route) => route.isFirst);
        break;
      case HomeTab.categories:
        Navigator.of(context).push(MaterialPageRoute(builder: (_) => const CategoriesScreen()));
        break;
      case HomeTab.cart:
        break;
      case HomeTab.profile:
        Navigator.of(context).push(MaterialPageRoute(builder: (_) => const ProfileScreen()));
        break;
    }
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
              child: FutureBuilder<CartModel>(
                future: _cartFuture,
                builder: (context, snapshot) {
                  if (snapshot.connectionState == ConnectionState.waiting) {
                    return const Center(child: CircularProgressIndicator());
                  }
                  if (snapshot.hasError) {
                    return _buildErrorState(colors, snapshot.error);
                  }
                  final cart = snapshot.data ?? CartModel.empty;
                  if (cart.items.isEmpty) return _buildEmptyState(colors);
                  return ListView.separated(
                    padding: const EdgeInsets.all(16),
                    itemCount: cart.items.length,
                    separatorBuilder: (_, _) => const SizedBox(height: 12),
                    itemBuilder: (context, index) {
                      final item = cart.items[index];
                      return CartItemRow(
                        item: item,
                        onQuantityChanged: (q) => _updateQuantity(item.id, q),
                        onRemove: () => _removeItem(item.id),
                      );
                    },
                  );
                },
              ),
            ),
            FutureBuilder<CartModel>(
              future: _cartFuture,
              builder: (context, snapshot) {
                final cart = snapshot.data;
                if (cart == null || cart.items.isEmpty) return const SizedBox.shrink();
                return _buildSummaryBar(context, colors, cart);
              },
            ),
          ],
        ),
      ),
      bottomNavigationBar: CustomBottomNavigation(currentTab: HomeTab.cart, onTabSelected: _onTabSelected),
    );
  }

  Widget _buildHeader(AppColorScheme colors) {
    return Container(
      color: colors.surface,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      child: Row(
        children: [
          const AppLogo(variant: AppLogoVariant.iconColor, size: 24),
          const SizedBox(width: 10),
          Text('Mon Panier', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: colors.textDark)),
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
            Icon(Icons.shopping_cart_outlined, size: 56, color: colors.textMuted),
            const SizedBox(height: 16),
            Text('Votre panier est vide', style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
            const SizedBox(height: 6),
            Text('Ajoutez des produits pour commencer vos achats.', textAlign: TextAlign.center, style: TextStyle(fontSize: 13, color: colors.textMuted)),
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
              error is ApiException ? error.message : 'Impossible de charger le panier.',
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

  Widget _buildSummaryBar(BuildContext context, AppColorScheme colors, CartModel cart) {
    return Container(
      padding: const EdgeInsets.fromLTRB(16, 14, 16, 14),
      decoration: BoxDecoration(color: colors.surface, border: Border(top: BorderSide(color: colors.border))),
      child: SafeArea(
        top: false,
        child: Column(
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text('Sous-total (${cart.itemCount} article${cart.itemCount > 1 ? 's' : ''})', style: TextStyle(color: colors.textMuted)),
                Text(formatFcfa(cart.totalAmount), style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
              ],
            ),
            const SizedBox(height: 12),
            PrimaryButton(
              label: 'Passer la commande',
              onPressed: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const CheckoutScreen())),
            ),
          ],
        ),
      ),
    );
  }
}
