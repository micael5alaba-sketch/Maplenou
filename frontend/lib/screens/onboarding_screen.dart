import 'package:flutter/material.dart';

import '../services/session_service.dart';
import '../theme/app_color_scheme.dart';
import '../widgets/app_logo.dart';
import 'login_screen.dart';

/// Carrousel d'accueil (3 pages), affiché une seule fois au tout premier
/// lancement sans session active — voir [SessionService.hasSeenOnboarding],
/// appelé depuis [SplashScreen]. "Suivant" fait défiler les deux premières
/// pages, "Commencer" sur la dernière marque l'onboarding comme vu et ouvre
/// [LoginScreen] (qui propose lui-même le lien vers l'inscription).
class OnboardingScreen extends StatefulWidget {
  const OnboardingScreen({super.key});

  @override
  State<OnboardingScreen> createState() => _OnboardingScreenState();
}

class _OnboardingPageData {
  final Widget illustration;
  final String title;
  final String subtitle;

  const _OnboardingPageData({required this.illustration, required this.title, required this.subtitle});
}

class _OnboardingScreenState extends State<OnboardingScreen> {
  final _pageController = PageController();
  int _currentPage = 0;

  late final List<_OnboardingPageData> _pages = [
    _OnboardingPageData(
      illustration: const _ShoppingIllustration(),
      title: 'Des milliers de produits\nà portée de main',
      subtitle: 'Découvrez une large sélection de produits de qualité, vendus par '
          'des vendeurs de confiance. Faites vos achats en toute simplicité.',
    ),
    _OnboardingPageData(
      illustration: const _DeliveryIllustration(),
      title: 'Une livraison rapide\net sécurisée',
      subtitle: 'Suivez votre commande en temps réel et recevez vos produits où que '
          'vous soyez, grâce à nos livreurs et partenaires de confiance.',
    ),
    _OnboardingPageData(
      illustration: const _AppPreviewIllustration(),
      title: 'Une expérience complète',
      subtitle: 'Commandez, payez en toute sécurité, suivez vos livraisons et gérez '
          'tout depuis votre compte, sur web et mobile.',
    ),
  ];

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  Future<void> _handlePrimaryAction() async {
    final isLastPage = _currentPage == _pages.length - 1;
    if (!isLastPage) {
      _pageController.nextPage(duration: const Duration(milliseconds: 320), curve: Curves.easeOut);
      return;
    }
    await SessionService().markOnboardingSeen();
    if (!mounted) return;
    Navigator.of(context).pushReplacement(MaterialPageRoute(builder: (_) => const LoginScreen()));
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    final isLastPage = _currentPage == _pages.length - 1;

    return Scaffold(
      backgroundColor: colors.lightBackground,
      body: SafeArea(
        child: Column(
          children: [
            Expanded(
              child: PageView.builder(
                controller: _pageController,
                itemCount: _pages.length,
                onPageChanged: (index) => setState(() => _currentPage = index),
                itemBuilder: (context, index) => _buildPage(colors, _pages[index]),
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(28, 8, 28, 24),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Row(
                    children: List.generate(
                      _pages.length,
                      (index) => _buildDot(colors, isActive: index == _currentPage),
                    ),
                  ),
                  TextButton(
                    onPressed: _handlePrimaryAction,
                    style: TextButton.styleFrom(foregroundColor: colors.accentOrange),
                    child: Row(
                      children: [
                        Text(
                          isLastPage ? 'Commencer' : 'Suivant',
                          style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15),
                        ),
                        const SizedBox(width: 4),
                        const Icon(Icons.arrow_forward_rounded, size: 18),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDot(AppColorScheme colors, {required bool isActive}) {
    return AnimatedContainer(
      duration: const Duration(milliseconds: 250),
      margin: const EdgeInsets.only(right: 6),
      width: isActive ? 20 : 7,
      height: 7,
      decoration: BoxDecoration(
        color: isActive ? colors.accentOrange : colors.border,
        borderRadius: BorderRadius.circular(4),
      ),
    );
  }

  Widget _buildPage(AppColorScheme colors, _OnboardingPageData page) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 28),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const SizedBox(height: 8),
          Expanded(child: Center(child: page.illustration)),
          const SizedBox(height: 16),
          Text(
            page.title,
            style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: colors.textDark, height: 1.25),
          ),
          const SizedBox(height: 10),
          Text(
            page.subtitle,
            style: TextStyle(fontSize: 14, color: colors.textMuted, height: 1.45),
          ),
          const SizedBox(height: 12),
        ],
      ),
    );
  }
}

/// Page 1 : panier central entouré de badges produits flottants.
class _ShoppingIllustration extends StatelessWidget {
  const _ShoppingIllustration();

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    return SizedBox(
      height: 260,
      width: 260,
      child: Stack(
        alignment: Alignment.center,
        children: [
          Container(
            width: 220,
            height: 220,
            decoration: BoxDecoration(color: colors.accentOrange.withValues(alpha: 0.12), shape: BoxShape.circle),
          ),
          Container(
            width: 120,
            height: 120,
            decoration: BoxDecoration(
              color: colors.accentOrange,
              borderRadius: BorderRadius.circular(32),
              boxShadow: [BoxShadow(color: colors.accentOrange.withValues(alpha: 0.35), blurRadius: 24, offset: const Offset(0, 10))],
            ),
            child: const Icon(Icons.shopping_cart_rounded, color: Colors.white, size: 56),
          ),
          Positioned(top: 10, left: 18, child: _FloatingBadge(icon: Icons.phone_iphone_rounded, color: colors.primary)),
          Positioned(top: 24, right: 10, child: _FloatingBadge(icon: Icons.headphones_rounded, color: colors.sage)),
          Positioned(bottom: 14, left: 6, child: _FloatingBadge(icon: Icons.local_mall_rounded, color: colors.primaryDark)),
          Positioned(bottom: 0, right: 24, child: _FloatingBadge(icon: Icons.checkroom_rounded, color: colors.accentOrange)),
        ],
      ),
    );
  }
}

/// Page 2 : livreur en tenue Maplenou, repère de localisation en superposition.
class _DeliveryIllustration extends StatelessWidget {
  const _DeliveryIllustration();

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    return Stack(
      clipBehavior: Clip.none,
      alignment: Alignment.center,
      children: [
        ClipRRect(
          borderRadius: BorderRadius.circular(28),
          child: Image.asset(
            'assets/images/role_courier.jpg',
            height: 280,
            width: 280,
            fit: BoxFit.cover,
          ),
        ),
        Positioned(
          bottom: -14,
          right: -6,
          child: Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: Colors.white,
              shape: BoxShape.circle,
              boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.15), blurRadius: 12, offset: const Offset(0, 4))],
            ),
            child: Icon(Icons.location_on_rounded, color: colors.accentOrange, size: 28),
          ),
        ),
      ],
    );
  }
}

/// Page 3 : aperçu stylisé de l'écran d'accueil dans un cadre "téléphone".
class _AppPreviewIllustration extends StatelessWidget {
  const _AppPreviewIllustration();

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    return Container(
      width: 220,
      height: 280,
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(28),
        border: Border.all(color: colors.border, width: 3),
        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.08), blurRadius: 20, offset: const Offset(0, 10))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const AppLogo(variant: AppLogoVariant.iconColor, size: 22),
          const SizedBox(height: 12),
          Container(
            height: 30,
            decoration: BoxDecoration(color: colors.inputFill, borderRadius: BorderRadius.circular(10)),
            padding: const EdgeInsets.symmetric(horizontal: 10),
            alignment: Alignment.centerLeft,
            child: Icon(Icons.search_rounded, size: 16, color: colors.textMuted),
          ),
          const SizedBox(height: 14),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              _CategoryDot(color: colors.primary, icon: Icons.checkroom_rounded),
              _CategoryDot(color: colors.accentOrange, icon: Icons.headphones_rounded),
              _CategoryDot(color: colors.sage, icon: Icons.chair_rounded),
              _CategoryDot(color: colors.primaryDark, icon: Icons.spa_rounded),
            ],
          ),
          const SizedBox(height: 14),
          Text('Produits populaires', style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: colors.textDark)),
          const SizedBox(height: 8),
          Expanded(
            child: Row(
              children: [
                Expanded(child: _ProductPlaceholderCard(color: colors.accentOrange)),
                const SizedBox(width: 8),
                Expanded(child: _ProductPlaceholderCard(color: colors.primary)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _CategoryDot extends StatelessWidget {
  final Color color;
  final IconData icon;

  const _CategoryDot({required this.color, required this.icon});

  @override
  Widget build(BuildContext context) {
    return CircleAvatar(radius: 16, backgroundColor: color.withValues(alpha: 0.15), child: Icon(icon, size: 15, color: color));
  }
}

class _ProductPlaceholderCard extends StatelessWidget {
  final Color color;

  const _ProductPlaceholderCard({required this.color});

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(color: color.withValues(alpha: 0.12), borderRadius: BorderRadius.circular(10)),
    );
  }
}

class _FloatingBadge extends StatelessWidget {
  final IconData icon;
  final Color color;

  const _FloatingBadge({required this.icon, required this.color});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 44,
      height: 44,
      decoration: BoxDecoration(
        color: Colors.white,
        shape: BoxShape.circle,
        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.1), blurRadius: 10, offset: const Offset(0, 4))],
      ),
      child: Icon(icon, color: color, size: 20),
    );
  }
}
