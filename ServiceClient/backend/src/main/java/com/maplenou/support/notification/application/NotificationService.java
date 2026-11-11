package com.maplenou.support.notification.application;

import com.maplenou.support.identity.application.UserDirectory;
import com.maplenou.support.notification.application.port.DeviceTokenStore;
import com.maplenou.support.notification.application.port.NotificationStore;
import com.maplenou.support.notification.domain.DeviceToken;
import com.maplenou.support.notification.domain.MessageTemplate;
import com.maplenou.support.notification.domain.Notification;
import com.maplenou.support.notification.domain.NotificationType;
import com.maplenou.support.shared.Actor;
import com.maplenou.support.shared.PageQuery;
import com.maplenou.support.shared.PageResult;
import com.maplenou.support.shared.SupportException;
import com.maplenou.support.ticket.application.VocabularyService;
import com.maplenou.support.ticket.domain.event.TicketEvent;
import com.maplenou.support.ticket.domain.vocabulary.StatusDefinition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Turns ticket events into in-app notifications, written in each recipient's language (DEC-CS11-09). Called by the
 * outbox relay inside its delivery transaction: notifications and the "delivered" mark are committed together, so a
 * crash never loses or duplicates them; a ticket change can never be undone by a notification problem (BR-020).
 */
@Service
public class NotificationService {

    private final NotificationStore notifications;
    private final DeviceTokenStore deviceTokens;
    private final UserDirectory directory;
    private final TemplateService templates;
    private final VocabularyService vocabulary;
    private final Clock clock;

    public NotificationService(NotificationStore notifications, DeviceTokenStore deviceTokens, UserDirectory directory,
                               TemplateService templates, VocabularyService vocabulary, Clock clock) {
        this.notifications = notifications;
        this.deviceTokens = deviceTokens;
        this.directory = directory;
        this.templates = templates;
        this.vocabulary = vocabulary;
        this.clock = clock;
    }

    @Transactional
    public void onTicketEvent(TicketEvent e) {
        switch (e.type()) {
            case TICKET_CREATED -> {
                // Confirmation to the requester, even though they are the actor.
                save(e.requesterId(), e, NotificationType.TICKET_CREATED);
                directory.activeManagers().forEach(m -> notifyUnlessActor(m.id(), e, NotificationType.NEW_TICKET_IN_QUEUE));
            }
            case TICKET_ASSIGNED -> notifyUnlessActor(e.assignedAgentId(), e, NotificationType.TICKET_ASSIGNED);
            case AGENT_REPLIED -> notifyUnlessActor(e.requesterId(), e, NotificationType.AGENT_REPLIED);
            case REQUESTER_REPLIED -> supportRecipients(e).forEach(r -> notifyUnlessActor(r, e, NotificationType.REQUESTER_REPLIED));
            case STATUS_CHANGED -> notifyUnlessActor(e.requesterId(), e, statusType(e));
            case REOPEN_REQUESTED -> supportRecipients(e).forEach(r -> notifyUnlessActor(r, e, NotificationType.REOPEN_REQUESTED));
            case REOPEN_DECIDED -> notifyUnlessActor(e.requesterId(), e, NotificationType.REOPEN_DECIDED);
            case TICKET_ESCALATED -> directory.activeManagers()
                    .forEach(m -> notifyUnlessActor(m.id(), e, NotificationType.TICKET_ESCALATED));
        }
    }

    private static NotificationType statusType(TicketEvent e) {
        if (e.statusKind() == null) {
            return NotificationType.STATUS_CHANGED;
        }
        return switch (e.statusKind()) {
            case WAITING_REQUESTER -> NotificationType.INFORMATION_REQUESTED;
            case RESOLVED -> NotificationType.TICKET_RESOLVED;
            case CLOSED -> NotificationType.TICKET_CLOSED;
            default -> NotificationType.STATUS_CHANGED;
        };
    }

    private Set<UUID> supportRecipients(TicketEvent e) {
        Set<UUID> recipients = new LinkedHashSet<>();
        if (e.assignedAgentId() != null) {
            recipients.add(e.assignedAgentId());
        } else {
            directory.activeManagers().forEach(m -> recipients.add(m.id()));
        }
        return recipients;
    }

    /** Users are not notified of their own actions. */
    private void notifyUnlessActor(UUID recipient, TicketEvent e, NotificationType type) {
        if (recipient != null && !recipient.equals(e.actorId())) {
            save(recipient, e, type);
        }
    }

    private void save(UUID recipient, TicketEvent e, NotificationType type) {
        String language = directory.language(recipient);
        MessageTemplate template = templates.find(type, language)
                .orElseThrow(() -> new IllegalStateException("No notification template for " + type));
        Map<String, String> values = Map.of(
                "ticket", e.ticketNumber() == null ? "" : e.ticketNumber(),
                "subject", e.subject() == null ? "" : e.subject(),
                "status", statusLabel(e.statusCode(), language),
                "decision", decisionLabel(e.detail(), language));
        notifications.save(Notification.create(UUID.randomUUID(), recipient, e.ticketId(), type,
                template.renderTitle(values), template.renderBody(values), e.ticketNumber(), detailOf(type, e),
                clock.instant()));
    }

    private String statusLabel(String code, String language) {
        if (code == null) {
            return "";
        }
        return vocabulary.workflow().find(code).map((StatusDefinition s) -> s.label(language)).orElse(code);
    }

    private static String decisionLabel(String detail, String language) {
        boolean approved = "APPROVED".equals(detail);
        return "en".equals(language) ? (approved ? "accepted" : "rejected") : (approved ? "acceptée" : "refusée");
    }

    /** The variable part a client needs to phrase the notification in another language. */
    private static String detailOf(NotificationType type, TicketEvent e) {
        return switch (type) {
            case STATUS_CHANGED -> e.statusCode();
            case REOPEN_DECIDED -> e.detail();
            default -> null;
        };
    }

    // ------------------------------------------------------------------ user-facing API

    @Transactional(readOnly = true)
    public PageResult<NotificationView> list(Actor user, boolean unreadOnly, PageQuery page) {
        return notifications.forRecipient(user.id(), unreadOnly, page).map(NotificationView::of);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Actor user) {
        return notifications.countUnread(user.id());
    }

    @Transactional
    public void markRead(Actor user, UUID id) {
        Notification n = notifications.find(id)
                .filter(x -> x.recipientId().equals(user.id()))
                .orElseThrow(() -> SupportException.notFound("Notification"));
        n.markRead(clock.instant());
        notifications.save(n);
    }

    @Transactional
    public int markAllRead(Actor user) {
        return notifications.markAllRead(user.id(), clock.instant());
    }

    @Transactional
    public void registerDevice(Actor user, String token, DeviceToken.Platform platform) {
        Instant now = clock.instant();
        deviceTokens.findByToken(token).ifPresentOrElse(
                t -> deviceTokens.save(t.reassign(user.id(), platform, now)),
                () -> deviceTokens.save(new DeviceToken(UUID.randomUUID(), user.id(), token, platform, now, now)));
    }

    @Transactional
    public void unregisterDevice(Actor user, String token) {
        deviceTokens.findByToken(token)
                .filter(t -> t.userId().equals(user.id()))
                .ifPresent(t -> deviceTokens.deleteByToken(t.token()));
    }

    /**
     * {@code title}/{@code body} are in the recipient's language. {@code type}, {@code ticketNumber} and
     * {@code detail} (status code for STATUS_CHANGED, APPROVED/REJECTED for REOPEN_DECIDED) let a client render the
     * message in another language.
     */
    public record NotificationView(UUID id, UUID ticketId, NotificationType type, String title, String body,
                                   String ticketNumber, String detail, Instant createdAt, Instant readAt) {
        static NotificationView of(Notification n) {
            return new NotificationView(n.id(), n.ticketId(), n.type(), n.title(), n.body(), n.ticketNumber(),
                    n.detail(), n.createdAt(), n.readAt());
        }
    }
}
