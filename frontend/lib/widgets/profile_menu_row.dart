import 'package:flutter/material.dart';

import '../theme/app_color_scheme.dart';

/// One tappable row used inside [AccountSectionCard]: an icon, a title, an
/// optional subtitle, an optional trailing badge, and a chevron.
class ProfileMenuRow extends StatelessWidget {
  final IconData icon;
  final String title;
  final String? subtitle;
  final String? badgeLabel;
  final Color? badgeColor;
  final VoidCallback? onTap;

  const ProfileMenuRow({
    super.key,
    required this.icon,
    required this.title,
    this.subtitle,
    this.badgeLabel,
    this.badgeColor,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    final resolvedBadgeColor = badgeColor ?? colors.accentOrange;

    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 12),
        child: Row(
          children: [
            Icon(icon, color: colors.primary, size: 22),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title, style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark)),
                  if (subtitle != null) ...[
                    const SizedBox(height: 2),
                    Text(subtitle!, style: TextStyle(fontSize: 12, color: colors.textMuted)),
                  ],
                ],
              ),
            ),
            if (badgeLabel != null) ...[
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(
                  color: resolvedBadgeColor.withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Text(
                  badgeLabel!,
                  style: TextStyle(color: resolvedBadgeColor, fontWeight: FontWeight.w600, fontSize: 11),
                ),
              ),
              const SizedBox(width: 8),
            ],
            Icon(Icons.chevron_right_rounded, color: colors.textMuted),
          ],
        ),
      ),
    );
  }
}
