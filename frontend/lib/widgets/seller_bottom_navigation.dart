import 'package:flutter/material.dart';

import '../theme/app_color_scheme.dart';

/// The four destinations reachable from [SellerBottomNavigation].
enum SellerTab { dashboard, orders, products, profile }

/// Fixed bottom navigation bar for the seller space (Dashboard, Commandes,
/// Produits, Profil). The active tab is shown in the brand green,
/// inactive ones in grey.
class SellerBottomNavigation extends StatelessWidget {
  final SellerTab currentTab;
  final ValueChanged<SellerTab> onTabSelected;

  const SellerBottomNavigation({
    super.key,
    required this.currentTab,
    required this.onTabSelected,
  });

  static const _items = [
    (tab: SellerTab.dashboard, icon: Icons.space_dashboard_rounded, label: 'Dashboard'),
    (tab: SellerTab.orders, icon: Icons.receipt_long_rounded, label: 'Commandes'),
    (tab: SellerTab.products, icon: Icons.inventory_2_rounded, label: 'Produits'),
    (tab: SellerTab.profile, icon: Icons.person_rounded, label: 'Profil'),
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

  Widget _buildItem(BuildContext context, ({SellerTab tab, IconData icon, String label}) item) {
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
