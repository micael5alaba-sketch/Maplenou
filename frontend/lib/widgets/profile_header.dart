import 'package:flutter/material.dart';

import '../models/user_profile_model.dart';
import '../theme/app_color_scheme.dart';

/// Centered avatar, full name, email and a "Compte vérifié" badge, shown
/// at the top of [ProfileScreen].
class ProfileHeader extends StatelessWidget {
  final UserProfileModel profile;

  const ProfileHeader({super.key, required this.profile});

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return Column(
      children: [
        CircleAvatar(
          radius: 44,
          backgroundColor: colors.inputFill,
          backgroundImage: profile.avatarUrl != null ? NetworkImage(profile.avatarUrl!) : null,
          child: profile.avatarUrl == null
              ? Text(
                  profile.fullName.isNotEmpty ? profile.fullName[0].toUpperCase() : '?',
                  style: TextStyle(fontSize: 32, fontWeight: FontWeight.bold, color: colors.primary),
                )
              : null,
        ),
        const SizedBox(height: 12),
        Text(
          profile.fullName,
          style: TextStyle(fontSize: 19, fontWeight: FontWeight.bold, color: colors.textDark),
        ),
        const SizedBox(height: 4),
        Text(profile.email, style: TextStyle(color: colors.textMuted)),
        if (profile.isVerified) ...[
          const SizedBox(height: 10),
          _buildVerifiedBadge(context),
        ],
      ],
    );
  }

  Widget _buildVerifiedBadge(BuildContext context) {
    final colors = context.colors;

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
            'Compte vérifié',
            style: TextStyle(color: colors.primary, fontWeight: FontWeight.w600, fontSize: 12),
          ),
        ],
      ),
    );
  }
}
