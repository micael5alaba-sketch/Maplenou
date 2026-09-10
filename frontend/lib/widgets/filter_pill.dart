import 'package:flutter/material.dart';

import '../theme/app_colors.dart';

/// Rounded filter chip used in a horizontal filter row (e.g. "Tout voir",
/// "Catégorie", "Prix" on the results screen). [isActive] renders it as a
/// filled brand-green pill; otherwise it's an outlined pill, optionally
/// with a trailing dropdown chevron.
class FilterPill extends StatelessWidget {
  final String label;
  final bool isActive;
  final bool showChevron;
  final VoidCallback? onTap;

  const FilterPill({
    super.key,
    required this.label,
    this.isActive = false,
    this.showChevron = false,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 9),
        decoration: BoxDecoration(
          color: isActive ? AppColors.primary : Colors.white,
          borderRadius: BorderRadius.circular(20),
          border: isActive ? null : Border.all(color: AppColors.border),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              label,
              style: TextStyle(
                color: isActive ? Colors.white : AppColors.textDark,
                fontSize: 13,
                fontWeight: isActive ? FontWeight.w600 : FontWeight.w500,
              ),
            ),
            if (showChevron) ...[
              const SizedBox(width: 4),
              Icon(
                Icons.keyboard_arrow_down_rounded,
                size: 16,
                color: isActive ? Colors.white : AppColors.textMuted,
              ),
            ],
          ],
        ),
      ),
    );
  }
}
