import 'dart:io';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';
import 'package:pdf/pdf.dart';
import 'package:pdf/widgets.dart' as pw;
import 'package:share_plus/share_plus.dart';

import '../models/order_model.dart';
import '../services/api_client.dart';
import '../services/order_service.dart';
import '../theme/app_color_scheme.dart';
import '../utils/formatters.dart';
import 'order_tracking_screen.dart';

/// "Reçu de commande" — détail complet d'une commande passée (articles,
/// adresse, montants), avec export PDF téléchargeable/partageable.
///
/// Backed by `GET /api/orders/{id}` (`OrderResponse`, avec le détail des
/// articles par sous-commande depuis le 01/10/2026 — voir
/// `SubOrderSummary.items`) via [OrderService.getOrder].
class OrderReceiptScreen extends StatefulWidget {
  final String orderId;

  const OrderReceiptScreen({super.key, required this.orderId});

  @override
  State<OrderReceiptScreen> createState() => _OrderReceiptScreenState();
}

class _OrderReceiptScreenState extends State<OrderReceiptScreen> {
  late Future<OrderModel> _orderFuture;
  bool _isExporting = false;

  static const _statusLabels = {
    'CREATED': 'Créée',
    'PAID': 'Payée',
    'CLOSED': 'Terminée',
    'CANCELLED': 'Annulée',
    'PAYMENT_FAILED': 'Paiement échoué',
    'PARTIALLY_REFUNDED': 'Partiellement remboursée',
    'REFUNDED': 'Remboursée',
  };

  @override
  void initState() {
    super.initState();
    _orderFuture = OrderService().getOrder(widget.orderId);
  }

  void _reload() {
    setState(() => _orderFuture = OrderService().getOrder(widget.orderId));
  }

  Future<void> _exportPdf(OrderModel order) async {
    setState(() => _isExporting = true);
    try {
      final bytes = await _buildReceiptPdf(order);
      final dir = await getTemporaryDirectory();
      final file = File('${dir.path}/recu-${order.orderNumber}.pdf');
      await file.writeAsBytes(bytes);
      await Share.shareXFiles([XFile(file.path)], text: 'Reçu commande #${order.orderNumber}');
    } finally {
      if (mounted) setState(() => _isExporting = false);
    }
  }

  Future<Uint8List> _buildReceiptPdf(OrderModel order) async {
    final doc = pw.Document();
    final subtotal = order.totalAmount + order.discountAmount;

    doc.addPage(
      pw.Page(
        pageFormat: PdfPageFormat.a4,
        build: (context) {
          return pw.Column(
            crossAxisAlignment: pw.CrossAxisAlignment.start,
            children: [
              pw.Text('Maplenou', style: pw.TextStyle(fontSize: 22, fontWeight: pw.FontWeight.bold)),
              pw.Text('Reçu de commande', style: const pw.TextStyle(fontSize: 14)),
              pw.SizedBox(height: 16),
              pw.Text('N° de commande : ${order.orderNumber}'),
              pw.Text('Date : ${_formatDate(order.createdAt)}'),
              pw.Text('Statut : ${_statusLabels[order.status] ?? order.status}'),
              if (order.deliveryAddress != null)
                pw.Text('Adresse de livraison : ${order.deliveryAddress!.summary}'
                    '${order.deliveryAddress!.details != null ? ', ${order.deliveryAddress!.details}' : ''}'),
              pw.SizedBox(height: 20),
              for (final subOrder in order.subOrders) ...[
                pw.Text(subOrder.shopName, style: pw.TextStyle(fontWeight: pw.FontWeight.bold, fontSize: 13)),
                pw.SizedBox(height: 6),
                pw.Table(
                  border: pw.TableBorder.all(color: PdfColors.grey300),
                  columnWidths: const {
                    0: pw.FlexColumnWidth(3),
                    1: pw.FlexColumnWidth(1),
                    2: pw.FlexColumnWidth(1.2),
                    3: pw.FlexColumnWidth(1.2),
                  },
                  children: [
                    pw.TableRow(
                      decoration: const pw.BoxDecoration(color: PdfColors.grey200),
                      children: [
                        _cell('Article', bold: true),
                        _cell('Qté', bold: true),
                        _cell('Prix unit.', bold: true),
                        _cell('Total', bold: true),
                      ],
                    ),
                    for (final item in subOrder.items)
                      pw.TableRow(children: [
                        _cell('${item.productName} (${item.variantLabel})'),
                        _cell('${item.quantity}'),
                        _cell(formatFcfa(item.unitPrice)),
                        _cell(formatFcfa(item.lineTotal)),
                      ]),
                  ],
                ),
                pw.SizedBox(height: 16),
              ],
              pw.Divider(),
              _totalRow('Sous-total', formatFcfa(subtotal)),
              if (order.discountAmount > 0) _totalRow('Remise', '- ${formatFcfa(order.discountAmount)}'),
              pw.SizedBox(height: 4),
              _totalRow('Total payé', formatFcfa(order.totalAmount), bold: true),
            ],
          );
        },
      ),
    );

    return doc.save();
  }

  pw.Widget _cell(String text, {bool bold = false}) {
    return pw.Padding(
      padding: const pw.EdgeInsets.all(6),
      child: pw.Text(text, style: pw.TextStyle(fontSize: 10, fontWeight: bold ? pw.FontWeight.bold : null)),
    );
  }

  pw.Widget _totalRow(String label, String value, {bool bold = false}) {
    return pw.Row(
      mainAxisAlignment: pw.MainAxisAlignment.spaceBetween,
      children: [
        pw.Text(label, style: pw.TextStyle(fontWeight: bold ? pw.FontWeight.bold : null)),
        pw.Text(value, style: pw.TextStyle(fontWeight: bold ? pw.FontWeight.bold : null)),
      ],
    );
  }

  String _formatDate(DateTime date) {
    return '${date.day.toString().padLeft(2, '0')}/${date.month.toString().padLeft(2, '0')}/${date.year} '
        '${date.hour.toString().padLeft(2, '0')}:${date.minute.toString().padLeft(2, '0')}';
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
              child: FutureBuilder<OrderModel>(
                future: _orderFuture,
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
                              snapshot.error is ApiException ? (snapshot.error as ApiException).message : 'Impossible de charger le reçu.',
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
          Expanded(child: Text('Reçu de commande', textAlign: TextAlign.center, style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark))),
          const SizedBox(width: 48),
        ],
      ),
    );
  }

  Widget _buildContent(AppColorScheme colors, OrderModel order) {
    final subtotal = order.totalAmount + order.discountAmount;
    final canTrack = order.status == 'CREATED' || order.status == 'PAID';

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        _buildCard(colors, [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text('#${order.orderNumber}', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: colors.textDark)),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(color: colors.primary.withValues(alpha: 0.12), borderRadius: BorderRadius.circular(20)),
                child: Text(_statusLabels[order.status] ?? order.status, style: TextStyle(color: colors.primary, fontWeight: FontWeight.w600, fontSize: 12)),
              ),
            ],
          ),
          const SizedBox(height: 6),
          Text(_formatDate(order.createdAt), style: TextStyle(color: colors.textMuted, fontSize: 12)),
          if (order.deliveryAddress != null) ...[
            const SizedBox(height: 10),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Icon(Icons.location_on_outlined, size: 16, color: colors.textMuted),
                const SizedBox(width: 6),
                Expanded(
                  child: Text(
                    [order.deliveryAddress!.summary, if (order.deliveryAddress!.details != null) order.deliveryAddress!.details!].join(', '),
                    style: TextStyle(fontSize: 12, color: colors.textMuted),
                  ),
                ),
              ],
            ),
          ],
        ]),
        const SizedBox(height: 14),
        for (final subOrder in order.subOrders) ...[
          _buildCard(colors, [
            Text(subOrder.shopName, style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
            const SizedBox(height: 10),
            for (final item in subOrder.items) _buildItemRow(colors, item),
          ]),
          const SizedBox(height: 14),
        ],
        _buildCard(colors, [
          _buildAmountRow(colors, 'Sous-total', formatFcfa(subtotal)),
          if (order.discountAmount > 0) _buildAmountRow(colors, 'Remise', '- ${formatFcfa(order.discountAmount)}'),
          const Divider(height: 20),
          _buildAmountRow(colors, 'Total payé', formatFcfa(order.totalAmount), emphasize: true),
        ]),
        const SizedBox(height: 20),
        SizedBox(
          width: double.infinity,
          height: 50,
          child: ElevatedButton.icon(
            onPressed: _isExporting ? null : () => _exportPdf(order),
            style: ElevatedButton.styleFrom(backgroundColor: colors.primary, foregroundColor: Colors.white, shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(26))),
            icon: _isExporting
                ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                : const Icon(Icons.download_rounded, size: 18),
            label: const Text('Télécharger le reçu (PDF)', style: TextStyle(fontWeight: FontWeight.w600)),
          ),
        ),
        if (canTrack) ...[
          const SizedBox(height: 10),
          SizedBox(
            width: double.infinity,
            height: 50,
            child: OutlinedButton(
              onPressed: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => OrderTrackingScreen(orderId: order.id))),
              style: OutlinedButton.styleFrom(foregroundColor: colors.primary, side: BorderSide(color: colors.primary), shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(26))),
              child: const Text('Suivre ma commande', style: TextStyle(fontWeight: FontWeight.w600)),
            ),
          ),
        ],
      ],
    );
  }

  Widget _buildItemRow(AppColorScheme colors, OrderItemModel item) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(item.productName, style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark)),
                Text('${item.variantLabel} · x${item.quantity} · ${formatFcfa(item.unitPrice)}', style: TextStyle(fontSize: 12, color: colors.textMuted)),
              ],
            ),
          ),
          Text(formatFcfa(item.lineTotal), style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark)),
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
