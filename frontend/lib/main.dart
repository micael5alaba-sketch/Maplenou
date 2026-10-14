import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';

import 'firebase_options.dart';
import 'l10n/app_localizations.dart';
import 'navigation.dart';
import 'screens/splash_screen.dart';
import 'services/locale_controller.dart';
import 'services/push_notification_service.dart';
import 'services/theme_controller.dart';
import 'theme/app_theme.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  try {
    await Firebase.initializeApp(options: DefaultFirebaseOptions.currentPlatform);
    FirebaseMessaging.onBackgroundMessage(firebaseMessagingBackgroundHandler);
  } catch (_) {
    // Le tracking GPS temps réel (Firebase Realtime Database) et les
    // notifications push seront simplement indisponibles sur cette
    // plateforme/config — le reste de l'app ne doit pas planter pour autant.
  }
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
    localeController.load();
  }

  @override
  Widget build(BuildContext context) {
    return ValueListenableBuilder<ThemeMode>(
      valueListenable: themeController,
      builder: (context, themeMode, child) {
        return ValueListenableBuilder<AppLanguage>(
          valueListenable: localeController,
          builder: (context, language, child) {
            return MaterialApp(
              navigatorKey: navigatorKey,
              title: 'Maplenou',
              debugShowCheckedModeBanner: false,
              themeMode: themeMode,
              theme: AppTheme.light,
              darkTheme: AppTheme.dark,
              locale: language.locale,
              supportedLocales: AppLocalizations.supportedLocales,
              localizationsDelegates: const [
                AppLocalizations.delegate,
                GlobalMaterialLocalizations.delegate,
                GlobalWidgetsLocalizations.delegate,
                GlobalCupertinoLocalizations.delegate,
              ],
              home: child,
            );
          },
          child: child,
        );
      },
      child: const SplashScreen(),
    );
  }
}
