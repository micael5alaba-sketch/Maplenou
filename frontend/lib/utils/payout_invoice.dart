import 'dart:typed_data';

import 'package:pdf/pdf.dart';
import 'package:pdf/widgets.dart' as pw;

import '../models/wallet_model.dart';
import 'formatters.dart';

/// Génère le PDF de facture d'un reversement vendeur — partagé entre
/// [WalletScreen] (facture individuelle) et l'écran "historique complet"
/// des retraits, pour ne pas dupliquer la mise en page.
Future<Uint8List> buildPayoutInvoicePdf(PayoutDetailModel detail) async {
  final doc = pw.Document();
  final grossTotal = detail.amount + detail.commissionTotal;

  doc.addPage(
    pw.Page(
      pageFormat: PdfPageFormat.a4,
      build: (context) {
        return pw.Column(
          crossAxisAlignment: pw.CrossAxisAlignment.start,
          children: [
            pw.Text('Maplenou', style: pw.TextStyle(fontSize: 22, fontWeight: pw.FontWeight.bold)),
            pw.Text('Facture de reversement', style: const pw.TextStyle(fontSize: 14)),
            pw.SizedBox(height: 16),
            pw.Text('Boutique : ${detail.shopName}'),
            pw.Text('Période : ${_formatDate(detail.periodStart)} — ${_formatDate(detail.periodEnd)}'),
            pw.Text('Statut : ${detail.status}'),
            if (detail.paidAt != null) pw.Text('Payé le : ${_formatDate(detail.paidAt!)}'),
            pw.SizedBox(height: 20),
            pw.Table(
              border: pw.TableBorder.all(color: PdfColors.grey300),
              columnWidths: const {
                0: pw.FlexColumnWidth(3),
                1: pw.FlexColumnWidth(1.3),
                2: pw.FlexColumnWidth(1.3),
                3: pw.FlexColumnWidth(1.3),
              },
              children: [
                pw.TableRow(
                  decoration: const pw.BoxDecoration(color: PdfColors.grey200),
                  children: [
                    _cell('Commande', bold: true),
                    _cell('Sous-total', bold: true),
                    _cell('Commission', bold: true),
                    _cell('Net', bold: true),
                  ],
                ),
                for (final line in detail.subOrders)
                  pw.TableRow(children: [
                    _cell('${line.itemsSummary} (${_formatDate(line.createdAt)})'),
                    _cell(formatFcfa(line.subtotal)),
                    _cell('- ${formatFcfa(line.commissionAmount)}'),
                    _cell(formatFcfa(line.netAmount)),
                  ]),
              ],
            ),
            pw.SizedBox(height: 20),
            pw.Divider(),
            _totalRow('Total brut', formatFcfa(grossTotal)),
            _totalRow('Commission Maplenou', '- ${formatFcfa(detail.commissionTotal)}'),
            pw.SizedBox(height: 4),
            _totalRow('Montant reversé', formatFcfa(detail.amount), bold: true),
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
    child: pw.Text(text, style: pw.TextStyle(fontSize: 9, fontWeight: bold ? pw.FontWeight.bold : null)),
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
  return '${date.day.toString().padLeft(2, '0')}/${date.month.toString().padLeft(2, '0')}/${date.year}';
}
