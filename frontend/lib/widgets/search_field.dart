import 'package:flutter/material.dart';

import '../theme/app_color_scheme.dart';

/// Rounded, shadowed search input used under the header on screens with a
/// product search (home, categories/results...).
class SearchField extends StatelessWidget {
  final String hintText;

  /// Optional: pass one to actually read/react to what's typed (see
  /// [VendorCatalogScreen], [SalesHistoryScreen], [CategoriesScreen]).
  /// Purely decorative when omitted.
  final TextEditingController? controller;

  /// Called on every keystroke — e.g. to debounce a catalog search request.
  final ValueChanged<String>? onChanged;

  /// Called when the user submits (return key) — e.g. to navigate to a
  /// results screen pre-filled with the typed query (see [HomeScreen]).
  final ValueChanged<String>? onSubmitted;

  const SearchField({super.key, required this.hintText, this.controller, this.onChanged, this.onSubmitted});

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return Container(
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.05),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: TextField(
        controller: controller,
        onChanged: onChanged,
        onSubmitted: onSubmitted,
        textInputAction: TextInputAction.search,
        decoration: InputDecoration(
          hintText: hintText,
          hintStyle: TextStyle(color: colors.textMuted, fontSize: 14),
          prefixIcon: Icon(Icons.search_rounded, color: colors.textMuted),
          border: InputBorder.none,
          contentPadding: const EdgeInsets.symmetric(vertical: 14),
        ),
      ),
    );
  }
}
