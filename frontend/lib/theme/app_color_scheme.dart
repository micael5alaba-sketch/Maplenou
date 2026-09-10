import 'package:flutter/material.dart';

/// Brand color palette, theme-aware via [ThemeExtension] so every widget
/// gets the right shade for light/dark mode through `context.colors`
/// instead of a hardcoded constant.
@immutable
class AppColorScheme extends ThemeExtension<AppColorScheme> {
  final Color primary;
  final Color primaryDark;
  final Color sage;
  final Color accentOrange;
  final Color background;
  final Color lightBackground;
  final Color homeBackground;
  final Color surface;
  final Color inputFill;
  final Color border;
  final Color textDark;
  final Color textMuted;
  final Color error;

  const AppColorScheme({
    required this.primary,
    required this.primaryDark,
    required this.sage,
    required this.accentOrange,
    required this.background,
    required this.lightBackground,
    required this.homeBackground,
    required this.surface,
    required this.inputFill,
    required this.border,
    required this.textDark,
    required this.textMuted,
    required this.error,
  });

  /// Light palette — the original Maplenou brand colors.
  static const light = AppColorScheme(
    primary: Color(0xFF1E5A24),
    primaryDark: Color(0xFF163F24),
    sage: Color(0xFF8FAD86),
    accentOrange: Color(0xFFF57C00),
    background: Color(0xFFFFFFFF),
    lightBackground: Color(0xFFF7F6F4),
    homeBackground: Color(0xFFF8F7F4),
    surface: Color(0xFFFFFFFF),
    inputFill: Color(0xFFF5F7F5),
    border: Color(0xFFE3E7E3),
    textDark: Color(0xFF1B1B1B),
    textMuted: Color(0xFF7A7A7A),
    error: Color(0xFFD64545),
  );

  /// Dark palette — WhatsApp-style green on a near-black background
  /// (same tones as WhatsApp's own dark mode: #0B141A background,
  /// #1F2C34 elevated surfaces, #00A884 accent green).
  static const dark = AppColorScheme(
    primary: Color(0xFF00A884),
    primaryDark: Color(0xFF075E54),
    sage: Color(0xFF6FA98A),
    accentOrange: Color(0xFFFFA726),
    background: Color(0xFF0B141A),
    lightBackground: Color(0xFF111B21),
    homeBackground: Color(0xFF0B141A),
    surface: Color(0xFF1F2C34),
    inputFill: Color(0xFF2A3942),
    border: Color(0xFF2A3942),
    textDark: Color(0xFFE9EDEF),
    textMuted: Color(0xFF8696A0),
    error: Color(0xFFFF6B6B),
  );

  @override
  AppColorScheme copyWith({
    Color? primary,
    Color? primaryDark,
    Color? sage,
    Color? accentOrange,
    Color? background,
    Color? lightBackground,
    Color? homeBackground,
    Color? surface,
    Color? inputFill,
    Color? border,
    Color? textDark,
    Color? textMuted,
    Color? error,
  }) {
    return AppColorScheme(
      primary: primary ?? this.primary,
      primaryDark: primaryDark ?? this.primaryDark,
      sage: sage ?? this.sage,
      accentOrange: accentOrange ?? this.accentOrange,
      background: background ?? this.background,
      lightBackground: lightBackground ?? this.lightBackground,
      homeBackground: homeBackground ?? this.homeBackground,
      surface: surface ?? this.surface,
      inputFill: inputFill ?? this.inputFill,
      border: border ?? this.border,
      textDark: textDark ?? this.textDark,
      textMuted: textMuted ?? this.textMuted,
      error: error ?? this.error,
    );
  }

  @override
  AppColorScheme lerp(ThemeExtension<AppColorScheme>? other, double t) {
    if (other is! AppColorScheme) return this;
    return AppColorScheme(
      primary: Color.lerp(primary, other.primary, t)!,
      primaryDark: Color.lerp(primaryDark, other.primaryDark, t)!,
      sage: Color.lerp(sage, other.sage, t)!,
      accentOrange: Color.lerp(accentOrange, other.accentOrange, t)!,
      background: Color.lerp(background, other.background, t)!,
      lightBackground: Color.lerp(lightBackground, other.lightBackground, t)!,
      homeBackground: Color.lerp(homeBackground, other.homeBackground, t)!,
      surface: Color.lerp(surface, other.surface, t)!,
      inputFill: Color.lerp(inputFill, other.inputFill, t)!,
      border: Color.lerp(border, other.border, t)!,
      textDark: Color.lerp(textDark, other.textDark, t)!,
      textMuted: Color.lerp(textMuted, other.textMuted, t)!,
      error: Color.lerp(error, other.error, t)!,
    );
  }
}

/// Ergonomic access: `context.colors.primary` instead of
/// `Theme.of(context).extension<AppColorScheme>()!.primary`.
extension AppColorSchemeContext on BuildContext {
  AppColorScheme get colors => Theme.of(this).extension<AppColorScheme>()!;
}
