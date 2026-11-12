import 'package:flutter/material.dart';

import '../theme/app_color_scheme.dart';

/// Éléments communs des écrans du service client, au style des autres écrans de l'application : en-tête avec flèche
/// de retour et titre centré, champs arrondis remplis, bouton principal arrondi.

/// En-tête identique à celui du Centre d'aide, des avis, des commandes... ([trailing] facultatif à droite).
class SupportHeader extends StatelessWidget {
  final String title;
  final Widget? trailing;

  const SupportHeader({super.key, required this.title, this.trailing});

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    return Container(
      color: colors.surface,
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
      child: Row(
        children: [
          IconButton(
            icon: Icon(Icons.arrow_back_rounded, color: colors.textDark),
            onPressed: () => Navigator.of(context).maybePop(),
          ),
          Expanded(
            child: Text(
              title,
              textAlign: TextAlign.center,
              style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark),
            ),
          ),
          SizedBox(width: 48, child: trailing),
        ],
      ),
    );
  }
}

/// Champ de saisie arrondi et rempli, comme sur l'écran de connexion.
InputDecoration supportInputDecoration(BuildContext context, String label, {String? hint}) {
  final colors = context.colors;
  final border = OutlineInputBorder(
    borderRadius: BorderRadius.circular(14),
    borderSide: BorderSide(color: colors.border),
  );
  return InputDecoration(
    labelText: label,
    hintText: hint,
    labelStyle: TextStyle(color: colors.textMuted, fontSize: 14),
    hintStyle: TextStyle(color: colors.textMuted, fontSize: 14),
    filled: true,
    fillColor: colors.inputFill,
    alignLabelWithHint: true,
    contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
    border: border,
    enabledBorder: border,
    focusedBorder: border.copyWith(borderSide: BorderSide(color: colors.primary, width: 1.6)),
    errorBorder: border.copyWith(borderSide: BorderSide(color: colors.error)),
    focusedErrorBorder: border.copyWith(borderSide: BorderSide(color: colors.error, width: 1.6)),
  );
}

/// Bouton principal plein largeur, même forme et mêmes couleurs que les autres boutons de l'application.
class SupportPrimaryButton extends StatelessWidget {
  final String label;
  final VoidCallback? onPressed;
  final bool busy;

  const SupportPrimaryButton({super.key, required this.label, required this.onPressed, this.busy = false});

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    return SizedBox(
      width: double.infinity,
      child: ElevatedButton(
        onPressed: busy ? null : onPressed,
        style: ElevatedButton.styleFrom(
          backgroundColor: colors.primary,
          foregroundColor: Colors.white,
          disabledBackgroundColor: colors.primary.withValues(alpha: 0.5),
          disabledForegroundColor: Colors.white,
          elevation: 0,
          padding: const EdgeInsets.symmetric(vertical: 14),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
        ),
        child: busy
            ? const SizedBox(height: 18, width: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
            : Text(label, style: const TextStyle(fontWeight: FontWeight.w600)),
      ),
    );
  }
}

/// Pastille de statut d'une demande.
class SupportStatusChip extends StatelessWidget {
  final String label;
  final bool done;

  const SupportStatusChip({super.key, required this.label, this.done = false});

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    final color = done ? colors.textMuted : colors.primary;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(color: color.withValues(alpha: 0.14), borderRadius: BorderRadius.circular(20)),
      child: Text(label, style: TextStyle(fontSize: 11, fontWeight: FontWeight.w600, color: color)),
    );
  }
}
