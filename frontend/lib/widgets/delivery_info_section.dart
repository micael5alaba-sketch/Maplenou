import 'package:flutter/material.dart';

import '../models/delivery_info_model.dart';
import '../theme/app_color_scheme.dart';
import '../utils/formatters.dart';

/// "Livraison et retours" block: standard/express delays, fee and return
/// policy, each with an icon.
class DeliveryInfoSection extends StatelessWidget {
  final DeliveryInfoModel info;

  const DeliveryInfoSection({super.key, required this.info});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        _buildRow(context, Icons.local_shipping_outlined, 'Livraison standard', info.standardDelay),
        _buildRow(context, Icons.bolt_rounded, 'Livraison express', info.expressDelay),
        _buildRow(context, Icons.payments_outlined, 'Frais de livraison', formatFcfa(info.deliveryFee)),
        _buildRow(context, Icons.replay_rounded, 'Retours', info.returnPolicy),
      ],
    );
  }

  Widget _buildRow(BuildContext context, IconData icon, String title, String value) {
    final colors = context.colors;

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, color: colors.primary, size: 22),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark)),
                const SizedBox(height: 2),
                Text(value, style: TextStyle(color: colors.textMuted, fontSize: 13)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
