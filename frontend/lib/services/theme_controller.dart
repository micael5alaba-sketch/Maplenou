import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Holds the app's current [ThemeMode] and persists the user's choice
/// on-device, so it survives an app restart. Listened to by [MaplenouApp]
/// to rebuild `MaterialApp` whenever the mode changes.
/// App-wide instance, read by [MaplenouApp] and by any screen that lets
/// the user change the theme (e.g. the "Thème" row on [ProfileScreen]).
final themeController = ThemeController();

class ThemeController extends ValueNotifier<ThemeMode> {
  static const _themeModeKey = 'theme_mode';

  ThemeController() : super(ThemeMode.system);

  Future<void> load() async {
    final prefs = await SharedPreferences.getInstance();
    final stored = prefs.getString(_themeModeKey);
    value = ThemeMode.values.firstWhere(
      (mode) => mode.name == stored,
      orElse: () => ThemeMode.system,
    );
  }

  Future<void> setThemeMode(ThemeMode mode) async {
    value = mode;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_themeModeKey, mode.name);
  }
}
