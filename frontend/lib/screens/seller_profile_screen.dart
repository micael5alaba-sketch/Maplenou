import 'package:flutter/material.dart';

import '../models/seller_profile_model.dart';
import '../services/api_client.dart';
import '../services/auth_service.dart';
import '../services/conversation_service.dart';
import '../services/seller_profile_service.dart';
import '../services/session_service.dart';
import '../services/shop_service.dart';
import '../theme/app_color_scheme.dart';
import '../widgets/account_section_card.dart';
import '../widgets/app_drawer.dart';
import '../widgets/app_logo.dart';
import '../widgets/logout_button.dart';
import '../widgets/profile_menu_row.dart';
import '../widgets/seller_bottom_navigation.dart';
import '../widgets/seller_profile_header.dart';
import 'about_screen.dart';
import 'help_center_screen.dart';
import 'login_screen.dart';
import 'orders_management_screen.dart';
import 'sales_history_screen.dart';
import 'seller_conversation_screen.dart';
import 'seller_dashboard_screen.dart';
import 'seller_messages_screen.dart';
import 'vendor_catalog_screen.dart';
import 'wallet_screen.dart';
import 'home_screen.dart';

/// "Profil" tab of the seller space: shop identity, wallet shortcut,
/// editable shop settings, help and logout.
///
/// Backed by [SellerProfileService] (`GET /api/shops/mine`), sauvegarde via
/// `PATCH /api/shops/mine` (nom + numéro fiscal — l'email de contact n'a
/// pas d'équivalent sur `Shop`, affiché en lecture seule).
class SellerProfileScreen extends StatefulWidget {
  const SellerProfileScreen({super.key});

  @override
  State<SellerProfileScreen> createState() => _SellerProfileScreenState();
}

class _SellerProfileScreenState extends State<SellerProfileScreen> {
  final _scaffoldKey = GlobalKey<ScaffoldState>();
  final _shopService = ShopService();
  late Future<SellerProfileModel> _profileFuture;

  final _shopNameController = TextEditingController();
  final _taxIdController = TextEditingController();
  String _contactEmail = '';

  bool _settingsExpanded = true;
  bool _helpExpanded = false;
  bool _isSaving = false;

  @override
  void initState() {
    super.initState();
    _profileFuture = _loadProfile();
  }

  Future<SellerProfileModel> _loadProfile() async {
    final profile = await SellerProfileService().getProfile();
    _shopNameController.text = profile.shopName;
    _taxIdController.text = profile.taxId ?? '';
    _contactEmail = profile.contactEmail;
    return profile;
  }

  @override
  void dispose() {
    _shopNameController.dispose();
    _taxIdController.dispose();
    super.dispose();
  }

  void _showComingSoon(String feature) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text('$feature bientôt disponible.')));
  }

  Future<void> _contactSupport() async {
    try {
      final conversationId = await ConversationService().startSupportConversation();
      if (!mounted) return;
      await Navigator.of(context).push(MaterialPageRoute(
        builder: (_) => SellerConversationScreen(
          conversationId: conversationId,
          participantName: 'Support Maplenou',
        ),
      ));
    } on ApiException catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(SnackBar(content: Text(e.message)));
    }
  }

  void _showMessage(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(message)));
  }

  Future<void> _handleSave() async {
    setState(() => _isSaving = true);
    try {
      await _shopService.updateMine(
        name: _shopNameController.text.trim(),
        taxId: _taxIdController.text.trim(),
      );
      _showMessage('Modifications enregistrées.');
    } on ApiException catch (e) {
      _showMessage(e.message);
    } finally {
      if (mounted) setState(() => _isSaving = false);
    }
  }

  /// L'écran de sélection de profil ne s'affiche qu'une seule fois, à la toute
  /// première connexion (voir `AppRouter`/`ProfileSelectionScreen`) — ce bouton
  /// est le seul moyen de rebasculer ensuite, malgré ce qu'annonce ce premier écran.
  Future<void> _switchToBuyer() async {
    await SessionService().saveSelectedRoleId('buyer');
    if (!mounted) return;
    Navigator.of(context).pushAndRemoveUntil(
      MaterialPageRoute(builder: (_) => const HomeScreen()),
      (route) => false,
    );
  }

  Future<void> _handleLogout() async {
    await AuthService().logout();
    if (!mounted) return;
    Navigator.of(context).pushAndRemoveUntil(
      MaterialPageRoute(builder: (_) => const LoginScreen()),
      (route) => false,
    );
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
              child: FutureBuilder<SellerProfileModel>(
                future: _profileFuture,
                builder: (context, snapshot) {
                  if (snapshot.connectionState == ConnectionState.waiting) {
                    return const Center(child: CircularProgressIndicator());
                  }
                  if (snapshot.hasError || !snapshot.hasData) {
                    return _buildErrorState(colors, snapshot.error);
                  }
                  return _buildContent(colors, snapshot.data!);
                },
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

  Widget _buildErrorState(AppColorScheme colors, Object? error) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.error_outline_rounded, size: 48, color: colors.error),
            const SizedBox(height: 12),
            Text(
              error is ApiException ? error.message : 'Impossible de charger le profil vendeur.',
              textAlign: TextAlign.center,
              style: TextStyle(color: colors.textMuted),
            ),
            const SizedBox(height: 12),
            TextButton(
              onPressed: () => setState(() => _profileFuture = _loadProfile()),
              child: const Text('Réessayer'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildContent(AppColorScheme colors, SellerProfileModel profile) {
    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
      children: [
        SellerProfileHeader(
          profile: profile,
          onViewSalesHistory: () =>
              Navigator.of(context).push(MaterialPageRoute(builder: (_) => const SalesHistoryScreen())),
        ),
        const SizedBox(height: 16),
        AccountSectionCard(
          title: 'Boutique',
          children: [
            ProfileMenuRow(
              icon: Icons.account_balance_wallet_outlined,
              title: 'Gestion du Portefeuille & Solde',
              onTap: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const WalletScreen())),
            ),
            ProfileMenuRow(
              icon: Icons.chat_bubble_outline_rounded,
              title: 'Messagerie',
              subtitle: 'Discuter avec vos clients',
              onTap: () =>
                  Navigator.of(context).push(MaterialPageRoute(builder: (_) => const SellerMessagesScreen())),
            ),
            ProfileMenuRow(
              icon: Icons.swap_horiz_rounded,
              title: 'Passer en mode Acheteur',
              subtitle: 'Votre compte reste vendeur, juste un autre affichage',
              onTap: _switchToBuyer,
            ),
          ],
        ),
        const SizedBox(height: 16),
        _buildSettingsSection(colors),
        const SizedBox(height: 16),
        _buildHelpSection(colors),
        const SizedBox(height: 28),
        LogoutButton(onPressed: _handleLogout),
      ],
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
            _buildTextField(colors, TextEditingController(text: _contactEmail), enabled: false),
            Padding(
              padding: const EdgeInsets.only(top: 4),
              child: Text(
                "Lié à votre compte, modifiable depuis Informations personnelles.",
                style: TextStyle(fontSize: 11, color: colors.textMuted),
              ),
            ),
            const SizedBox(height: 16),
            _buildLabel(colors, "Numéro d'Identification Fiscale (NINEA/TVA)"),
            const SizedBox(height: 8),
            _buildTextField(colors, _taxIdController, hint: 'Entrez votre numéro fiscal'),
            const SizedBox(height: 16),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: _isSaving ? null : _handleSave,
                style: ElevatedButton.styleFrom(
                  backgroundColor: colors.primary,
                  foregroundColor: Colors.white,
                  elevation: 0,
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                ),
                child: _isSaving
                    ? const SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                      )
                    : const Text('Enregistrer les modifications', style: TextStyle(fontWeight: FontWeight.w600)),
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
              onTap: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const HelpCenterScreen())),
            ),
            ProfileMenuRow(
              icon: Icons.chat_bubble_outline_rounded,
              title: 'Nous contacter',
              onTap: _contactSupport,
            ),
            ProfileMenuRow(
              icon: Icons.info_outline_rounded,
              title: 'À propos de Maplenou',
              onTap: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const AboutScreen())),
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
    bool enabled = true,
  }) {
    final border = OutlineInputBorder(
      borderRadius: BorderRadius.circular(14),
      borderSide: BorderSide(color: colors.border),
    );

    return TextField(
      controller: controller,
      keyboardType: keyboardType,
      enabled: enabled,
      decoration: InputDecoration(
        hintText: hint,
        hintStyle: TextStyle(color: colors.textMuted, fontSize: 14),
        filled: true,
        fillColor: enabled ? colors.inputFill : colors.inputFill.withValues(alpha: 0.5),
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        border: border,
        enabledBorder: border,
        focusedBorder: border.copyWith(borderSide: BorderSide(color: colors.primary, width: 1.6)),
      ),
    );
  }
}
