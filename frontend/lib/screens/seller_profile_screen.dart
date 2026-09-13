import 'package:flutter/material.dart';

import '../services/seller_profile_service.dart';
import '../theme/app_color_scheme.dart';
import '../widgets/account_section_card.dart';
import '../widgets/app_drawer.dart';
import '../widgets/app_logo.dart';
import '../widgets/logout_button.dart';
import '../widgets/profile_menu_row.dart';
import '../widgets/seller_bottom_navigation.dart';
import '../widgets/seller_profile_header.dart';
import 'orders_management_screen.dart';
import 'sales_history_screen.dart';
import 'seller_dashboard_screen.dart';
import 'seller_messages_screen.dart';
import 'vendor_catalog_screen.dart';
import 'wallet_screen.dart';

/// "Profil" tab of the seller space: shop identity, wallet shortcut,
/// editable shop settings, help and logout.
///
/// Backed by [SellerProfileService] mocked data for now — no network call
/// yet. Swapping it for a real `GET/PATCH /api/shops/mine` call later only
/// touches that service and [_handleSave] below.
class SellerProfileScreen extends StatefulWidget {
  const SellerProfileScreen({super.key});

  @override
  State<SellerProfileScreen> createState() => _SellerProfileScreenState();
}

class _SellerProfileScreenState extends State<SellerProfileScreen> {
  final _scaffoldKey = GlobalKey<ScaffoldState>();
  final _profile = SellerProfileService().getProfile();

  late final _shopNameController = TextEditingController(text: _profile.shopName);
  late final _emailController = TextEditingController(text: _profile.contactEmail);
  late final _taxIdController = TextEditingController(text: _profile.taxId ?? '');

  bool _settingsExpanded = true;
  bool _helpExpanded = false;

  @override
  void dispose() {
    _shopNameController.dispose();
    _emailController.dispose();
    _taxIdController.dispose();
    super.dispose();
  }

  void _showComingSoon(String feature) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text('$feature bientôt disponible.')));
  }

  void _handleSave() {
    // Mocké pour l'instant — PATCH /api/shops/mine (avec taxId, désormais
    // supporté côté backend) une fois l'authentification branchée.
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(const SnackBar(content: Text('Modifications enregistrées.')));
  }

  void _onTabSelected(SellerTab tab) {
    if (tab == SellerTab.profile) return;
    switch (tab) {
      case SellerTab.dashboard:
        Navigator.of(context).pushReplacement(MaterialPageRoute(builder: (_) => const SellerDashboardScreen()));
        break;
      case SellerTab.orders:
        Navigator.of(context).pushReplacement(MaterialPageRoute(builder: (_) => const OrdersManagementScreen()));
        break;
      case SellerTab.products:
        Navigator.of(context).pushReplacement(MaterialPageRoute(builder: (_) => const VendorCatalogScreen()));
        break;
      case SellerTab.profile:
        break;
    }
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return Scaffold(
      key: _scaffoldKey,
      backgroundColor: colors.homeBackground,
      drawer: AppDrawer(onComingSoon: _showComingSoon),
      body: SafeArea(
        child: Column(
          children: [
            _buildHeader(colors),
            Expanded(
              child: ListView(
                padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
                children: [
                  SellerProfileHeader(
                    profile: _profile,
                    onViewSalesHistory: () => Navigator.of(context)
                        .push(MaterialPageRoute(builder: (_) => const SalesHistoryScreen())),
                  ),
                  const SizedBox(height: 16),
                  AccountSectionCard(
                    title: 'Boutique',
                    children: [
                      ProfileMenuRow(
                        icon: Icons.account_balance_wallet_outlined,
                        title: 'Gestion du Portefeuille & Solde',
                        onTap: () =>
                            Navigator.of(context).push(MaterialPageRoute(builder: (_) => const WalletScreen())),
                      ),
                      ProfileMenuRow(
                        icon: Icons.chat_bubble_outline_rounded,
                        title: 'Messagerie',
                        subtitle: 'Discuter avec vos clients',
                        onTap: () =>
                            Navigator.of(context).push(MaterialPageRoute(builder: (_) => const SellerMessagesScreen())),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  _buildSettingsSection(colors),
                  const SizedBox(height: 16),
                  _buildHelpSection(colors),
                  const SizedBox(height: 28),
                  LogoutButton(onPressed: () => _showComingSoon('La déconnexion')),
                ],
              ),
            ),
          ],
        ),
      ),
      bottomNavigationBar: SellerBottomNavigation(
        currentTab: SellerTab.profile,
        onTabSelected: _onTabSelected,
      ),
    );
  }

  Widget _buildHeader(AppColorScheme colors) {
    return Container(
      color: colors.surface,
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      child: Row(
        children: [
          IconButton(
            icon: Icon(Icons.menu_rounded, color: colors.textDark),
            onPressed: () => _scaffoldKey.currentState?.openDrawer(),
          ),
          Expanded(
            child: Text(
              'Maplenou Vendor',
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 17, fontWeight: FontWeight.bold, color: colors.textDark),
            ),
          ),
          Padding(
            padding: const EdgeInsets.only(right: 8),
            child: ClipOval(
              child: Container(
                width: 32,
                height: 32,
                color: colors.inputFill,
                padding: const EdgeInsets.all(6),
                child: const AppLogo(variant: AppLogoVariant.iconColor, size: 20),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSettingsSection(AppColorScheme colors) {
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
          InkWell(
            onTap: () => setState(() => _settingsExpanded = !_settingsExpanded),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text('Paramètres & Préférences',
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: colors.textDark)),
                Icon(
                  _settingsExpanded ? Icons.keyboard_arrow_up_rounded : Icons.keyboard_arrow_down_rounded,
                  color: colors.textMuted,
                ),
              ],
            ),
          ),
          if (_settingsExpanded) ...[
            const SizedBox(height: 16),
            _buildLabel(colors, 'Nom de la Boutique'),
            const SizedBox(height: 8),
            _buildTextField(colors, _shopNameController),
            const SizedBox(height: 16),
            _buildLabel(colors, 'Email de Contact'),
            const SizedBox(height: 8),
            _buildTextField(colors, _emailController, keyboardType: TextInputType.emailAddress),
            const SizedBox(height: 16),
            _buildLabel(colors, "Numéro d'Identification Fiscale (NINEA/TVA)"),
            const SizedBox(height: 8),
            _buildTextField(colors, _taxIdController, hint: 'Entrez votre numéro fiscal'),
            const SizedBox(height: 16),
            Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(color: colors.inputFill, borderRadius: BorderRadius.circular(14)),
              child: Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text("Thème de l'application", style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark)),
                        const SizedBox(height: 4),
                        Text(
                          'Actuellement verrouillé sur le mode clair pour une lisibilité optimale.',
                          style: TextStyle(fontSize: 12, color: colors.textMuted, height: 1.3),
                        ),
                      ],
                    ),
                  ),
                  Switch(value: false, onChanged: null),
                ],
              ),
            ),
            const SizedBox(height: 16),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: _handleSave,
                style: ElevatedButton.styleFrom(
                  backgroundColor: colors.primary,
                  foregroundColor: Colors.white,
                  elevation: 0,
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                ),
                child: const Text('Enregistrer les modifications', style: TextStyle(fontWeight: FontWeight.w600)),
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildHelpSection(AppColorScheme colors) {
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
          InkWell(
            onTap: () => setState(() => _helpExpanded = !_helpExpanded),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text('Aide et Assistance',
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: colors.textDark)),
                Icon(
                  _helpExpanded ? Icons.keyboard_arrow_up_rounded : Icons.keyboard_arrow_down_rounded,
                  color: colors.textMuted,
                ),
              ],
            ),
          ),
          if (_helpExpanded) ...[
            Divider(height: 24, color: colors.border),
            ProfileMenuRow(
              icon: Icons.help_outline_rounded,
              title: "Centre d'aide",
              onTap: () => _showComingSoon("Centre d'aide"),
            ),
            ProfileMenuRow(
              icon: Icons.chat_bubble_outline_rounded,
              title: 'Nous contacter',
              onTap: () => _showComingSoon('Nous contacter'),
            ),
            ProfileMenuRow(
              icon: Icons.info_outline_rounded,
              title: 'À propos de Maplenou',
              onTap: () => _showComingSoon('À propos de Maplenou'),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildLabel(AppColorScheme colors, String label) {
    return Text(label, style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600, color: colors.textDark));
  }

  Widget _buildTextField(
    AppColorScheme colors,
    TextEditingController controller, {
    String? hint,
    TextInputType? keyboardType,
  }) {
    final border = OutlineInputBorder(
      borderRadius: BorderRadius.circular(14),
      borderSide: BorderSide(color: colors.border),
    );

    return TextField(
      controller: controller,
      keyboardType: keyboardType,
      decoration: InputDecoration(
        hintText: hint,
        hintStyle: TextStyle(color: colors.textMuted, fontSize: 14),
        filled: true,
        fillColor: colors.inputFill,
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        border: border,
        enabledBorder: border,
        focusedBorder: border.copyWith(borderSide: BorderSide(color: colors.primary, width: 1.6)),
      ),
    );
  }
}
