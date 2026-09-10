import 'package:flutter/material.dart';

import 'app_color_scheme.dart';

class AppTheme {
  AppTheme._();

  static ThemeData get light => _buildTheme(AppColorScheme.light, Brightness.light);

  static ThemeData get dark => _buildTheme(AppColorScheme.dark, Brightness.dark);

  static ThemeData _buildTheme(AppColorScheme colors, Brightness brightness) {
    return ThemeData(
      useMaterial3: true,
      brightness: brightness,
      scaffoldBackgroundColor: colors.background,
      colorScheme: ColorScheme.fromSeed(
        seedColor: colors.primary,
        brightness: brightness,
        primary: colors.primary,
        error: colors.error,
      ),
      appBarTheme: AppBarTheme(
        backgroundColor: colors.background,
        foregroundColor: colors.textDark,
        elevation: 0,
        centerTitle: true,
      ),
      textSelectionTheme: TextSelectionThemeData(
        cursorColor: colors.primary,
        selectionHandleColor: colors.primary,
      ),
      extensions: [colors],
    );
  }
}
