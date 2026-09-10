import 'package:flutter/material.dart';

import '../models/role_model.dart';
import '../theme/app_colors.dart';

/// Provides the list of profiles a user can choose from.
///
/// The data is mocked (no API call) so the selection screen can be built
/// and tested before a real roles endpoint exists.
class RoleService {
  static const List<RoleModel> roles = [
    RoleModel(
      id: 'buyer',
      title: 'Acheteur',
      description:
          'Découvrez des produits artisanaux, achetez en toute sécurité et suivez vos commandes.',
      imagePath: 'assets/images/role_buyer.jpg',
      icon: Icons.shopping_bag_rounded,
      accentColor: AppColors.primary,
    ),
    RoleModel(
      id: 'seller',
      title: 'Vendeur',
      description:
          'Créez votre boutique, gérez votre catalogue et atteignez de nouveaux clients.',
      imagePath: 'assets/images/role_seller.jpg',
      icon: Icons.storefront_rounded,
      accentColor: AppColors.accentOrange,
    ),
    RoleModel(
      id: 'courier',
      title: 'Livreur',
      description:
          'Rejoignez notre réseau logistique, gérez vos courses et gagnez des revenus.',
      imagePath: 'assets/images/role_courier.jpg',
      icon: Icons.delivery_dining_rounded,
      accentColor: AppColors.sage,
    ),
  ];

  List<RoleModel> getRoles() => roles;
}
