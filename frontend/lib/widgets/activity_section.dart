import 'package:flutter/material.dart';

import '../models/user_profile_model.dart';
import 'account_section_card.dart';
import 'profile_menu_row.dart';

/// "Mes Activités" card: orders (with an in-progress count badge),
/// favorites and reviews.
class ActivitySection extends StatelessWidget {
  final UserProfileModel profile;
  final void Function(String label) onItemTap;

  const ActivitySection({super.key, required this.profile, required this.onItemTap});

  @override
  Widget build(BuildContext context) {
    return AccountSectionCard(
      title: 'Mes Activités',
      children: [
        ProfileMenuRow(
          icon: Icons.inventory_2_outlined,
          title: 'Mes Commandes',
          badgeLabel: profile.ongoingOrdersCount > 0 ? '${profile.ongoingOrdersCount} en cours' : null,
          onTap: () => onItemTap('Mes Commandes'),
        ),
        ProfileMenuRow(
          icon: Icons.favorite_border_rounded,
          title: 'Mes Favoris',
          onTap: () => onItemTap('Mes Favoris'),
        ),
        ProfileMenuRow(
          icon: Icons.rate_review_outlined,
          title: 'Mes Avis',
          onTap: () => onItemTap('Mes Avis'),
        ),
      ],
    );
  }
}
