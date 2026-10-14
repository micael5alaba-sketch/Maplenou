import 'package:flutter/material.dart';

import '../models/user_model.dart';
import '../screens/courier_dashboard_screen.dart';
import '../screens/courier_pin_lock_screen.dart';
import '../screens/create_shop_screen.dart';
import '../screens/home_screen.dart';
import '../screens/profile_selection_screen.dart';
import '../screens/seller_dashboard_screen.dart';
import 'courier_pin_service.dart';
import 'session_service.dart';
import 'shop_service.dart';

/// Décide quel écran ouvrir pour un [UserModel] donné, une fois authentifié
/// (après login/register, ou au démarrage si une session valide existe).
///
/// Le compte est unique (voir `CLAUDE.md` §3) : la même personne peut être
/// Acheteur et Vendeur, jamais Livreur en plus (compte `DELIVERY_AGENT`
/// dédié, soit auto-inscrit comme tel — cahier des charges §3.3 — soit créé
/// directement par l'admin). Un livreur n'a donc qu'une seule interface
/// possible ; un acheteur devenu vendeur approuvé peut basculer entre les
/// deux, et son dernier choix est mémorisé par [SessionService].
class AppRouter {
  static Future<Widget> resolveHomeScreen(UserModel user) async {
    if (user.isDeliveryAgent) {
      final hasPin = await CourierPinService().isPinSet();
      return hasPin ? const CourierPinLockScreen(child: CourierDashboardScreen()) : const CourierDashboardScreen();
    }

    if (!user.isApprovedSeller) {
      return const HomeScreen();
    }

    final preferredMode = await SessionService().getSelectedRoleId();
    if (preferredMode == null) {
      // Premier login avec double capacité : laisse la personne choisir,
      // au lieu de deviner à sa place.
      return ProfileSelectionScreen(user: user);
    }
    return preferredMode == 'seller' ? await resolveSellerEntryScreen() : const HomeScreen();
  }

  /// `SellerProfile` (approuvé) et `Shop` sont deux entités distinctes côté
  /// backend (`CLAUDE.md` §3) : un vendeur approuvé peut ne pas avoir
  /// encore créé sa boutique — dans ce cas [CreateShopScreen] passe avant
  /// [SellerDashboardScreen].
  static Future<Widget> resolveSellerEntryScreen() async {
    final shop = await ShopService().getMine();
    return shop == null ? const CreateShopScreen() : const SellerDashboardScreen();
  }
}
