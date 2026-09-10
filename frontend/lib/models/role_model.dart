import 'package:flutter/material.dart';

/// Describes one selectable profile/role shown on [ProfileSelectionScreen]
/// (Acheteur, Vendeur, Livreur).
class RoleModel {
  final String id;
  final String title;
  final String description;
  final String imagePath;

  /// Icon shown in the card's color badge (e.g. a shopping bag for
  /// "Acheteur"), and [accentColor] the badge's background color — each
  /// role gets a distinct brand color for quick visual recognition.
  final IconData icon;
  final Color accentColor;

  const RoleModel({
    required this.id,
    required this.title,
    required this.description,
    required this.imagePath,
    required this.icon,
    required this.accentColor,
  });
}
