import 'package:flutter/material.dart';

import '../models/courier_live_location_model.dart';
import '../models/order_tracking_model.dart';
import '../services/api_client.dart';
import '../services/buyer_courier_tracking_service.dart';
import '../services/conversation_service.dart';
import '../services/order_tracking_service.dart';
import '../theme/app_color_scheme.dart';
import '../widgets/courier_rating_dialog.dart';
import 'courier_conversation_screen.dart';

/// "Suivi de commande" — statut réel de la sous-commande, adresse de
/// livraison réelle, nom du livreur une fois assigné, et sa position GPS en
/// direct pendant IN_DELIVERY (voir [BuyerCourierTrackingService] — flux
/// 100% Firebase Realtime Database, le backend n'y participe pas).
///
/// Backed by [OrderTrackingService].
class OrderTrackingScreen extends StatefulWidget {
  final String orderId;

  const OrderTrackingScreen({super.key, required this.orderId});

  @override
  State<OrderTrackingScreen> createState() => _OrderTrackingScreenState();
}

class _OrderTrackingScreenState extends State<OrderTrackingScreen> {
  late Future<OrderTrackingModel> _trackingFuture;
  bool _courierRated = false;

  @override
  void initState() {
    super.initState();
    _trackingFuture = OrderTrackingService().getTracking(widget.orderId);
  }

  void _showComingSoon(String feature) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text('$feature bientôt disponible.')));
  }

  Future<void> _contactCourier(String deliveryId, String courierName) async {
    try {
      final conversationId = await ConversationService().startCourierConversation(deliveryId);
      if (!mounted) return;
      Navigator.of(context).push(MaterialPageRoute(
        builder: (_) => CourierConversationScreen(conversationId: conversationId, participantName: courierName),
      ));
    } on ApiException catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(SnackBar(content: Text(e.message)));
    }
  }

  Future<void> _rateCourier(String deliveryId, String courierName) async {
    final submitted = await CourierRatingDialog.show(context, deliveryId: deliveryId, courierName: courierName);
    if (submitted == true) {
      setState(() => _courierRated = true);
      if (!mounted) return;
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(const SnackBar(content: Text('Merci pour votre note !')));
    }
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
              child: FutureBuilder<OrderTrackingModel>(
                future: _trackingFuture,
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
              error is ApiException ? error.message : 'Impossible de charger le suivi de la commande.',
              textAlign: TextAlign.center,
              style: TextStyle(color: colors.textMuted),
            ),
            const SizedBox(height: 12),
            TextButton(
              onPressed: () => setState(() {
                _trackingFuture = OrderTrackingService().getTracking(widget.orderId);
              }),
              child: const Text('Réessayer'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildContent(AppColorScheme colors, OrderTrackingModel tracking) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        _buildOrderStatusCard(colors, tracking),
        const SizedBox(height: 16),
        _buildMapPlaceholder(colors, tracking),
        const SizedBox(height: 16),
        _buildCourierCard(context, colors, tracking),
        const SizedBox(height: 16),
        _buildStatusTimeline(colors, tracking),
        const SizedBox(height: 16),
        _buildAddressCard(colors, tracking),
        const SizedBox(height: 16),
        _buildSupportCard(context, colors),
      ],
    );
  }

  Widget _buildHeader(BuildContext context, AppColorScheme colors) {
    return Container(
      color: colors.surface,
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
      child: Row(
        children: [
          IconButton(icon: Icon(Icons.arrow_back_rounded, color: colors.textDark), onPressed: () => Navigator.of(context).pop()),
          Expanded(child: Text('Suivi de Livraison', textAlign: TextAlign.center, style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark))),
          const SizedBox(width: 48),
        ],
      ),
    );
  }

  Widget _buildOrderStatusCard(AppColorScheme colors, OrderTrackingModel tracking) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 10, offset: const Offset(0, 3))],
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('COMMANDE', style: TextStyle(fontSize: 11, color: colors.textMuted, fontWeight: FontWeight.w700)),
                Text('#${tracking.orderNumber}', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: colors.textDark)),
              ],
            ),
          ),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
            decoration: BoxDecoration(color: colors.primary.withValues(alpha: 0.12), borderRadius: BorderRadius.circular(20)),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(Icons.local_shipping_outlined, size: 14, color: colors.primary),
                const SizedBox(width: 4),
                Text('En cours', style: TextStyle(color: colors.primary, fontSize: 11, fontWeight: FontWeight.w700)),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMapPlaceholder(AppColorScheme colors, OrderTrackingModel tracking) {
    final isLiveTrackable = !tracking.isDelivered && tracking.courierName != null && tracking.deliveryId != null;

    return ClipRRect(
      borderRadius: BorderRadius.circular(20),
      child: Container(
        height: 180,
        color: colors.inputFill,
        child: isLiveTrackable
            ? StreamBuilder<CourierLiveLocationModel?>(
                stream: BuyerCourierTrackingService().watch(tracking.deliveryId!),
                builder: (context, snapshot) {
                  final location = snapshot.data;
                  return Stack(
                    alignment: Alignment.center,
                    children: [
                      Icon(Icons.map_outlined, size: 48, color: colors.textMuted),
                      Positioned(
                        top: 12,
                        left: 0,
                        right: 0,
                        child: Center(
                          child: Container(
                            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                            decoration: BoxDecoration(color: colors.surface, borderRadius: BorderRadius.circular(20)),
                            child: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                Icon(
                                  Icons.circle,
                                  size: 8,
                                  color: location != null ? Colors.green : colors.textMuted,
                                ),
                                const SizedBox(width: 6),
                                Text(
                                  location != null ? 'Livreur en direct' : 'En attente de la position du livreur…',
                                  style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark, fontSize: 12),
                                ),
                              ],
                            ),
                          ),
                        ),
                      ),
                      if (location != null)
                        Positioned(
                          bottom: 12,
                          left: 0,
                          right: 0,
                          child: Center(
                            child: Text(
                              'Position mise à jour ${_formatFreshness(location.updatedAt)}',
                              style: TextStyle(fontSize: 11, color: colors.textMuted),
                            ),
                          ),
                        ),
                    ],
                  );
                },
              )
            : Center(child: Icon(Icons.map_outlined, size: 48, color: colors.textMuted)),
      ),
    );
  }

  String _formatFreshness(DateTime updatedAt) {
    final seconds = DateTime.now().difference(updatedAt).inSeconds;
    if (seconds < 5) return "à l'instant";
    if (seconds < 60) return 'il y a ${seconds}s';
    return 'il y a ${seconds ~/ 60} min';
  }

  Widget _buildCourierCard(BuildContext context, AppColorScheme colors, OrderTrackingModel tracking) {
    final courierName = tracking.courierName;
    final canRate = tracking.isDelivered && courierName != null && tracking.deliveryId != null && !_courierRated;

    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 10, offset: const Offset(0, 3))],
      ),
      child: Column(
        children: [
          Row(
            children: [
              CircleAvatar(
                radius: 22,
                backgroundColor: colors.inputFill,
                child: Icon(Icons.local_shipping_outlined, color: colors.primary),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('Votre livreur', style: TextStyle(fontSize: 11, color: colors.textMuted)),
                    Text(
                      courierName ?? "Pas encore assigné",
                      style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark),
                    ),
                  ],
                ),
              ),
              if (courierName != null && tracking.deliveryId != null && !tracking.isDelivered) ...[
                IconButton(
                  icon: Icon(Icons.chat_bubble_outline_rounded, color: colors.primary),
                  onPressed: () => _contactCourier(tracking.deliveryId!, courierName),
                ),
                IconButton(
                  icon: Icon(Icons.phone_rounded, color: Colors.white),
                  style: IconButton.styleFrom(backgroundColor: colors.primary),
                  onPressed: () => _showComingSoon("L'appel"),
                ),
              ],
            ],
          ),
          if (canRate) ...[
            const SizedBox(height: 10),
            SizedBox(
              width: double.infinity,
              child: OutlinedButton.icon(
                onPressed: () => _rateCourier(tracking.deliveryId!, courierName),
                icon: const Icon(Icons.star_outline_rounded, size: 18),
                label: const Text('Noter le livreur'),
              ),
            ),
          ] else if (_courierRated) ...[
            const SizedBox(height: 10),
            Row(
              children: [
                Icon(Icons.check_circle_rounded, size: 16, color: colors.primary),
                const SizedBox(width: 6),
                Text('Livreur noté, merci !', style: TextStyle(fontSize: 12, color: colors.textMuted)),
              ],
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildStatusTimeline(AppColorScheme colors, OrderTrackingModel tracking) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 10, offset: const Offset(0, 3))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('État de la commande', style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
          const SizedBox(height: 12),
          for (var i = 0; i < tracking.steps.length; i++)
            _buildStepRow(colors, tracking.steps[i], isLast: i == tracking.steps.length - 1),
        ],
      ),
    );
  }

  Widget _buildStepRow(AppColorScheme colors, OrderTrackingStep step, {required bool isLast}) {
    final color = step.isDone ? colors.primary : (step.isCurrent ? colors.accentOrange : colors.border);

    return IntrinsicHeight(
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Column(
            children: [
              Container(
                width: 16,
                height: 16,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: step.isDone ? colors.primary : Colors.transparent,
                  border: Border.all(color: color, width: 2),
                ),
                child: step.isDone ? const Icon(Icons.check, size: 10, color: Colors.white) : null,
              ),
              if (!isLast) Expanded(child: Container(width: 2, color: colors.border)),
            ],
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Padding(
              padding: const EdgeInsets.only(bottom: 16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    step.label,
                    style: TextStyle(fontWeight: FontWeight.w600, color: step.isCurrent ? colors.accentOrange : colors.textDark),
                  ),
                  if (step.timeLabel.isNotEmpty) Text(step.timeLabel, style: TextStyle(fontSize: 12, color: colors.textMuted)),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildAddressCard(AppColorScheme colors, OrderTrackingModel tracking) {
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
                Text(tracking.deliveryAddress, style: TextStyle(color: colors.textDark, height: 1.3)),
                if (tracking.addressNote != null && tracking.addressNote!.isNotEmpty) ...[
                  const SizedBox(height: 4),
                  Text('Note : ${tracking.addressNote}', style: TextStyle(fontSize: 12, color: colors.textMuted)),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSupportCard(BuildContext context, AppColorScheme colors) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 10, offset: const Offset(0, 3))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text("Besoin d'aide ?", style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
          const SizedBox(height: 12),
          SizedBox(
            width: double.infinity,
            child: ElevatedButton.icon(
              onPressed: () => _showComingSoon('Le support'),
              style: ElevatedButton.styleFrom(
                backgroundColor: colors.primary,
                foregroundColor: Colors.white,
                elevation: 0,
                padding: const EdgeInsets.symmetric(vertical: 12),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
              ),
              icon: const Icon(Icons.support_agent_rounded, size: 18),
              label: const Text('Contacter le support'),
            ),
          ),
          const SizedBox(height: 8),
          SizedBox(
            width: double.infinity,
            child: OutlinedButton.icon(
              onPressed: () => _showComingSoon('Le signalement de problème'),
              style: OutlinedButton.styleFrom(
                foregroundColor: colors.textDark,
                side: BorderSide(color: colors.border),
                padding: const EdgeInsets.symmetric(vertical: 12),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
              ),
              icon: const Icon(Icons.report_gmailerrorred_rounded, size: 18),
              label: const Text('Signaler un problème'),
            ),
          ),
        ],
      ),
    );
  }
}
