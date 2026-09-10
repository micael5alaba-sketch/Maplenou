import 'package:flutter/material.dart';

import '../theme/app_color_scheme.dart';

/// White (or dark-surface) rounded card with a light shadow, a title, and
/// a list of rows (typically [ProfileMenuRow]) separated by thin dividers.
/// Shared by every settings-style section on [ProfileScreen].
class AccountSectionCard extends StatelessWidget {
  final String title;
  final List<Widget> children;

  const AccountSectionCard({super.key, required this.title, required this.children});

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(18),
        boxShadow: [
          BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 12, offset: const Offset(0, 4)),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: colors.textDark)),
          for (var i = 0; i < children.length; i++) ...[
            children[i],
            if (i != children.length - 1) Divider(height: 1, color: colors.border),
          ],
        ],
      ),
    );
  }
}
