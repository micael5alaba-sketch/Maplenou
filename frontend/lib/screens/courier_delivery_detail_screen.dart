import 'package:flutter/material.dart';

import '../models/active_delivery_detail_model.dart';
import '../services/active_delivery_service.dart';
import '../services/api_client.dart';
import '../services/courier_delivery_service.dart';
import '../services/courier_location_tracking_service.dart';
import '../services/courier_messaging_service.dart';
import '../theme/app_color_scheme.dart';
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
    return ClipRRect(
      borderRadius: BorderRadius.circular(20),
      child: Container(
        height: 200,
        color: colors.inputFill,
        child: Stack(
          children: [
            Center(child: Icon(Icons.map_outlined, size: 48, color: colors.textMuted)),
            if (delivery.distanceKm != null)
              Positioned(
                top: 12,
                left: 0,
                right: 0,
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
          ],
        ),
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
