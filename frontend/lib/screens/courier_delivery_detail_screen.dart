import 'package:flutter/material.dart';
import 'package:latlong2/latlong.dart' as ll;
import 'package:url_launcher/url_launcher.dart';

import '../models/active_delivery_detail_model.dart';
import '../services/active_delivery_service.dart';
import '../services/api_client.dart';
import '../services/courier_delivery_service.dart';
import '../services/courier_location_tracking_service.dart';
import '../services/courier_messaging_service.dart';
import '../theme/app_color_scheme.dart';
import '../utils/formatters.dart';
import '../widgets/live_tracking_map.dart';
import 'courier_conversation_screen.dart';
import 'courier_delivery_confirmation_screen.dart';

/// Detail of one delivery: a map placeholder, l'adresse de retrait/
/// livraison, et l'action pertinente selon le statut réel : "Commencer"
/// (ASSIGNED → IN_TRANSIT) ou "Confirmer la livraison" (IN_TRANSIT → écran
/// de preuve). Pendant IN_TRANSIT, pousse la position GPS en direct sur
/// Firebase via [CourierLocationTrackingService] pour que l'acheteur la
/// voie sur [OrderTrackingScreen].
///
/// Reached from [CourierDashboardScreen] / [CourierDeliveriesScreen].
class CourierDeliveryDetailScreen extends StatefulWidget {
  final String deliveryId;

  const CourierDeliveryDetailScreen({super.key, required this.deliveryId});

  @override
  State<CourierDeliveryDetailScreen> createState() => _CourierDeliveryDetailScreenState();
}

class _CourierDeliveryDetailScreenState extends State<CourierDeliveryDetailScreen> {
  final _deliveryService = CourierDeliveryService();
  final _trackingService = CourierLocationTrackingService();
  late Future<ActiveDeliveryDetailModel> _deliveryFuture;
  bool _isStarting = false;

  @override
  void initState() {
    super.initState();
    _deliveryFuture = ActiveDeliveryService().getActiveDelivery(widget.deliveryId)..then(_syncTracking);
  }

  @override
  void dispose() {
    _trackingService.stop();
    super.dispose();
  }

  void _reload() {
    setState(() {
      _deliveryFuture = ActiveDeliveryService().getActiveDelivery(widget.deliveryId)..then(_syncTracking);
    });
  }

  Future<void> _syncTracking(ActiveDeliveryDetailModel delivery) async {
    if (delivery.status == 'IN_TRANSIT') {
      try {
        await _trackingService.start(delivery.id);
      } catch (_) {
        // Suivi GPS indisponible (permission refusée, service désactivé...) —
        // la livraison continue normalement, ce n'est pas bloquant.
      }
    } else {
      await _trackingService.stop();
    }
  }

  void _showComingSoon(String feature) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text('$feature bientôt disponible.')));
  }

  Future<void> _contactClient(String clientName) async {
    try {
      final conversationId = await CourierMessagingService().startClientConversation(widget.deliveryId);
      if (!mounted) return;
      Navigator.of(context).push(MaterialPageRoute(
        builder: (_) => CourierConversationScreen(conversationId: conversationId, participantName: clientName),
      ));
    } on ApiException catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(SnackBar(content: Text(e.message)));
    }
  }

  Future<void> _handleStart() async {
    setState(() => _isStarting = true);
    try {
      await _deliveryService.startDelivery(widget.deliveryId);
      _reload();
    } on ApiException catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context)
          ..hideCurrentSnackBar()
          ..showSnackBar(SnackBar(content: Text(e.message)));
      }
    } finally {
      if (mounted) setState(() => _isStarting = false);
    }
  }

  /// Ouvre l'app Google Maps installée sur le téléphone avec l'itinéraire
  /// vers le point de retrait (tant que la course n'est pas commencée) ou
  /// vers le client (une fois en route) — pas de carte intégrée, pas de clé
  /// Google Maps Platform (voir `INVENTAIRE_ABONNEMENTS.md` §3).
  Future<void> _openNavigation(ActiveDeliveryDetailModel delivery) async {
    final lat = delivery.navigationLatitude;
    final lng = delivery.navigationLongitude;
    if (lat == null || lng == null) {
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(const SnackBar(content: Text('Position GPS non disponible pour cette adresse.')));
      return;
    }
    final uri = Uri.parse('https://www.google.com/maps/dir/?api=1&destination=$lat,$lng');
    final opened = await launchUrl(uri, mode: LaunchMode.externalApplication);
    if (!opened && mounted) {
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(const SnackBar(content: Text('Impossible d\'ouvrir Google Maps.')));
    }
  }

  void _handleConfirm() {
    Navigator.of(context)
        .push(MaterialPageRoute(builder: (_) => CourierDeliveryConfirmationScreen(deliveryId: widget.deliveryId)))
        .then((_) => _reload());
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return Scaffold(
      backgroundColor: colors.homeBackground,
      body: SafeArea(
        child: Column(
          children: [
            _buildHeader(context, colors),
            Expanded(
              child: FutureBuilder<ActiveDeliveryDetailModel>(
                future: _deliveryFuture,
                builder: (context, snapshot) {
                  if (snapshot.connectionState == ConnectionState.waiting) {
                    return const Center(child: CircularProgressIndicator());
                  }
                  if (snapshot.hasError || !snapshot.hasData) {
                    return Center(
                      child: Text(
                        snapshot.error is ApiException
                            ? (snapshot.error as ApiException).message
                            : 'Impossible de charger la livraison.',
                        style: TextStyle(color: colors.textMuted),
                      ),
                    );
                  }
                  return _buildContent(context, colors, snapshot.data!);
                },
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildContent(BuildContext context, AppColorScheme colors, ActiveDeliveryDetailModel delivery) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        _buildMapPlaceholder(colors, delivery),
        const SizedBox(height: 16),
        if (delivery.estimatedEarning != null) ...[
          _buildEarningCard(colors, delivery.estimatedEarning!),
          const SizedBox(height: 16),
        ],
        _buildClientCard(context, colors, delivery),
        const SizedBox(height: 16),
        _buildAddressCard(colors, delivery),
        const SizedBox(height: 16),
        _buildPickupCard(colors, delivery),
        const SizedBox(height: 20),
        if (delivery.canStart)
          SizedBox(
            width: double.infinity,
            height: 54,
            child: ElevatedButton.icon(
              onPressed: _isStarting ? null : _handleStart,
              style: ElevatedButton.styleFrom(
                backgroundColor: colors.primary,
                foregroundColor: Colors.white,
                elevation: 0,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28)),
              ),
              icon: _isStarting
                  ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                  : const Icon(Icons.play_circle_outline_rounded),
              label: const Text('Commencer la livraison', style: TextStyle(fontWeight: FontWeight.w600)),
            ),
          )
        else if (delivery.canConfirm)
          SizedBox(
            width: double.infinity,
            height: 54,
            child: ElevatedButton.icon(
              onPressed: _handleConfirm,
              style: ElevatedButton.styleFrom(
                backgroundColor: colors.primary,
                foregroundColor: Colors.white,
                elevation: 0,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28)),
              ),
              icon: const Icon(Icons.check_circle_outline_rounded),
              label: const Text('Confirmer la livraison', style: TextStyle(fontWeight: FontWeight.w600)),
            ),
          ),
      ],
    );
  }

  Widget _buildHeader(BuildContext context, AppColorScheme colors) {
    return Container(
      color: colors.surface,
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
      child: Row(
        children: [
          IconButton(
            icon: Icon(Icons.arrow_back_rounded, color: colors.textDark),
            onPressed: () => Navigator.of(context).pop(),
          ),
          Expanded(
            child: Text('Détail de la livraison', style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
          ),
          IconButton(
            icon: Icon(Icons.more_vert_rounded, color: colors.textDark),
            onPressed: () => _showComingSoon('Plus d\'options'),
          ),
        ],
      ),
    );
  }

  Widget _buildMapPlaceholder(AppColorScheme colors, ActiveDeliveryDetailModel delivery) {
    final hasTarget = delivery.hasNavigationTarget;
    final points = <TrackedPoint>[
      if (delivery.pickupLatitude != null && delivery.pickupLongitude != null)
        TrackedPoint(
          position: ll.LatLng(delivery.pickupLatitude!, delivery.pickupLongitude!),
          icon: Icons.storefront_rounded,
          color: colors.accentOrange,
        ),
      if (delivery.dropoffLatitude != null && delivery.dropoffLongitude != null)
        TrackedPoint(
          position: ll.LatLng(delivery.dropoffLatitude!, delivery.dropoffLongitude!),
          icon: Icons.location_on_rounded,
          color: colors.primary,
        ),
    ];

    return Column(
      children: [
        ClipRRect(
          borderRadius: BorderRadius.circular(20),
          child: Stack(
            children: [
              LiveTrackingMap(points: points, height: 200),
              if (delivery.distanceKm != null)
                Positioned(
                  top: 12,
                  left: 0,
                  right: 0,
                  child: IgnorePointer(
                    child: Center(
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                        decoration: BoxDecoration(color: colors.surface, borderRadius: BorderRadius.circular(20)),
                        child: Text(
                          '${delivery.distanceKm!.toStringAsFixed(1)} km à vol d\'oiseau',
                          style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark, fontSize: 13),
                        ),
                      ),
                    ),
                  ),
                ),
            ],
          ),
        ),
        const SizedBox(height: 10),
        if (hasTarget)
          SizedBox(
            width: double.infinity,
            child: ElevatedButton.icon(
              onPressed: () => _openNavigation(delivery),
              style: ElevatedButton.styleFrom(
                backgroundColor: colors.primary,
                foregroundColor: Colors.white,
                elevation: 0,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
              ),
              icon: const Icon(Icons.directions_rounded, size: 18),
              label: const Text('Ouvrir l\'itinéraire dans Google Maps'),
            ),
          )
        else
          Text(
            'Position GPS non disponible pour cette adresse',
            style: TextStyle(color: colors.textMuted, fontSize: 12),
          ),
      ],
    );
  }

  Widget _buildEarningCard(AppColorScheme colors, double estimatedEarning) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: colors.primary.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: colors.primary.withValues(alpha: 0.25)),
      ),
      child: Row(
        children: [
          Icon(Icons.payments_outlined, color: colors.primary),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('GAIN ESTIMÉ POUR CETTE COURSE', style: TextStyle(fontSize: 11, fontWeight: FontWeight.w700, color: colors.textMuted)),
                const SizedBox(height: 2),
                Text(formatFcfa(estimatedEarning), style: TextStyle(fontWeight: FontWeight.bold, color: colors.primary, fontSize: 16)),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildClientCard(BuildContext context, AppColorScheme colors, ActiveDeliveryDetailModel delivery) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 10, offset: const Offset(0, 3))],
      ),
      child: Row(
        children: [
          CircleAvatar(
            radius: 22,
            backgroundColor: colors.inputFill,
            child: Text(
              delivery.clientName.isNotEmpty ? delivery.clientName[0] : '?',
              style: TextStyle(fontWeight: FontWeight.bold, color: colors.primary),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(delivery.clientName, style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
                Text('Client • ${delivery.orderNumber}', style: TextStyle(fontSize: 12, color: colors.textMuted)),
              ],
            ),
          ),
          IconButton(
            icon: Icon(Icons.chat_bubble_outline_rounded, color: colors.primary),
            onPressed: () => _contactClient(delivery.clientName),
          ),
        ],
      ),
    );
  }

  Widget _buildAddressCard(AppColorScheme colors, ActiveDeliveryDetailModel delivery) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 10, offset: const Offset(0, 3))],
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.location_on_outlined, color: colors.primary),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('ADRESSE DE LIVRAISON', style: TextStyle(fontSize: 11, fontWeight: FontWeight.w700, color: colors.textMuted)),
                const SizedBox(height: 4),
                Text(delivery.deliveryAddress, style: TextStyle(color: colors.textDark, height: 1.3)),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildPickupCard(AppColorScheme colors, ActiveDeliveryDetailModel delivery) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 10, offset: const Offset(0, 3))],
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.storefront_outlined, color: colors.accentOrange),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('RETRAIT CHEZ', style: TextStyle(fontSize: 11, fontWeight: FontWeight.w700, color: colors.textMuted)),
                const SizedBox(height: 4),
                Text(delivery.shopName, style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark)),
                Text(delivery.pickupAddress, style: TextStyle(fontSize: 12, color: colors.textMuted)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
