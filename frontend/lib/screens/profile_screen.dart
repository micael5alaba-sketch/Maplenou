import 'package:flutter/material.dart';

import '../services/profile_service.dart';
import '../services/theme_controller.dart';
import '../theme/app_color_scheme.dart';
import '../widgets/account_section_card.dart';
import '../widgets/activity_section.dart';
import '../widgets/app_drawer.dart';
import '../widgets/app_logo.dart';
import '../widgets/custom_bottom_navigation.dart';
import '../widgets/logout_button.dart';
import '../widgets/profile_header.dart';
import '../widgets/profile_menu_row.dart';
import '../widgets/support_section.dart';
import 'addresses_screen.dart';
import 'categories_screen.dart';
import 'favorites_screen.dart';
import 'my_orders_screen.dart';
import 'my_reviews_screen.dart';

/// "Profil" tab: user identity, quick activity shortcuts, account settings
/// and support links, plus the fixed bottom navigation.
///
/// Backed by [ProfileService] mocked data for now — no network call yet.
/// Swapping it for a real `GET /api/users/me` later only touches that
/// service, not this screen or its widgets.
class ProfileScreen extends StatefulWidget {
  const ProfileScreen({super.key});

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  final _scaffoldKey = GlobalKey<ScaffoldState>();
  final _profile = ProfileService().getCurrentProfile();

  void _showComingSoon(String feature) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text('$feature bientôt disponible.')));
  }

  void _handleActivityTap(String label) {
    if (label == 'Mes Commandes') {
      Navigator.of(context).push(MaterialPageRoute(builder: (_) => const MyOrdersScreen()));
      return;
    }
    if (label == 'Mes Favoris') {
      Navigator.of(context).push(MaterialPageRoute(builder: (_) => const FavoritesScreen()));
      return;
    }
    if (label == 'Mes Avis') {
      Navigator.of(context).push(MaterialPageRoute(builder: (_) => const MyReviewsScreen()));
      return;
    }
    _showComingSoon(label);
  }

  void _onTabSelected(HomeTab tab) {
    if (tab == HomeTab.profile) return;
    switch (tab) {
      case HomeTab.home:
        Navigator.of(context).popUntil((route) => route.isFirst);
        break;
      case HomeTab.categories:
        Navigator.of(context).push(MaterialPageRoute(builder: (_) => const CategoriesScreen()));
        break;
      case HomeTab.cart:
        _showComingSoon('Le panier');
        break;
      case HomeTab.profile:
        break;
    }
  }

  String _themeModeLabel(ThemeMode mode) {
    switch (mode) {
      case ThemeMode.light:
        return 'Clair';
      case ThemeMode.dark:
        return 'Sombre';
      case ThemeMode.system:
        return 'Système';
    }
  }

  void _showThemePicker() {
    showModalBottomSheet<void>(
      context: context,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (sheetContext) {
        return ValueListenableBuilder<ThemeMode>(
          valueListenable: themeController,
          builder: (context, currentMode, _) {
            return SafeArea(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Padding(
                    padding: const EdgeInsets.all(16),
                    child: Text(
                      'Thème',
                      style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: context.colors.textDark),
                    ),
                  ),
                  RadioGroup<ThemeMode>(
                    groupValue: currentMode,
                    onChanged: (value) {
                      if (value != null) themeController.setThemeMode(value);
                      Navigator.of(sheetContext).pop();
                    },
                    child: Column(
                      children: [
                        for (final mode in ThemeMode.values)
                          RadioListTile<ThemeMode>(
                            value: mode,
                            activeColor: context.colors.primary,
                            title: Text(_themeModeLabel(mode)),
                          ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 8),
                ],
              ),
            );
          },
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      key: _scaffoldKey,
      backgroundColor: context.colors.homeBackground,
      drawer: AppDrawer(onComingSoon: _showComingSoon),
      body: SafeArea(
        child: Column(
          children: [
            _buildHeader(),
            Expanded(
              child: SingleChildScrollView(
                padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
                child: Column(
                  children: [
                    ProfileHeader(profile: _profile),
                    const SizedBox(height: 24),
                    ActivitySection(profile: _profile, onItemTap: _handleActivityTap),
                    const SizedBox(height: 16),
                    _buildAccountSettingsCard(),
                    const SizedBox(height: 16),
                    SupportSection(onItemTap: _showComingSoon),
                    const SizedBox(height: 28),
                    LogoutButton(onPressed: () => _showComingSoon('La déconnexion')),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
      bottomNavigationBar: CustomBottomNavigation(
        currentTab: HomeTab.profile,
        onTabSelected: _onTabSelected,
      ),
    );
  }

  Widget _buildHeader() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      child: Row(
        children: [
          IconButton(
            icon: Icon(Icons.menu_rounded, color: context.colors.textDark),
            onPressed: () => _scaffoldKey.currentState?.openDrawer(),
          ),
          const Expanded(
            child: Center(child: AppLogo(variant: AppLogoVariant.horizontalColor, size: 34)),
          ),
          IconButton(
            icon: Icon(Icons.search_rounded, color: context.colors.textDark),
            onPressed: () => _showComingSoon('La recherche'),
          ),
        ],
      ),
    );
  }

  Widget _buildAccountSettingsCard() {
    return AccountSectionCard(
      title: 'Paramètres du compte',
      children: [
        ProfileMenuRow(
          icon: Icons.person_outline_rounded,
          title: 'Informations personnelles',
          subtitle: 'Modifier profil, mot de passe',
          onTap: () => _showComingSoon('Informations personnelles'),
        ),
        ProfileMenuRow(
          icon: Icons.location_on_outlined,
          title: 'Adresses enregistrées',
          subtitle: _profile.primaryAddressSummary,
          onTap: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const AddressesScreen())),
        ),
        ProfileMenuRow(
          icon: Icons.credit_card_outlined,
          title: 'Modes de paiement',
          subtitle: _profile.paymentMethodsSummary,
          onTap: () => _showComingSoon('Modes de paiement'),
        ),
        ValueListenableBuilder<ThemeMode>(
          valueListenable: themeController,
          builder: (context, mode, _) {
            return ProfileMenuRow(
              icon: Icons.dark_mode_outlined,
              title: 'Thème',
              subtitle: _themeModeLabel(mode),
              onTap: _showThemePicker,
            );
          },
        ),
        ProfileMenuRow(
          icon: Icons.language_rounded,
          title: 'Langue',
          subtitle: 'Français, English',
          onTap: () => _showComingSoon('Langue'),
        ),
      ],
    );
  }
}
