import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../services/courier_pin_service.dart';
import '../theme/app_color_scheme.dart';

enum _PinStep { enterCurrent, enterNew, confirmNew }

/// "Changer le code PIN" — crée, modifie ou désactive le verrou local
/// (`CourierPinService`). Si un code existe déjà, il faut le saisir avant
/// d'en définir un nouveau.
class CourierPinSettingsScreen extends StatefulWidget {
  const CourierPinSettingsScreen({super.key});

  @override
  State<CourierPinSettingsScreen> createState() => _CourierPinSettingsScreenState();
}

class _CourierPinSettingsScreenState extends State<CourierPinSettingsScreen> {
  final _pinService = CourierPinService();
  final _controller = TextEditingController();

  bool _hasExistingPin = false;
  bool _isLoading = true;
  late _PinStep _step;
  String? _newPin;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _init();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _init() async {
    final hasPin = await _pinService.isPinSet();
    if (!mounted) return;
    setState(() {
      _hasExistingPin = hasPin;
      _step = hasPin ? _PinStep.enterCurrent : _PinStep.enterNew;
      _isLoading = false;
    });
  }

  Future<void> _disablePin() async {
    await _pinService.clearPin();
    if (!mounted) return;
    Navigator.of(context).pop();
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(const SnackBar(content: Text('Code PIN désactivé.')));
  }

  Future<void> _handleSubmit() async {
    final pin = _controller.text;
    if (pin.length != 4) {
      setState(() => _errorMessage = 'Le code doit faire 4 chiffres.');
      return;
    }

    switch (_step) {
      case _PinStep.enterCurrent:
        if (!await _pinService.verifyPin(pin)) {
          setState(() => _errorMessage = 'Code incorrect.');
          return;
        }
        setState(() {
          _step = _PinStep.enterNew;
          _controller.clear();
          _errorMessage = null;
        });
      case _PinStep.enterNew:
        setState(() {
          _newPin = pin;
          _step = _PinStep.confirmNew;
          _controller.clear();
          _errorMessage = null;
        });
      case _PinStep.confirmNew:
        if (pin != _newPin) {
          setState(() {
            _errorMessage = 'Les deux codes ne correspondent pas.';
            _step = _PinStep.enterNew;
            _newPin = null;
            _controller.clear();
          });
          return;
        }
        await _pinService.setPin(pin);
        if (!mounted) return;
        Navigator.of(context).pop();
        ScaffoldMessenger.of(context)
          ..hideCurrentSnackBar()
          ..showSnackBar(SnackBar(content: Text(_hasExistingPin ? 'Code PIN modifié.' : 'Code PIN activé.')));
    }
  }

  String get _title {
    switch (_step) {
      case _PinStep.enterCurrent:
        return 'Code PIN actuel';
      case _PinStep.enterNew:
        return _hasExistingPin ? 'Nouveau code PIN' : 'Créer un code PIN';
      case _PinStep.confirmNew:
        return 'Confirmer le code PIN';
    }
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return Scaffold(
      backgroundColor: colors.homeBackground,
      body: SafeArea(
        child: Column(
          children: [
            _buildHeader(colors),
            if (_isLoading)
              const Expanded(child: Center(child: CircularProgressIndicator()))
            else
              Expanded(
                child: Padding(
                  padding: const EdgeInsets.all(24),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      const SizedBox(height: 24),
                      Icon(Icons.dialpad_rounded, size: 48, color: colors.primary),
                      const SizedBox(height: 16),
                      Text(_title, textAlign: TextAlign.center, style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: colors.textDark)),
                      const SizedBox(height: 4),
                      Text(
                        "Ce code protège l'accès à l'application sur cet appareil, en plus de votre mot de passe.",
                        textAlign: TextAlign.center,
                        style: TextStyle(fontSize: 12, color: colors.textMuted),
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
                        onSubmitted: (_) => _handleSubmit(),
                      ),
                      const SizedBox(height: 20),
                      SizedBox(
                        height: 50,
                        child: ElevatedButton(
                          onPressed: _handleSubmit,
                          style: ElevatedButton.styleFrom(
                            backgroundColor: colors.primary,
                            foregroundColor: Colors.white,
                            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                          ),
                          child: const Text('Continuer', style: TextStyle(fontWeight: FontWeight.w600)),
                        ),
                      ),
                      if (_hasExistingPin && _step == _PinStep.enterCurrent) ...[
                        const SizedBox(height: 12),
                        TextButton(
                          onPressed: _disablePin,
                          child: Text('Désactiver le code PIN', style: TextStyle(color: colors.error)),
                        ),
                      ],
                    ],
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader(AppColorScheme colors) {
    return Container(
      color: colors.surface,
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
      child: Row(
        children: [
          IconButton(
            icon: Icon(Icons.arrow_back_rounded, color: colors.textDark),
            onPressed: () => Navigator.of(context).pop(),
          ),
          Expanded(child: Text('Code PIN', textAlign: TextAlign.center, style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark))),
          const SizedBox(width: 48),
        ],
      ),
    );
  }
}
