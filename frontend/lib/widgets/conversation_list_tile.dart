import 'package:flutter/material.dart';

import '../models/conversation_model.dart';
import '../theme/app_color_scheme.dart';
import '../utils/formatters.dart';

/// One row in [SellerMessagesScreen]'s conversation list: avatar, name,
/// last message preview, relative time, and an unread badge.
class ConversationListTile extends StatelessWidget {
  final ConversationPreviewModel conversation;
  final VoidCallback onTap;

  const ConversationListTile({super.key, required this.conversation, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;
    final hasUnread = conversation.unreadCount > 0;

    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(16),
      child: Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: colors.surface,
          borderRadius: BorderRadius.circular(16),
          boxShadow: [
            BoxShadow(color: Colors.black.withValues(alpha: 0.04), blurRadius: 10, offset: const Offset(0, 3)),
          ],
        ),
        child: Row(
          children: [
            CircleAvatar(
              radius: 22,
              backgroundColor: colors.inputFill,
              backgroundImage: conversation.participantAvatarUrl != null
                  ? NetworkImage(conversation.participantAvatarUrl!)
                  : null,
              child: conversation.participantAvatarUrl == null
                  ? Text(
                      conversation.participantName.isNotEmpty ? conversation.participantName[0].toUpperCase() : '?',
                      style: TextStyle(fontWeight: FontWeight.bold, color: colors.primary),
                    )
                  : null,
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    conversation.participantName,
                    style: TextStyle(fontWeight: FontWeight.w600, color: colors.textDark),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    conversation.lastMessage,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(
                      fontSize: 13,
                      color: hasUnread ? colors.textDark : colors.textMuted,
                      fontWeight: hasUnread ? FontWeight.w600 : FontWeight.normal,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(width: 8),
            Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Text(formatRelativeDate(conversation.lastMessageAt), style: TextStyle(fontSize: 11, color: colors.textMuted)),
                if (hasUnread) ...[
                  const SizedBox(height: 6),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
                    decoration: BoxDecoration(color: colors.primary, borderRadius: BorderRadius.circular(20)),
                    child: Text(
                      '${conversation.unreadCount}',
                      style: const TextStyle(color: Colors.white, fontSize: 11, fontWeight: FontWeight.w600),
                    ),
                  ),
                ],
              ],
            ),
          ],
        ),
      ),
    );
  }
}
