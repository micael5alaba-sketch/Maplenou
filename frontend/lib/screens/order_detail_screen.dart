import 'package:flutter/material.dart';

import '../models/vendor_order_model.dart';
import '../services/api_client.dart';
import '../services/vendor_orders_service.dart';
import '../theme/app_color_scheme.dart';
import '../utils/formatters.dart';

/// "Détail de la commande" — reached from [OrdersManagementScreen]'s
/// "Voir détails" action.
///
/// Backed by `GET /api/sub-orders/{id}` (`SubOrderResponse`, avec le détail
/// des articles) via [VendorOrdersService.getSubOrderDetail].
class OrderDetailScreen extends StatefulWidget {
  final String subOrderId;

  const OrderDetailScreen({super.key, required this.subOrderId});

  @override
  State<OrderDetailScreen> createState() => _OrderDetailScreenState();
}

class _OrderDetailScreenState extends State<OrderDetailScreen> {
  final _service = VendorOrdersService();
  late Future<Map<String, dynamic>> _detailFuture;
  bool _isAdvancing = false;

  static const _statusLabels = {
    'PENDING': 'En attente',
    'PREPARING': 'En préparation',
    'READY_FOR_PICKUP': 'Prête pour ramassage',
    'IN_DELIVERY': 'En livraison',
    'DELIVERED': 'Livrée',
    'CANCELLED': 'Annulée',
    'RETURN_REQUESTED': 'Retour demandé',
    'RETURNED': 'Retournée',
  };

  static const _nextStatus = {'PENDING': 'PREPARING', 'PREPARING': 'READY_FOR_PICKUP'};
  static const _nextActionLabel = {'PENDING': 'Marquer en préparation', 'PREPARING': 'Marquer prête pour ramassage'};

  @override
  void initState() {
    super.initState();
    _detailFuture = _service.getSubOrderDetail(widget.subOrderId);
  }

  void _reload() {
    setState(() => _detailFuture = _service.getSubOrderDetail(widget.subOrderId));
  }

  Future<void> _advance(String currentStatus) async {
    final next = _nextStatus[currentStatus];
    if (next == null) return;
    setState(() => _isAdvancing = true);
    try {
      await _service.advanceStatus(VendorOrderModel(
        id: widget.subOrderId,
        orderNumber: '',
        dateTime: DateTime.now(),
        customerName: '',
        productNames: const [],
        rawStatus: currentStatus,
        amount: 0,
      ));
      _reload();
    } on ApiException catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(SnackBar(content: Text(e.message)));
    } finally {
      if (mounted) setState(() => _isAdvancing = false);
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
            _buildHeader(colors),
            Expanded(
              child: FutureBuilder<Map<String, dynamic>>(
                future: _detailFuture,
                builder: (context, snapshot) {
                  if (snapshot.connectionState == ConnectionState.waiting) {
                    return Center(child: CircularProgressIndicator(color: colors.primary));
                  }
                  if (snapshot.hasError || !snapshot.hasData) {
                    return Center(
                      child: Padding(
                        padding: const EdgeInsets.all(32),
                        child: Column(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Text(
                              snapshot.error is ApiException ? (snapshot.error as ApiException).message : 'Impossible de charger cette commande.',
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
                  return _buildContent(colors, snapshot.data!);
                },
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader(AppColorScheme colors) {
    return Container(
      color: colors.surface,
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
      child: Row(
        children: [
          IconButton(icon: Icon(Icons.arrow_back_rounded, color: colors.textDark), onPressed: () => Navigator.of(context).pop()),
          Expanded(child: Text('Détail de la commande', textAlign: TextAlign.center, style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark))),
          const SizedBox(width: 48),
        ],
      ),
    );
  }

  Widget _buildContent(AppColorScheme colors, Map<String, dynamic> detail) {
    final status = detail['status'] as String;
    final items = detail['items'] as List<dynamic>? ?? [];
    final subtotal = detail['subtotal'] as num;
    final commissionRate = detail['commissionRate'] as num;
    final commissionAmount = detail['commissionAmount'] as num;
    final netAmount = detail['netAmount'] as num;
    final createdAt = DateTime.parse(detail['createdAt'] as String);
    final nextAction = _nextActionLabel[status];

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        _buildCard(colors, [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text('#${detail['orderNumber']}', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: colors.textDark)),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(color: colors.primary.withValues(alpha: 0.12), borderRadius: BorderRadius.circular(20)),
                child: Text(_statusLabels[status] ?? status, style: TextStyle(color: colors.primary, fontWeight: FontWeight.w600, fontSize: 12)),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Text('Client : ${detail['buyerName']}', style: TextStyle(color: colors.textDark)),
          const SizedBox(height: 4),
          Text(
            'Passée le ${createdAt.day}/${createdAt.month}/${createdAt.year} à ${createdAt.hour.toString().padLeft(2, '0')}:${createdAt.minute.toString().padLeft(2, '0')}',
            style: TextStyle(color: colors.textMuted, fontSize: 12),
          ),
        ]),
        const SizedBox(height: 14),
        _buildCard(colors, [
          Text('Articles', style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
          const SizedBox(height: 10),
          for (final item in items) _buildItemRow(colors, item as Map<String, dynamic>),
        ]),
        const SizedBox(height: 14),
        _buildCard(colors, [
          Text('Répartition', style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
          const SizedBox(height: 10),
          _buildAmountRow(colors, 'Sous-total', formatFcfa(subtotal)),
          _buildAmountRow(colors, 'Commission ($commissionRate%)', '- ${formatFcfa(commissionAmount)}'),
          const Divider(height: 20),
          _buildAmountRow(colors, 'Montant net vendeur', formatFcfa(netAmount), emphasize: true),
        ]),
        if (nextAction != null) ...[
          const SizedBox(height: 20),
          SizedBox(
            width: double.infinity,
            height: 50,
            child: ElevatedButton(
              onPressed: _isAdvancing ? null : () => _advance(status),
              style: ElevatedButton.styleFrom(backgroundColor: colors.primary, foregroundColor: Colors.white, shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(26))),
              child: _isAdvancing
                  ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                  : Text(nextAction, style: const TextStyle(fontWeight: FontWeight.w600)),
            ),
          ),
        ],
      ],
    );
  }

  Widget _buildItemRow(AppColorScheme colors, Map<String, dynamic> item) {
    final quantity = item['quantity'] as int;
    final unitPrice = item['unitPrice'] as num;
    final lineTotal = item['lineTotal'] as num;
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('${item['productName']}', style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark)),
                Text('${item['variantLabel']} · x$quantity · ${formatFcfa(unitPrice)}', style: TextStyle(fontSize: 12, color: colors.textMuted)),
              ],
            ),
          ),
          Text(formatFcfa(lineTotal), style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark)),
        ],
      ),
    );
  }

  Widget _buildAmountRow(AppColorScheme colors, String label, String value, {bool emphasize = false}) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 6),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: TextStyle(color: emphasize ? colors.textDark : colors.textMuted, fontWeight: emphasize ? FontWeight.bold : FontWeight.normal)),
          Text(value, style: TextStyle(color: emphasize ? colors.primary : colors.textDark, fontWeight: FontWeight.bold, fontSize: emphasize ? 16 : 14)),
        ],
      ),
    );
  }

  Widget _buildCard(AppColorScheme colors, List<Widget> children) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 10, offset: const Offset(0, 3))],
      ),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: children),
    );
  }
}
