import 'package:flutter/material.dart';

import '../models/role_model.dart';
import '../theme/app_color_scheme.dart';

/// Immersive, tappable card representing one selectable [RoleModel]:
/// background photo tinted with the role's own accent color, a colored
/// icon badge, and the title/description at the bottom-left.
///
/// Animates its border, shadow and selection badge when [isSelected]
/// changes so the selection state feels smooth rather than an abrupt
/// style swap.
class RoleCard extends StatelessWidget {
  final RoleModel role;
  final bool isSelected;
  final VoidCallback onTap;

  const RoleCard({
    super.key,
    required this.role,
    required this.isSelected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 250),
        curve: Curves.easeOut,
        height: 176,
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(22),
          border: Border.all(
            color: isSelected ? context.colors.primary : Colors.transparent,
            width: 3,
          ),
          boxShadow: [
            BoxShadow(
              color: isSelected
                  ? context.colors.primary.withValues(alpha: 0.28)
                  : Colors.black.withValues(alpha: 0.08),
              blurRadius: isSelected ? 18 : 8,
              offset: const Offset(0, 4),
            ),
          ],
        ),
        child: ClipRRect(
          borderRadius: BorderRadius.circular(19),
          child: Stack(
            fit: StackFit.expand,
            children: [
              Image.asset(role.imagePath, fit: BoxFit.cover),
              // Tinted with the role's own accent color so each card reads
              // as distinct, fading to dark for text readability.
              DecoratedBox(
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.topCenter,
                    end: Alignment.bottomCenter,
                    colors: [
                      role.accentColor.withValues(alpha: 0.4),
                      Colors.black.withValues(alpha: 0.6),
                    ],
                  ),
                ),
              ),
              Positioned(top: 14, left: 14, child: _IconBadge(role: role)),
              if (isSelected) const Positioned(top: 14, right: 14, child: _SelectedBadge()),
              Positioned(
                left: 18,
                right: 18,
                bottom: 16,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      role.title,
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 20,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 6),
                    Text(
                      role.description,
                      style: const TextStyle(
                        color: Colors.white70,
                        fontSize: 13,
                        height: 1.3,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Small circular badge showing the role's icon on its own accent color.
class _IconBadge extends StatelessWidget {
  final RoleModel role;

  const _IconBadge({required this.role});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 38,
      height: 38,
      decoration: BoxDecoration(
        color: role.accentColor,
        shape: BoxShape.circle,
        border: Border.all(color: Colors.white.withValues(alpha: 0.5), width: 1.5),
      ),
      child: Icon(role.icon, color: Colors.white, size: 20),
    );
  }
}

/// Green check badge shown in the top-right corner once a card is selected.
class _SelectedBadge extends StatelessWidget {
  const _SelectedBadge();

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 28,
      height: 28,
      decoration: BoxDecoration(color: context.colors.primary, shape: BoxShape.circle),
      child: const Icon(Icons.check_rounded, color: Colors.white, size: 18),
    );
  }
}
