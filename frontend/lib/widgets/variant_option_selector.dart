import 'package:flutter/material.dart';

import '../models/variant_group_model.dart';
import '../theme/app_color_scheme.dart';

/// One variant group ("Format", "Taille", "Couleur"...) rendered as a row
/// of selectable chips. The selected option gets a light green fill and a
/// green border.
class VariantOptionSelector extends StatelessWidget {
  final VariantGroupModel group;
  final String? selectedOption;
  final ValueChanged<String> onSelected;

  const VariantOptionSelector({
    super.key,
    required this.group,
    required this.selectedOption,
    required this.onSelected,
  });

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          group.label,
          style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark, fontSize: 15),
        ),
        const SizedBox(height: 10),
        Wrap(
          spacing: 10,
          runSpacing: 10,
          children: group.options.map((option) {
            final isSelected = option == selectedOption;
            return GestureDetector(
              onTap: () => onSelected(option),
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 10),
                decoration: BoxDecoration(
                  color: isSelected ? colors.primary.withValues(alpha: 0.12) : colors.surface,
                  borderRadius: BorderRadius.circular(10),
                  border: Border.all(
                    color: isSelected ? colors.primary : colors.border,
                    width: isSelected ? 1.6 : 1,
                  ),
                ),
                child: Text(
                  option,
                  style: TextStyle(
                    color: isSelected ? colors.primary : colors.textDark,
                    fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                  ),
                ),
              ),
            );
          }).toList(),
        ),
      ],
    );
  }
}
