import 'dart:async';

import 'package:flutter/material.dart';

import '../navigation.dart';
import '../services/notification_router.dart';

/// Pop-up de notification affiché EN HAUT de l'écran quand l'app est ouverte (au premier plan,
/// le système n'affiche rien tout seul) : glisse depuis le haut, disparaît après quelques
/// secondes, et un tap ouvre directement l'écran concerné (discussion, commande, livraison...).
class InAppNotificationBanner {
  static OverlayEntry? _entry;
  static Timer? _timer;

  static void show({required String title, required String body, required Map<String, dynamic> data}) {
    final overlay = navigatorKey.currentState?.overlay;
    if (overlay == null) return;
    dismiss();

    final entry = OverlayEntry(
      builder: (context) => _BannerView(
        title: title,
        body: body,
        onTap: () {
          dismiss();
          openNotification(data);
        },
        onClose: dismiss,
      ),
    );
    overlay.insert(entry);
    _entry = entry;
    _timer = Timer(const Duration(seconds: 6), dismiss);
  }

  static void dismiss() {
    _timer?.cancel();
    _timer = null;
    _entry?.remove();
    _entry = null;
  }
}

class _BannerView extends StatelessWidget {
  final String title;
  final String body;
  final VoidCallback onTap;
  final VoidCallback onClose;

  const _BannerView({required this.title, required this.body, required this.onTap, required this.onClose});

  @override
  Widget build(BuildContext context) {
    return Positioned(
      top: 0,
      left: 0,
      right: 0,
      child: SafeArea(
        child: TweenAnimationBuilder<double>(
          tween: Tween(begin: -80, end: 0),
          duration: const Duration(milliseconds: 250),
          curve: Curves.easeOut,
          builder: (context, offset, child) => Transform.translate(offset: Offset(0, offset), child: child),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
            child: Center(
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 520),
                child: Material(
                  elevation: 8,
                  borderRadius: BorderRadius.circular(16),
                  color: Theme.of(context).colorScheme.surface,
                  child: InkWell(
                    borderRadius: BorderRadius.circular(16),
                    onTap: onTap,
                    child: Padding(
                      padding: const EdgeInsets.fromLTRB(14, 12, 6, 12),
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Icon(Icons.notifications_active_rounded, color: Theme.of(context).colorScheme.primary),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(title,
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                    style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 14)),
                                if (body.isNotEmpty) ...[
                                  const SizedBox(height: 2),
                                  Text(body, maxLines: 2, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 13)),
                                ],
                              ],
                            ),
                          ),
                          IconButton(
                            visualDensity: VisualDensity.compact,
                            icon: const Icon(Icons.close_rounded, size: 18),
                            onPressed: onClose,
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
