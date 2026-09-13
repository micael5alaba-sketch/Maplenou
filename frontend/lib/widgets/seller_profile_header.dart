import 'package:flutter/material.dart';

import '../models/seller_profile_model.dart';
import '../theme/app_color_scheme.dart';

/// Shop identity card at the top of [SellerProfileScreen]: avatar, "Vendeur
/// certifié" badge, shop name, location, member-since year, and a shortcut
/// to the full sales history.
class SellerProfileHeader extends StatelessWidget {
  final SellerProfileModel profile;
  final VoidCallback onViewSalesHistory;

  const SellerProfileHeader({
    super.key,
    required this.profile,
    required this.onViewSalesHistory,
  });

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 12, offset: const Offset(0, 4)),
        ],
      ),
      child: Column(
        children: [
          CircleAvatar(
            radius: 40,
            backgroundColor: colors.inputFill,
            backgroundImage: profile.logoUrl != null ? NetworkImage(profile.logoUrl!) : null,
            child: profile.logoUrl == null
                ? Icon(Icons.storefront_rounded, size: 36, color: colors.primary)
                : null,
          ),
          const SizedBox(height: 14),
          if (profile.isCertified) ...[
            _buildCertifiedBadge(colors),
            const SizedBox(height: 10),
          ],
          Text(
            profile.shopName,
            style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: colors.textDark),
          ),
          const SizedBox(height: 4),
          Text(
            '${profile.location} • Membre depuis ${profile.memberSinceYear}',
            style: TextStyle(fontSize: 13, color: colors.textMuted),
          ),
          const SizedBox(height: 16),
          SizedBox(
            width: double.infinity,
            child: OutlinedButton.icon(
              onPressed: onViewSalesHistory,
              style: OutlinedButton.styleFrom(
                foregroundColor: colors.primary,
                side: BorderSide(color: colors.primary, width: 1.3),
                padding: const EdgeInsets.symmetric(vertical: 12),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
              ),
              icon: const Icon(Icons.receipt_long_outlined),
              label: const Text('Voir mon historique des ventes', style: TextStyle(fontWeight: FontWeight.w600)),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildCertifiedBadge(AppColorScheme colors) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
      decoration: BoxDecoration(
        color: colors.primary.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.verified_rounded, size: 16, color: colors.primary),
          const SizedBox(width: 6),
          Text(
            'Vendeur certifié',
            style: TextStyle(color: colors.primary, fontWeight: FontWeight.w600, fontSize: 12),
          ),
        ],
      ),
    );
  }
}
