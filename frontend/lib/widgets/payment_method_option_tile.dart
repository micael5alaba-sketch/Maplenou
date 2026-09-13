import 'package:flutter/material.dart';

import '../models/payment_method_option_model.dart';
import '../theme/app_color_scheme.dart';

/// One selectable row in [CheckoutScreen]'s "Choisissez un moyen de
/// paiement" section — a radio button, a label/subtitle, and a colored
/// provider badge.
class PaymentMethodOptionTile extends StatelessWidget {
  final PaymentMethodOptionModel option;
  final bool isSelected;
  final VoidCallback onTap;

  const PaymentMethodOptionTile({
    super.key,
    required this.option,
    required this.isSelected,
    required this.onTap,
  });

  Color _badgeColor() {
    switch (option.type) {
      case PaymentMethodType.tMoney:
        return const Color(0xFFF5C518);
      case PaymentMethodType.flooz:
        return const Color(0xFF1E5FCB);
      case PaymentMethodType.stripe:
        return Colors.transparent;
    }
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(14),
      child: Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: isSelected ? colors.primary.withValues(alpha: 0.06) : colors.surface,
          borderRadius: BorderRadius.circular(14),
          border: Border.all(color: isSelected ? colors.primary : colors.border, width: isSelected ? 1.6 : 1),
        ),
        child: Row(
          children: [
            _buildRadioDot(colors),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(option.title, style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark)),
                  Text(option.subtitle, style: TextStyle(fontSize: 12, color: colors.textMuted)),
                ],
              ),
            ),
            if (option.type == PaymentMethodType.stripe)
              Icon(Icons.credit_card_rounded, color: colors.textDark)
            else
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(color: _badgeColor(), borderRadius: BorderRadius.circular(6)),
                child: Text(option.badgeLabel, style: const TextStyle(color: Colors.white, fontSize: 11, fontWeight: FontWeight.bold)),
              ),
          ],
        ),
      ),
    );
  }

  /// A plain visual radio dot — no built-in `Radio` widget here since
  /// selection is driven entirely by [isSelected]/[onTap] from the parent,
  /// not by an ancestor `RadioGroup`.
  Widget _buildRadioDot(AppColorScheme colors) {
    return Container(
      width: 20,
      height: 20,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        border: Border.all(color: isSelected ? colors.primary : colors.border, width: 2),
      ),
      child: isSelected
          ? Center(child: Container(width: 10, height: 10, decoration: BoxDecoration(shape: BoxShape.circle, color: colors.primary)))
          : null,
    );
  }
}
