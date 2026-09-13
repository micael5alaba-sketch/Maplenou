import 'package:flutter/material.dart';

import '../theme/app_color_scheme.dart';

/// "Hors ligne / En ligne" segmented pill at the top of
/// [CourierDashboardScreen].
class OnlineStatusToggle extends StatelessWidget {
  final bool isOnline;
  final ValueChanged<bool> onChanged;

  const OnlineStatusToggle({super.key, required this.isOnline, required this.onChanged});

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return Container(
      padding: const EdgeInsets.all(4),
      decoration: BoxDecoration(
        color: colors.inputFill,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: colors.border),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          _buildOption(colors, 'Hors ligne', !isOnline, () => onChanged(false)),
          _buildOption(colors, 'En ligne', isOnline, () => onChanged(true)),
        ],
      ),
    );
  }

  Widget _buildOption(AppColorScheme colors, String label, bool selected, VoidCallback onTap) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 8),
        decoration: BoxDecoration(
          color: selected ? colors.primary : Colors.transparent,
          borderRadius: BorderRadius.circular(20),
        ),
        child: Text(
          label,
          style: TextStyle(
            color: selected ? Colors.white : colors.textMuted,
            fontWeight: selected ? FontWeight.w700 : FontWeight.w500,
            fontSize: 13,
          ),
        ),
      ),
    );
  }
}
