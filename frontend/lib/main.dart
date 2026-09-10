import 'package:flutter/material.dart';

import 'screens/splash_screen.dart';
import 'theme/app_theme.dart';

void main() {
  runApp(const MaplenouApp());
}

class MaplenouApp extends StatelessWidget {
  const MaplenouApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Maplenou',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light,
      home: const SplashScreen(),
    );
  }
}
