import 'package:flutter/material.dart';

import 'screens/splash_screen.dart';
import 'services/theme_controller.dart';
import 'theme/app_theme.dart';

void main() {
  runApp(const MaplenouApp());
}

class MaplenouApp extends StatefulWidget {
  const MaplenouApp({super.key});

  @override
  State<MaplenouApp> createState() => _MaplenouAppState();
}

class _MaplenouAppState extends State<MaplenouApp> {
  @override
  void initState() {
    super.initState();
    themeController.load();
  }

  @override
  Widget build(BuildContext context) {
    return ValueListenableBuilder<ThemeMode>(
      valueListenable: themeController,
      builder: (context, themeMode, child) {
        return MaterialApp(
          title: 'Maplenou',
          debugShowCheckedModeBanner: false,
          themeMode: themeMode,
          theme: AppTheme.light,
          darkTheme: AppTheme.dark,
          home: child,
        );
      },
      child: const SplashScreen(),
    );
  }
}
