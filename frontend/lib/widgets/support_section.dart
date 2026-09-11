import 'package:flutter/material.dart';

import 'account_section_card.dart';
import 'profile_menu_row.dart';

/// "Aide & Support" card: help center, contact us, about.
class SupportSection extends StatelessWidget {
  final void Function(String label) onItemTap;

  const SupportSection({super.key, required this.onItemTap});

  @override
  Widget build(BuildContext context) {
    return AccountSectionCard(
      title: 'Aide & Support',
      children: [
        ProfileMenuRow(
          icon: Icons.help_outline_rounded,
          title: "Centre d'aide",
          onTap: () => onItemTap("Centre d'aide"),
        ),
        ProfileMenuRow(
          icon: Icons.chat_bubble_outline_rounded,
          title: 'Nous contacter',
          onTap: () => onItemTap('Nous contacter'),
        ),
        ProfileMenuRow(
          icon: Icons.info_outline_rounded,
          title: 'À propos de Maplenou',
          onTap: () => onItemTap('À propos de Maplenou'),
        ),
      ],
    );
  }
}
