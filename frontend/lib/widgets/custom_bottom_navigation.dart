import 'package:flutter/material.dart';

import '../theme/app_color_scheme.dart';

/// The four destinations reachable from [CustomBottomNavigation].
enum HomeTab { home, categories, cart, profile }

/// Fixed bottom navigation bar with 4 tabs (Home, Catégories, Panier,
/// Profil). The active tab is shown in the brand green, inactive ones in
/// grey.
class CustomBottomNavigation extends StatelessWidget {
  final HomeTab currentTab;
  final ValueChanged<HomeTab> onTabSelected;

  const CustomBottomNavigation({
    super.key,
    required this.currentTab,
    required this.onTabSelected,
  });

  static const _items = [
    (tab: HomeTab.home, icon: Icons.home_rounded, label: 'Home'),
    (tab: HomeTab.categories, icon: Icons.grid_view_rounded, label: 'Catégories'),
    (tab: HomeTab.cart, icon: Icons.shopping_cart_rounded, label: 'Panier'),
    (tab: HomeTab.profile, icon: Icons.person_rounded, label: 'Profil'),
  ];

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: BoxDecoration(
        color: context.colors.surface,
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.06),
            blurRadius: 12,
            offset: const Offset(0, -4),
          ),
        ],
      ),
      child: SafeArea(
        top: false,
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 8),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: [for (final item in _items) _buildItem(context, item)],
          ),
        ),
      ),
    );
  }

  Widget _buildItem(BuildContext context, ({HomeTab tab, IconData icon, String label}) item) {
    final isActive = item.tab == currentTab;
    final color = isActive ? context.colors.primary : context.colors.textMuted;

    return GestureDetector(
      onTap: () => onTabSelected(item.tab),
      behavior: HitTestBehavior.opaque,
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(item.icon, color: color, size: 24),
          const SizedBox(height: 4),
          Text(
            item.label,
            style: TextStyle(
              color: color,
              fontSize: 11,
              fontWeight: isActive ? FontWeight.w600 : FontWeight.w400,
            ),
          ),
        ],
      ),
    );
  }
}
