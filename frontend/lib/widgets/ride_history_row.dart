import 'package:flutter/material.dart';

import '../models/courier_history_model.dart';
import '../theme/app_color_scheme.dart';
import '../utils/formatters.dart';

/// One row in [CourierHistoryScreen]'s "Courses" tab: date, amount,
/// départ/arrivée, and a cancelled-state variant.
class RideHistoryRow extends StatelessWidget {
  final RideHistoryItem ride;

  const RideHistoryRow({super.key, required this.ride});

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    final statusColor = ride.cancelled ? colors.textMuted : colors.primary;

    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 10, offset: const Offset(0, 3)),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(
                children: [
                  Icon(
                    ride.cancelled ? Icons.cancel_outlined : Icons.check_circle_rounded,
                    size: 18,
                    color: statusColor,
                  ),
                  const SizedBox(width: 8),
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(formatOrderDateTime(ride.dateTime), style: TextStyle(fontSize: 12, color: colors.textMuted)),
                      Text(
                        ride.cancelled ? 'Annulé' : 'Terminé',
                        style: TextStyle(fontWeight: FontWeight.w600, color: statusColor, fontSize: 12),
                      ),
                    ],
                  ),
                ],
              ),
              if (!ride.cancelled)
                Text(formatFcfa(ride.amount), style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: colors.textDark)),
            ],
          ),
          const SizedBox(height: 10),
          _buildAddressRow(colors, Icons.trip_origin, 'Départ', ride.fromLabel),
          const SizedBox(height: 4),
          _buildAddressRow(colors, Icons.location_on, 'Arrivée', ride.toLabel),
          if (ride.cancelled && ride.cancelReason != null) ...[
            const SizedBox(height: 8),
            Text(ride.cancelReason!, style: TextStyle(fontSize: 12, color: colors.textMuted, fontStyle: FontStyle.italic)),
          ],
        ],
      ),
    );
  }

  Widget _buildAddressRow(AppColorScheme colors, IconData icon, String label, String value) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(icon, size: 14, color: colors.textMuted),
        const SizedBox(width: 8),
        Expanded(
          child: RichText(
            text: TextSpan(
              children: [
                TextSpan(text: '$label  ', style: TextStyle(fontSize: 11, color: colors.textMuted)),
                TextSpan(text: value, style: TextStyle(fontSize: 12, color: colors.textDark, fontWeight: FontWeight.w500)),
              ],
            ),
          ),
        ),
      ],
    );
  }
}
