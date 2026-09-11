import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../services/session_service.dart';
import '../theme/app_colors.dart';
import '../widgets/app_logo.dart';
import 'home_screen.dart';
import 'profile_selection_screen.dart';

/// App entry screen: minimalist full-screen brand splash.
/// Deep forest green background with the Maplenou logo centered,
/// no buttons, no loader, no extra copy.
///
/// After a short delay, it checks [SessionService] for a profile chosen on
/// a previous visit: a returning user is sent straight to that profile's
/// home screen, while a first-time visitor goes through profile selection.
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
    final roleId = await SessionService().getSelectedRoleId();
    if (!mounted) return;

    Navigator.of(context).pushReplacement(
      MaterialPageRoute(builder: (_) => _screenForRole(roleId)),
    );
  }

  /// Only the "Acheteur" profile has its home screen built so far; any
  /// other saved role (or none at all) falls back to profile selection.
  Widget _screenForRole(String? roleId) {
    switch (roleId) {
      case 'buyer':
        return const HomeScreen();
      default:
        return const ProfileSelectionScreen();
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
