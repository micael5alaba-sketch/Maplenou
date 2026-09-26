import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../services/auth_service.dart';
import '../services/courier_pin_service.dart';
import '../theme/app_color_scheme.dart';
import 'login_screen.dart';

/// Verrou local affiché au lancement de l'app pour un compte livreur ayant
/// configuré un code PIN (`CourierSettingsScreen`) — s'affiche avant
/// [child], jamais un appel réseau (vérifié uniquement contre
/// `CourierPinService`, qui lit le Keychain/Keystore local).
class CourierPinLockScreen extends StatefulWidget {
  final Widget child;

  const CourierPinLockScreen({super.key, required this.child});

  @override
  State<CourierPinLockScreen> createState() => _CourierPinLockScreenState();
}

class _CourierPinLockScreenState extends State<CourierPinLockScreen> {
  final _pinService = CourierPinService();
  final _controller = TextEditingController();
  String? _errorMessage;
  bool _isUnlocked = false;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _handleSubmit() async {
    final pin = _controller.text;
    if (pin.length != 4) return;

    if (await _pinService.verifyPin(pin)) {
      if (!mounted) return;
      setState(() => _isUnlocked = true);
    } else {
      setState(() {
        _errorMessage = 'Code incorrect.';
        _controller.clear();
      });
    }
  }

  Future<void> _handleLogout() async {
    await AuthService().logout();
    if (!mounted) return;
    Navigator.of(context).pushAndRemoveUntil(
      MaterialPageRoute(builder: (_) => const LoginScreen()),
      (route) => false,
    );
  }

  @override
  Widget build(BuildContext context) {
    if (_isUnlocked) return widget.child;

    final colors = context.colors;

    return Scaffold(
      backgroundColor: colors.homeBackground,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Icon(Icons.lock_outline_rounded, size: 48, color: colors.primary),
              const SizedBox(height: 16),
              Text('Code PIN', textAlign: TextAlign.center, style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: colors.textDark)),
              const SizedBox(height: 4),
              Text(
                "Entrez votre code pour continuer.",
                textAlign: TextAlign.center,
                style: TextStyle(fontSize: 13, color: colors.textMuted),
              ),
              const SizedBox(height: 24),
              TextField(
                controller: _controller,
                autofocus: true,
                obscureText: true,
                textAlign: TextAlign.center,
                keyboardType: TextInputType.number,
                inputFormatters: [FilteringTextInputFormatter.digitsOnly, LengthLimitingTextInputFormatter(4)],
                style: TextStyle(fontSize: 28, letterSpacing: 16, color: colors.textDark),
                decoration: InputDecoration(
                  counterText: '',
                  filled: true,
                  fillColor: colors.inputFill,
                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(14), borderSide: BorderSide.none),
                  errorText: _errorMessage,
                ),
                onChanged: (value) {
                  if (_errorMessage != null) setState(() => _errorMessage = null);
                  if (value.length == 4) _handleSubmit();
                },
              ),
              const SizedBox(height: 20),
              TextButton(onPressed: _handleLogout, child: const Text('Code oublié ? Se déconnecter')),
            ],
          ),
        ),
      ),
    );
  }
}
