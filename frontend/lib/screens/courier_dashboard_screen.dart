import 'package:flutter/material.dart';

import '../models/courier_dashboard_model.dart';
import '../services/api_client.dart';
import '../services/courier_dashboard_service.dart';
import '../services/courier_documents_service.dart';
import '../theme/app_color_scheme.dart';
import '../widgets/active_delivery_card.dart';
import '../widgets/app_drawer.dart';
import '../widgets/app_logo.dart';
import '../widgets/courier_bottom_navigation.dart';
import '../widgets/daily_earnings_card.dart';
import '../widgets/earnings_evolution_card.dart';
import '../widgets/online_status_toggle.dart';
import '../widgets/performance_card.dart';
import 'courier_delivery_confirmation_screen.dart';
import 'courier_delivery_detail_screen.dart';
import 'courier_documents_screen.dart';
import 'courier_messages_screen.dart';
import 'courier_profile_screen.dart';
import 'notifications_screen.dart';
import 'courier_deliveries_screen.dart';

/// Courier's main dashboard: online/offline toggle (état local, pas de
/// notion backend de disponibilité), évolution des gains, objectif du
/// jour, performance, et livraisons actives.
///
/// Backed by [CourierDashboardService] (gains/livraisons réels — note et
/// objectif du jour restent volontairement sans équivalent backend, voir
/// ce service).
class CourierDashboardScreen extends StatefulWidget {
  const CourierDashboardScreen({super.key});

  @override
  State<CourierDashboardScreen> createState() => _CourierDashboardScreenState();
}

class _CourierDashboardScreenState extends State<CourierDashboardScreen> {
  final _scaffoldKey = GlobalKey<ScaffoldState>();
  final _service = CourierDashboardService();
  final _documentsService = CourierDocumentsService();
  late Future<CourierDashboardModel> _dashboardFuture;
  late Future<bool> _verifiedFuture;
  bool _isOnline = false;

  final CourierTab _currentTab = CourierTab.dashboard;

  @override
  void initState() {
    super.initState();
    _dashboardFuture = _service.getDashboard(isOnline: _isOnline);
    _verifiedFuture = _documentsService.isFullyVerified();
  }

  void _setOnline(bool isOnline) {
    setState(() {
      _isOnline = isOnline;
      _dashboardFuture = _service.getDashboard(isOnline: isOnline);
    });
  }

  void _reload() {
    setState(() {
      _dashboardFuture = _service.getDashboard(isOnline: _isOnline);
      _verifiedFuture = _documentsService.isFullyVerified();
    });
  }

  void _showComingSoon(String feature) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text('$feature bientôt disponible.')));
  }

  void _openDeliveryDetail(ActiveDeliveryModel delivery) {
    Navigator.of(context)
        .push(MaterialPageRoute(builder: (_) => CourierDeliveryDetailScreen(deliveryId: delivery.id)))
        .then((_) => _reload());
  }

  void _openConfirmation(ActiveDeliveryModel delivery) {
    Navigator.of(context)
        .push(MaterialPageRoute(builder: (_) => CourierDeliveryConfirmationScreen(deliveryId: delivery.id)))
        .then((_) => _reload());
  }

  // `pushReplacement` dans tous les cas (jamais `pop`/`push`) : voir le même motif corrigé
  // côté vendeur (seller_dashboard_screen.dart) et acheteur (home_screen.dart). Plus besoin
  // du `.then(_reload)` : avec pushReplacement on ne "revient" jamais sur cet écran par pop,
  // retourner au dashboard recrée l'écran et son initState recharge déjà les données.
  void _onTabSelected(CourierTab tab) {
    if (tab == _currentTab) return;
    switch (tab) {
      case CourierTab.dashboard:
        break;
      case CourierTab.deliveries:
        Navigator.of(context).pushReplacement(MaterialPageRoute(builder: (_) => const CourierDeliveriesScreen()));
        break;
      case CourierTab.messages:
        Navigator.of(context).pushReplacement(MaterialPageRoute(builder: (_) => const CourierMessagesScreen()));
        break;
      case CourierTab.profile:
        Navigator.of(context).pushReplacement(MaterialPageRoute(builder: (_) => const CourierProfileScreen()));
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
              child: FutureBuilder<bool>(
                future: _verifiedFuture,
                builder: (context, verifiedSnapshot) {
                  if (verifiedSnapshot.connectionState == ConnectionState.waiting) {
                    return const Center(child: CircularProgressIndicator());
                  }
                  if (verifiedSnapshot.hasError) {
                    return _buildErrorState(colors, verifiedSnapshot.error);
                  }
                  if (verifiedSnapshot.data != true) {
                    return _buildVerificationPendingState(colors);
                  }
                  return FutureBuilder<CourierDashboardModel>(
                    future: _dashboardFuture,
                    builder: (context, snapshot) {
                      if (snapshot.connectionState == ConnectionState.waiting) {
                        return const Center(child: CircularProgressIndicator());
                      }
                      if (snapshot.hasError || !snapshot.hasData) {
                        return _buildErrorState(colors, snapshot.error);
                      }
                      return _buildContent(context, colors, snapshot.data!);
                    },
                  );
                },
              ),
            ),
          ],
        ),
      ),
      bottomNavigationBar: CourierBottomNavigation(currentTab: _currentTab, onTabSelected: _onTabSelected),
    );
  }

  /// Cahier des charges §3.3/§3.4 : tant que l'admin n'a pas validé les 4
  /// documents obligatoires, le livreur ne doit avoir accès à rien d'autre
  /// qu'au dépôt de ses documents — aucune livraison ne peut lui être
  /// affectée de toute façon (verrou appliqué aussi côté backend).
  Widget _buildVerificationPendingState(AppColorScheme colors) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.pending_actions_rounded, size: 56, color: colors.primary),
            const SizedBox(height: 16),
            Text(
              'Compte en cours de validation',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: colors.textDark),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 10),
            Text(
              'Déposez vos documents (pièce d\'identité, permis, assurance et carte grise du '
              'véhicule). Vous recevrez vos premières livraisons dès que notre équipe les aura validés.',
              textAlign: TextAlign.center,
              style: TextStyle(color: colors.textMuted, height: 1.4),
            ),
            const SizedBox(height: 24),
            ElevatedButton(
              onPressed: () async {
                await Navigator.of(context).push(MaterialPageRoute(builder: (_) => const CourierDocumentsScreen()));
                _reload();
              },
              style: ElevatedButton.styleFrom(
                backgroundColor: colors.primary,
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 14),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(26)),
              ),
              child: const Text('Déposer mes documents', style: TextStyle(fontWeight: FontWeight.w600)),
            ),
            const SizedBox(height: 8),
            TextButton(onPressed: _reload, child: const Text('Rafraîchir le statut')),
          ],
        ),
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
              error is ApiException ? error.message : 'Impossible de charger le dashboard.',
              textAlign: TextAlign.center,
              style: TextStyle(color: colors.textMuted),
            ),
            const SizedBox(height: 12),
            TextButton(onPressed: _reload, child: const Text('Réessayer')),
          ],
        ),
      ),
    );
  }

  Widget _buildContent(BuildContext context, AppColorScheme colors, CourierDashboardModel dashboard) {
    return SingleChildScrollView(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Bonjour, ${dashboard.courierName}',
            style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: colors.textDark),
          ),
          const SizedBox(height: 4),
          Text('Prêt pour votre journée ?', style: TextStyle(fontSize: 14, color: colors.textMuted)),
          const SizedBox(height: 16),
          OnlineStatusToggle(isOnline: dashboard.isOnline, onChanged: _setOnline),
          const SizedBox(height: 20),
          EarningsEvolutionCard(
            points: dashboard.weeklyEarningsSeries,
            weeklyTotal: dashboard.weeklyTotal,
            changePercent: dashboard.weeklyChangePercent ?? 0,
          ),
          const SizedBox(height: 16),
          DailyEarningsCard(earnings: dashboard.dailyEarnings, goal: dashboard.dailyGoal),
          const SizedBox(height: 16),
          PerformanceCard(
            rating: dashboard.rating,
            validatedDeliveries: dashboard.validatedDeliveries,
            totalDeliveries: dashboard.totalDeliveriesToday,
          ),
          const SizedBox(height: 20),
          SizedBox(
            width: double.infinity,
            height: 54,
            child: ElevatedButton.icon(
              onPressed: dashboard.isOnline
                  ? () => Navigator.of(context)
                      .push(MaterialPageRoute(builder: (_) => const CourierDeliveriesScreen()))
                      .then((_) => _reload())
                  : () => _setOnline(true),
              style: ElevatedButton.styleFrom(
                backgroundColor: colors.accentOrange,
                foregroundColor: Colors.white,
                elevation: 0,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28)),
              ),
              icon: Icon(dashboard.isOnline ? Icons.local_shipping_rounded : Icons.toggle_on_rounded),
              label: Text(
                dashboard.isOnline ? 'Voir mes livraisons' : 'Passer en ligne',
                style: const TextStyle(fontWeight: FontWeight.w600),
              ),
            ),
          ),
          const SizedBox(height: 20),
          if (dashboard.isOnline && dashboard.activeDeliveries.isNotEmpty) ...[
            Text('Courses Actives', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: colors.textDark)),
            const SizedBox(height: 12),
            for (final delivery in dashboard.activeDeliveries) ...[
              ActiveDeliveryCard(
                delivery: delivery,
                onTap: () => _openDeliveryDetail(delivery),
                onConfirm: delivery.status == ActiveDeliveryStatus.enRoute ? () => _openConfirmation(delivery) : null,
              ),
              const SizedBox(height: 12),
            ],
          ] else
            _buildOfflineState(colors, dashboard.isOnline),
        ],
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
          const Expanded(child: Center(child: AppLogo(variant: AppLogoVariant.horizontalColor, size: 34))),
          IconButton(
            icon: Icon(Icons.notifications_none_rounded, color: colors.textDark),
            onPressed: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const NotificationsScreen())),
          ),
        ],
      ),
    );
  }

  Widget _buildOfflineState(AppColorScheme colors, bool isOnline) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 12, offset: const Offset(0, 4)),
        ],
      ),
      child: Column(
        children: [
          Icon(Icons.cloud_off_rounded, size: 40, color: colors.textMuted),
          const SizedBox(height: 12),
          Text(
            isOnline ? 'Aucune livraison active pour le moment.' : 'Vous êtes hors ligne',
            style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark),
          ),
          const SizedBox(height: 6),
          Text(
            isOnline
                ? "Consultez l'onglet Courses pour vos livraisons assignées."
                : 'Passez en ligne pour voir vos livraisons assignées.',
            textAlign: TextAlign.center,
            style: TextStyle(fontSize: 13, color: colors.textMuted, height: 1.4),
          ),
        ],
      ),
    );
  }
}
