import 'package:flutter/material.dart';

import '../services/unread_messages_service.dart';

/// Pose une pastille rouge (avec le nombre de messages non lus) sur le coin de [child], tant qu'il
/// y a des messages non lus. Sans message non lu, affiche [child] seul.
class UnreadDot extends StatelessWidget {
  final Widget child;

  const UnreadDot({super.key, required this.child});

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: UnreadMessagesService.instance,
      builder: (context, _) {
        final count = UnreadMessagesService.instance.count;
        if (count <= 0) return child;
        return Stack(
          clipBehavior: Clip.none,
          children: [
            child,
            Positioned(
              right: -8,
              top: -6,
              child: Container(
                constraints: const BoxConstraints(minWidth: 16, minHeight: 16),
                padding: const EdgeInsets.symmetric(horizontal: 4),
                decoration: const BoxDecoration(color: Colors.red, shape: BoxShape.rectangle, borderRadius: BorderRadius.all(Radius.circular(8))),
                alignment: Alignment.center,
                child: Text(
                  count > 99 ? '99+' : '$count',
                  style: const TextStyle(color: Colors.white, fontSize: 10, fontWeight: FontWeight.bold, height: 1),
                ),
              ),
            ),
          ],
        );
      },
    );
  }
}
