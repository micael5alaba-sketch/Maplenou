import 'package:flutter/material.dart';

/// Fixed brand colors, used only where a compile-time `const` is required
/// and the color must stay the same regardless of light/dark mode (the
/// splash screen, the onboarding profile-selection header/role cards —
/// deliberately on-brand rather than theme-reactive).
///
/// Everywhere else, use `context.colors` from `app_color_scheme.dart`,
/// which does adapt to the current theme.
class AppColors {
  AppColors._();

  static const Color primary = Color(0xFF1E5A24);
  static const Color primaryDark = Color(0xFF163F24);
  static const Color sage = Color(0xFF8FAD86);
  static const Color accentOrange = Color(0xFFF57C00);
}
