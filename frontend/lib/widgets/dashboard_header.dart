import 'package:flutter/material.dart';

import '../theme/app_color_scheme.dart';
import 'app_logo.dart';

/// Fixed top bar shared by the seller dashboard: hamburger menu, centered
/// logo, notification bell. White background, no shadow.
class DashboardHeader extends StatelessWidget {
  final VoidCallback onMenuTap;
  final VoidCallback onNotificationTap;

  const DashboardHeader({
    super.key,
    required this.onMenuTap,
    required this.onNotificationTap,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      color: context.colors.surface,
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      child: Row(
        children: [
          IconButton(
            icon: Icon(Icons.menu_rounded, color: context.colors.textDark),
            onPressed: onMenuTap,
          ),
          const Expanded(
            child: Center(
              child: AppLogo(variant: AppLogoVariant.horizontalColor, size: 34),
            ),
          ),
          IconButton(
            icon: Icon(Icons.notifications_none_rounded, color: context.colors.textDark),
            onPressed: onNotificationTap,
          ),
        ],
      ),
    );
  }
}
