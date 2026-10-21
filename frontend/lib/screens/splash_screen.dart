import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../services/app_router.dart';
import '../services/push_notification_service.dart';
import '../services/session_service.dart';
import '../services/token_storage.dart';
import '../services/user_service.dart';
import '../theme/app_colors.dart';
import '../widgets/app_logo.dart';
import 'login_screen.dart';
import 'onboarding_screen.dart';

/// App entry screen: minimalist full-screen brand splash.
/// Deep forest green background with the Maplenou logo centered,
/// no buttons, no loader, no extra copy.
///
/// After a short delay, checks whether a JWT is stored ([TokenStorage]): no
/// token (ou invalide/expiré) envoie un tout premier visiteur vers
/// [OnboardingScreen] (une seule fois, voir [SessionService.hasSeenOnboarding]),
/// puis [LoginScreen] pour les visites suivantes ; une session valide récupère
/// le vrai compte (`GET /api/users/me`) et laisse [AppRouter] décider quelle
/// interface ouvrir selon ses capacités réelles (Acheteur/Vendeur/Livreur —
/// voir `CLAUDE.md` §3).
class SplashScreen extends StatefulWidget {
  const SplashScreen({super.key});

  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen> {
  Timer? _redirectTimer;

  @override
  void initState() {
    super.initState();
    _redirectTimer = Timer(const Duration(seconds: 5), _navigateNext);
  }

  @override
  void dispose() {
    _redirectTimer?.cancel();
    super.dispose();
  }

  Future<void> _navigateNext() async {
    final target = await _resolveTarget();
    if (!mounted) return;

    Navigator.of(context).pushReplacement(MaterialPageRoute(builder: (_) => target));
  }

  Future<Widget> _resolveTarget() async {
    final token = await TokenStorage().getAccessToken();
    if (token == null) {
      final hasSeenOnboarding = await SessionService().hasSeenOnboarding();
      return hasSeenOnboarding ? const LoginScreen() : const OnboardingScreen();
    }

    try {
      final user = await UserService().getCurrentUser();
      PushNotificationService().initialize();
      return await AppRouter.resolveHomeScreen(user);
    } catch (_) {
      // Token expiré/révoqué et rafraîchissement impossible : repart de zéro.
      await TokenStorage().clear();
      return const LoginScreen();
    }
  }

  @override
  Widget build(BuildContext context) {
    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: SystemUiOverlayStyle.light.copyWith(
        statusBarColor: Colors.transparent,
        systemNavigationBarColor: AppColors.primary,
      ),
      child: const Scaffold(
        backgroundColor: AppColors.primary,
        body: Center(
          child: AppLogo(variant: AppLogoVariant.stackedWhite, size: 350),
        ),
      ),
    );
  }
}
