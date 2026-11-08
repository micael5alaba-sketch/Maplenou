package com.maplenou.support.ticket.application;

import com.maplenou.support.context.application.EcosystemService;
import com.maplenou.support.context.application.port.EcosystemActions;
import com.maplenou.support.context.domain.Ecosystem;
import com.maplenou.support.shared.Actor;
import com.maplenou.support.shared.SupportException;
import com.maplenou.support.ticket.application.port.ConversationStore;
import com.maplenou.support.ticket.application.port.ExternalActionStore;
import com.maplenou.support.ticket.application.port.TicketStore;
import com.maplenou.support.ticket.domain.ContextRefs;
import com.maplenou.support.ticket.domain.ExternalAction;
import com.maplenou.support.ticket.domain.HistoryAction;
import com.maplenou.support.ticket.domain.MessageVisibility;
import com.maplenou.support.ticket.domain.Ticket;
import com.maplenou.support.ticket.domain.TicketAction;
import com.maplenou.support.ticket.domain.TicketMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static com.maplenou.support.shared.Text.truncate;

/**
 * Actions on Maplenou started from a ticket (DEC-CS11-07): refund/return request, delivery released for a new
 * courier, message to the courier. Maplenou applies its own rules and audit; here every attempt — accepted or
 * refused — is kept on the ticket (internal note + history + {@code ticket_external_actions}).
 */
@Service
public class ExternalActionService {

    private static final Set<String> CONDITIONS = Set.of("NEW", "GOOD", "DAMAGED");

    private final TicketCommandService commands;
    private final TicketStore tickets;
    private final ConversationStore conversation;
    private final ExternalActionStore actions;
    private final VocabularyService vocabulary;
    private final EcosystemService ecosystem;
    private final Clock clock;

    public ExternalActionService(TicketCommandService commands, TicketStore tickets, ConversationStore conversation,
                                 ExternalActionStore actions, VocabularyService vocabulary, EcosystemService ecosystem,
                                 Clock clock) {
        this.commands = commands;
        this.tickets = tickets;
        this.conversation = conversation;
        this.actions = actions;
        this.vocabulary = vocabulary;
        this.ecosystem = ecosystem;
        this.clock = clock;
    }

    @Transactional(noRollbackFor = SupportException.class)
    public ExternalAction requestRefund(Actor user, UUID ticketId, TicketCommands.RequestRefund req) {
        Ticket t = commands.lock(ticketId);
        vocabulary.policy().require(user, t, commands.facts(t), TicketAction.REQUEST_REFUND);
        String order = t.contextRefs().get(ContextRefs.ORDER);
        if (order == null) {
            throw SupportException.businessRule("NO_ORDER_LINKED", "Aucune commande n'est liée à ce ticket.", "BR-019");
        }
        String condition = req.productCondition() == null ? "GOOD" : req.productCondition();
        if (!CONDITIONS.contains(condition)) {
            throw SupportException.badRequest("INVALID_PRODUCT_CONDITION", "État du produit : NEW, GOOD ou DAMAGED.", null);
        }
        String reason = req.reason().trim() + " (ticket " + t.ticketNumber() + ")";
        return run(user, t, ExternalAction.Type.REFUND_REQUEST, order,
                ctx -> ecosystem.requestReturn(order, req.subOrderId(), reason, condition, ctx),
                "Demande de retour/remboursement envoyée à Maplenou pour la commande " + order + ".");
    }

    @Transactional(noRollbackFor = SupportException.class)
    public ExternalAction rescheduleDelivery(Actor user, UUID ticketId, String reason) {
        Ticket t = commands.lock(ticketId);
        vocabulary.policy().require(user, t, commands.facts(t), TicketAction.RESCHEDULE_DELIVERY);
        String delivery = t.contextRefs().get(ContextRefs.DELIVERY);
        if (delivery == null) {
            throw SupportException.businessRule("NO_DELIVERY_LINKED", "Aucune livraison n'est liée à ce ticket.", "BR-019");
        }
        return run(user, t, ExternalAction.Type.DELIVERY_RESCHEDULE, delivery,
                ctx -> ecosystem.rescheduleDelivery(delivery, reason.trim(), ctx),
                "Livraison " + delivery + " remise en attente d'un livreur : " + reason.trim());
    }

    @Transactional(noRollbackFor = SupportException.class)
    public ExternalAction contactDriver(Actor user, UUID ticketId, String message) {
        Ticket t = commands.lock(ticketId);
        vocabulary.policy().require(user, t, commands.facts(t), TicketAction.CONTACT_DRIVER);
        String delivery = t.contextRefs().get(ContextRefs.DELIVERY);
        Ecosystem.DeliverySummary d = delivery == null ? null : ecosystem.delivery(delivery).data();
        if (d == null || d.driverId() == null) {
            throw SupportException.businessRule("NO_DRIVER", "Aucun livreur n'est connu pour la livraison de ce ticket.",
                    "BR-019");
        }
        return run(user, t, ExternalAction.Type.DRIVER_MESSAGE, d.driverId().toString(),
                ctx -> ecosystem.messageDriver(d.driverId(), message.trim(), ctx),
                "Message envoyé au livreur " + (d.driverName() == null ? "" : d.driverName()) + " : " + message.trim());
    }

    private interface Call {
        EcosystemActions.ActionResult call(EcosystemActions.ActionContext ctx);
    }

    /** Calls Maplenou, then records the outcome on the ticket whatever it is; a refusal is reported to the caller. */
    private ExternalAction run(Actor user, Ticket t, ExternalAction.Type type, String target, Call call, String note) {
        Instant now = clock.instant();
        EcosystemActions.ActionContext ctx = new EcosystemActions.ActionContext(t.ticketNumber(), user.displayName(),
                user.id());
        ExternalAction action;
        SupportException failure = null;
        try {
            EcosystemActions.ActionResult result = call.call(ctx);
            action = new ExternalAction(UUID.randomUUID(), t.id(), type, target, true, result.ref(), null, user.id(), now);
            conversation.save(new TicketMessage(UUID.randomUUID(), t.id(), user.id(), user.role(),
                    MessageVisibility.INTERNAL, note + (result.ref() == null ? "" : " Référence : " + result.ref() + "."),
                    null, now));
        } catch (SupportException e) {
            failure = e;
            action = new ExternalAction(UUID.randomUUID(), t.id(), type, target, false, null, truncate(e.getMessage(), 480),
                    user.id(), now);
        }
        actions.save(action);
        commands.record(t, user, HistoryAction.EXTERNAL_ACTION, type.name(), action.succeeded() ? "OK" : "REFUSED",
                truncate(action.succeeded() ? note : action.error(), 500), false, now);
        t.touch(now);
        tickets.save(t);
        if (failure != null) {
            throw failure;
        }
        return action;
    }
}
