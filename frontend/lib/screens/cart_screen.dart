import 'package:flutter/material.dart';

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
/// Backed by [CartService], an in-memory singleton shared across the app
/// session (see that service's doc comment for what real endpoints it
/// maps onto once auth is wired up).
class CartScreen extends StatefulWidget {
  const CartScreen({super.key});

  @override
  State<CartScreen> createState() => _CartScreenState();
}

class _CartScreenState extends State<CartScreen> {
  final _cart = CartService();

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
    final items = _cart.items;

    return Scaffold(
      backgroundColor: colors.homeBackground,
      body: SafeArea(
        child: Column(
          children: [
            _buildHeader(colors),
            Expanded(
              child: items.isEmpty
                  ? _buildEmptyState(colors)
                  : ListView.separated(
                      padding: const EdgeInsets.all(16),
                      itemCount: items.length,
                      separatorBuilder: (_, _) => const SizedBox(height: 12),
                      itemBuilder: (context, index) {
                        final item = items[index];
                        return CartItemRow(
                          item: item,
                          onQuantityChanged: (q) => setState(() => _cart.updateQuantity(item.id, q)),
                          onRemove: () => setState(() => _cart.removeItem(item.id)),
                        );
                      },
                    ),
            ),
            if (items.isNotEmpty) _buildSummaryBar(context, colors),
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

  Widget _buildSummaryBar(BuildContext context, AppColorScheme colors) {
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
                Text('Sous-total (${_cart.itemCount} article${_cart.itemCount > 1 ? 's' : ''})', style: TextStyle(color: colors.textMuted)),
                Text(formatFcfa(_cart.subtotal), style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
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
