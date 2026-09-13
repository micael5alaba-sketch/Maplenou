import 'package:flutter/material.dart';

import '../models/cart_item_model.dart';
import '../theme/app_color_scheme.dart';
import '../utils/formatters.dart';
import 'quantity_selector.dart';

/// One row in [CartScreen]: thumbnail, name/variant, quantity stepper,
/// line total, and a remove button.
class CartItemRow extends StatelessWidget {
  final CartItemModel item;
  final ValueChanged<int> onQuantityChanged;
  final VoidCallback onRemove;

  const CartItemRow({
    super.key,
    required this.item,
    required this.onQuantityChanged,
    required this.onRemove,
  });

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 10, offset: const Offset(0, 3))],
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          ClipRRect(
            borderRadius: BorderRadius.circular(12),
            child: SizedBox(
              width: 64,
              height: 64,
              child: item.imageUrl != null
                  ? Image.network(item.imageUrl!, fit: BoxFit.cover, errorBuilder: (_, _, _) => _placeholder(colors))
                  : _placeholder(colors),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(item.productName, maxLines: 2, overflow: TextOverflow.ellipsis, style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark)),
                if (item.variantLabel != null) ...[
                  const SizedBox(height: 2),
                  Text(item.variantLabel!, style: TextStyle(fontSize: 12, color: colors.textMuted)),
                ],
                const SizedBox(height: 8),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    QuantitySelector(quantity: item.quantity, onChanged: onQuantityChanged),
                    Text(formatFcfa(item.lineTotal), style: TextStyle(fontWeight: FontWeight.bold, color: colors.primary)),
                  ],
                ),
              ],
            ),
          ),
          IconButton(
            icon: Icon(Icons.close_rounded, size: 18, color: colors.textMuted),
            onPressed: onRemove,
          ),
        ],
      ),
    );
  }

  Widget _placeholder(AppColorScheme colors) {
    return Container(color: colors.inputFill, alignment: Alignment.center, child: Icon(Icons.image_not_supported_outlined, color: colors.textMuted));
  }
}
