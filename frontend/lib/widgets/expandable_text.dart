import 'package:flutter/material.dart';

import '../theme/app_color_scheme.dart';

/// Body text clamped to a few lines with a "Lire la suite" / "Réduire"
/// toggle to expand/collapse.
class ExpandableText extends StatefulWidget {
  final String text;
  final int collapsedMaxLines;

  const ExpandableText({super.key, required this.text, this.collapsedMaxLines = 3});

  @override
  State<ExpandableText> createState() => _ExpandableTextState();
}

class _ExpandableTextState extends State<ExpandableText> {
  bool _expanded = false;

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          widget.text,
          maxLines: _expanded ? null : widget.collapsedMaxLines,
          overflow: _expanded ? TextOverflow.visible : TextOverflow.ellipsis,
          style: TextStyle(color: colors.textDark, height: 1.4),
        ),
        GestureDetector(
          onTap: () => setState(() => _expanded = !_expanded),
          child: Padding(
            padding: const EdgeInsets.only(top: 4),
            child: Text(
              _expanded ? 'Réduire' : 'Lire la suite',
              style: TextStyle(color: colors.primary, fontWeight: FontWeight.w600),
            ),
          ),
        ),
      ],
    );
  }
}
