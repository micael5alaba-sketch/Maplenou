import 'package:flutter/material.dart';

import '../theme/app_color_scheme.dart';

/// [-] N [+] stepper for choosing an order quantity.
class QuantitySelector extends StatelessWidget {
  final int quantity;
  final ValueChanged<int> onChanged;
  final int min;
  final int max;

  const QuantitySelector({
    super.key,
    required this.quantity,
    required this.onChanged,
    this.min = 1,
    this.max = 99,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(30),
        color: context.colors.inputFill,
      ),
      padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 4),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          _buildButton(context, Icons.remove_rounded, quantity > min ? () => onChanged(quantity - 1) : null),
          SizedBox(
            width: 36,
            child: Text(
              '$quantity',
              textAlign: TextAlign.center,
              style: TextStyle(fontWeight: FontWeight.bold, color: context.colors.textDark),
            ),
          ),
          _buildButton(context, Icons.add_rounded, quantity < max ? () => onChanged(quantity + 1) : null),
        ],
      ),
    );
  }

  Widget _buildButton(BuildContext context, IconData icon, VoidCallback? onTap) {
    final colors = context.colors;

    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(20),
      child: Container(
        width: 32,
        height: 32,
        alignment: Alignment.center,
        child: Icon(icon, size: 18, color: onTap == null ? colors.textMuted : colors.textDark),
      ),
    );
  }
}
