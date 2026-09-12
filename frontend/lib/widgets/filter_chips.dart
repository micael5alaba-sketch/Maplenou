import 'package:flutter/material.dart';

import 'filter_pill.dart';

/// One entry in a [FilterChips] row: a label, how many items it matches,
/// and the value it filters by (`null` conventionally means "no filter").
class FilterChipItem<T> {
  final String label;
  final int count;
  final T value;

  const FilterChipItem({required this.label, required this.count, required this.value});
}

/// Horizontal, scrollable row of filter chips with a match count next to
/// each label (e.g. "Tous (42)", "En stock (38)") — single selection.
/// Built on top of [FilterPill] for the same look as the rest of the app.
class FilterChips<T> extends StatelessWidget {
  final List<FilterChipItem<T>> items;
  final T selected;
  final ValueChanged<T> onSelected;

  const FilterChips({
    super.key,
    required this.items,
    required this.selected,
    required this.onSelected,
  });

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: Row(
        children: [
          for (var i = 0; i < items.length; i++) ...[
            if (i != 0) const SizedBox(width: 8),
            FilterPill(
              label: '${items[i].label} (${items[i].count})',
              isActive: items[i].value == selected,
              onTap: () => onSelected(items[i].value),
            ),
          ],
        ],
      ),
    );
  }
}
