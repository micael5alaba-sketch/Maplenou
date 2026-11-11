import 'package:flutter/material.dart';

import '../services/courier_presence_service.dart';
import '../theme/app_color_scheme.dart';

/// Remplace l'ancien bouton « En ligne / Hors ligne » : le livreur n'a rien à activer, la
/// localisation est envoyée en permanence. Ce bandeau confirme que c'est bien le cas, ou demande
/// d'activer la localisation si elle est refusée (sans elle, aucune course ne peut être affectée
/// automatiquement au livreur).
class LocationStatusBanner extends StatelessWidget {
  const LocationStatusBanner({super.key});

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return ListenableBuilder(
      listenable: CourierPresenceService.instance,
      builder: (context, _) {
        final denied = CourierPresenceService.instance.status == PresenceStatus.denied;
        final active = CourierPresenceService.instance.status == PresenceStatus.active;
        final accent = denied ? Colors.red : colors.primary;

        return Container(
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            color: accent.withValues(alpha: 0.1),
            borderRadius: BorderRadius.circular(14),
          ),
          child: Row(
            children: [
              Icon(denied ? Icons.location_off_rounded : Icons.my_location_rounded, color: accent),
              const SizedBox(width: 10),
              Expanded(
                child: Text(
                  denied
                      ? 'Localisation désactivée : activez-la pour recevoir les courses proches de vous.'
                      : active
                          ? 'Localisation active : les courses proches de vous vous sont affectées automatiquement.'
                          : 'Activation de la localisation...',
                  style: TextStyle(color: colors.textDark, fontSize: 13),
                ),
              ),
              if (denied)
                TextButton(
                  onPressed: CourierPresenceService.instance.retry,
                  child: const Text('Activer'),
                ),
            ],
          ),
        );
      },
    );
  }
}
