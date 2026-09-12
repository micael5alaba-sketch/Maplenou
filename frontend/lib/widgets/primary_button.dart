import 'package:flutter/material.dart';
import '../theme/app_color_scheme.dart';

/// Full-width rounded call-to-action button.
///
/// Defaults to the brand green, but accepts a custom [backgroundColor]
/// (e.g. sage green for "Suivant", orange for "Acheter maintenant").
/// Passing `onPressed: null` renders it disabled with a muted grey style.
/// Set [isLoading] to show a spinner instead of the label and block
/// further taps while an async action (e.g. a form submission) is running.
class PrimaryButton extends StatelessWidget {
  final String label;
  final VoidCallback? onPressed;
  final Color? backgroundColor;
  final bool isLoading;

  const PrimaryButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.backgroundColor,
    this.isLoading = false,
  });

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: double.infinity,
      height: 54,
      child: ElevatedButton(
        onPressed: isLoading ? null : onPressed,
        style: ElevatedButton.styleFrom(
          backgroundColor: backgroundColor ?? context.colors.primary,
          foregroundColor: Colors.white,
          disabledBackgroundColor: isLoading
              ? (backgroundColor ?? context.colors.primary)
              : Colors.grey.shade300,
          disabledForegroundColor: isLoading ? Colors.white : Colors.grey.shade600,
          elevation: 0,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(30)),
        ),
        child: isLoading
            ? const SizedBox(
                width: 22,
                height: 22,
                child: CircularProgressIndicator(strokeWidth: 2.4, color: Colors.white),
              )
            : Text(
                label,
                style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
              ),
      ),
    );
  }
}
