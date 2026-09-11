import 'package:flutter/material.dart';

/// Small colored rounded tag ("Bio", "Promotion", "Nouveau"...). The color
/// is picked from the label so the same tag always looks the same
/// wherever it's shown.
class BadgeChip extends StatelessWidget {
  final String label;

  const BadgeChip({super.key, required this.label});

  static const Map<String, Color> _colors = {
    'bio': Color(0xFF4C8C4A),
    'promotion': Color(0xFFE0533D),
    'nouveau': Color(0xFF2E6F9E),
    'artisanal': Color(0xFF8B5E3C),
    'électronique': Color(0xFF546E7A),
    'mode': Color(0xFFB1467F),
    'maison': Color(0xFF3C8C89),
    'beauté': Color(0xFF9C6ADE),
  };

  Color get _color => _colors[label.toLowerCase()] ?? const Color(0xFF6B7280);

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(
        color: _color.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: _color.withValues(alpha: 0.4)),
      ),
      child: Text(
        label,
        style: TextStyle(color: _color, fontWeight: FontWeight.w600, fontSize: 12),
      ),
    );
  }
}
