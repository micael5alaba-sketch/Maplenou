package com.maplenou.support.notification.adapter.in.events;

import com.maplenou.support.notification.application.NotificationService;
import com.maplenou.support.ticket.domain.event.TicketEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Receives ticket events from the outbox relay, inside the relay's delivery transaction (notifications and the
 * "delivered" mark are committed together).
 */
@Component
public class TicketEventListener {

    private final NotificationService notifications;

    public TicketEventListener(NotificationService notifications) {
        this.notifications = notifications;
    }

    @EventListener
    public void on(TicketEvent event) {
        notifications.onTicketEvent(event);
    }
}
