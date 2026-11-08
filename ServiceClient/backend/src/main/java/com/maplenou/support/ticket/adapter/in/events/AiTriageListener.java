package com.maplenou.support.ticket.adapter.in.events;

import com.maplenou.support.ticket.application.AiAssistService;
import com.maplenou.support.ticket.domain.event.TicketEvent;
import com.maplenou.support.ticket.domain.event.TicketEventType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * When a ticket is created, asks the assistant for a category/priority suggestion — in the background, after the
 * outbox delivery is committed, so a slow or absent assistant never delays anything (DEC-CS11-08).
 */
@Component
public class AiTriageListener {

    private final AiAssistService ai;

    public AiTriageListener(AiAssistService ai) {
        this.ai = ai;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTicketEvent(TicketEvent event) {
        if (event.type() == TicketEventType.TICKET_CREATED && ai.available()) {
            ai.suggest(event.ticketId());
        }
    }
}
