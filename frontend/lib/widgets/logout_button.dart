import 'package:flutter/material.dart';

import '../theme/app_color_scheme.dart';

/// Centered "Déconnexion" pill button: red icon and text on a light red
/// background.
class LogoutButton extends StatelessWidget {
  final VoidCallback onPressed;

  const LogoutButton({super.key, required this.onPressed});

  @override
  Widget build(BuildContext context) {
    final error = context.colors.error;

    return SizedBox(
      width: double.infinity,
      child: ElevatedButton.icon(
        onPressed: onPressed,
        style: ElevatedButton.styleFrom(
          backgroundColor: error.withValues(alpha: 0.1),
          foregroundColor: error,
          elevation: 0,
          padding: const EdgeInsets.symmetric(vertical: 14),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(30)),
        ),
        icon: Icon(Icons.logout_rounded, color: error),
        label: const Text('Déconnexion', style: TextStyle(fontWeight: FontWeight.w600)),
      ),
    );
  }
}
