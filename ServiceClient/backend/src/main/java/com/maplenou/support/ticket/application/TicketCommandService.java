package com.maplenou.support.ticket.application;

import com.maplenou.support.attachment.application.AttachmentService;
import com.maplenou.support.attachment.domain.AttachmentPolicy.ValidatedFile;
import com.maplenou.support.context.application.ReferenceService;
import com.maplenou.support.identity.application.UserDirectory;
import com.maplenou.support.identity.application.UserDirectory.UserSummary;
import com.maplenou.support.shared.Actor;
import com.maplenou.support.shared.SupportException;
import com.maplenou.support.shared.event.DomainEventPublisher;
import com.maplenou.support.sla.application.SlaPeriodService;
import com.maplenou.support.sla.application.SlaService;
import com.maplenou.support.ticket.application.port.ConversationStore;
import com.maplenou.support.ticket.application.port.FeedbackStore;
import com.maplenou.support.ticket.application.port.HistoryStore;
import com.maplenou.support.ticket.application.port.ReopenRequestStore;
import com.maplenou.support.ticket.application.port.TicketStore;
import com.maplenou.support.ticket.domain.AttachmentRecord;
import com.maplenou.support.ticket.domain.Category;
import com.maplenou.support.ticket.domain.ContextRefs;
import com.maplenou.support.ticket.domain.HistoryAction;
import com.maplenou.support.ticket.domain.HistoryEntry;
import com.maplenou.support.ticket.domain.MessageVisibility;
import com.maplenou.support.ticket.domain.ReopenRequest;
import com.maplenou.support.ticket.domain.Satisfaction;
import com.maplenou.support.ticket.domain.Ticket;
import com.maplenou.support.ticket.domain.TicketAccessPolicy;
import com.maplenou.support.ticket.domain.TicketAccessPolicy.TicketFacts;
import com.maplenou.support.ticket.domain.TicketAction;
import com.maplenou.support.ticket.domain.TicketMessage;
import com.maplenou.support.ticket.domain.TicketNumbers;
import com.maplenou.support.ticket.domain.event.TicketEvent;
import com.maplenou.support.ticket.domain.event.TicketEventType;
import com.maplenou.support.ticket.domain.vocabulary.PriorityDefinition;
import com.maplenou.support.ticket.domain.vocabulary.StatusKind;
import com.maplenou.support.ticket.domain.vocabulary.Workflow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static com.maplenou.support.shared.Text.blankToNull;
import static com.maplenou.support.shared.Text.truncate;

/**
 * Ticket commands. Every method runs in one transaction: either the whole change (ticket, message, attachment
 * metadata, history, SLA, outgoing event) is committed, or nothing is (BR-022). Notifications are produced from the
 * outgoing event after commit and cannot affect the ticket (BR-020).
 */
@Service
public class TicketCommandService {

    private final TicketStore tickets;
    private final ConversationStore conversation;
    private final HistoryStore history;
    private final ReopenRequestStore reopenRequests;
    private final FeedbackStore feedback;
    private final VocabularyService vocabulary;
    private final CategoryService categories;
    private final SlaService sla;
    private final SlaPeriodService slaPeriods;
    private final UserDirectory directory;
    private final ReferenceService references;
    private final AttachmentService attachments;
    private final DomainEventPublisher events;
    private final Clock clock;

    public TicketCommandService(TicketStore tickets, ConversationStore conversation, HistoryStore history,
                                ReopenRequestStore reopenRequests, FeedbackStore feedback,
                                VocabularyService vocabulary, CategoryService categories, SlaService sla,
                                SlaPeriodService slaPeriods, UserDirectory directory, ReferenceService references,
                                AttachmentService attachments, DomainEventPublisher events, Clock clock) {
        this.tickets = tickets;
        this.conversation = conversation;
        this.history = history;
        this.reopenRequests = reopenRequests;
        this.feedback = feedback;
        this.vocabulary = vocabulary;
        this.categories = categories;
        this.sla = sla;
        this.slaPeriods = slaPeriods;
        this.directory = directory;
        this.references = references;
        this.attachments = attachments;
        this.events = events;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ creation (FR-001, FR-002)

    @Transactional
    public Ticket create(Actor user, TicketCommands.CreateTicket req) {
        if (user.role() == null || !user.role().canCreateTickets()) {
            throw SupportException.forbidden("Seuls les clients, vendeurs et livreurs peuvent créer un ticket.", "BR-003");
        }
        PriorityDefinition priority = req.priority() == null || req.priority().isBlank()
                ? vocabulary.priorities().defaultPriority()
                : vocabulary.priorities().requireUsable(req.priority());
        if (!priority.requesterSelectable()) {
            throw SupportException.businessRule("CRITICAL_PRIORITY_RESERVED",
                    "La priorité " + priority.labelFr() + " est attribuée uniquement par le support. "
                            + "Indiquez plutôt que c'est urgent.", "BR-036");
        }
        Category category = categories.requireActive(req.categoryCode());
        ReferenceService.ResolvedReferences refs = references.resolveForCreation(user, req.orderRef(), req.deliveryRef());
        List<ValidatedFile> validFiles = attachments.validate(req.files(), 0);

        Instant now = clock.instant();
        UserSummary requester = directory.require(user.id());
        Workflow wf = vocabulary.workflow();
        String number = TicketNumbers.format(now.atZone(ZoneOffset.UTC).getYear(), tickets.nextNumber());
        boolean urgent = Boolean.TRUE.equals(req.urgent());
        Ticket t = Ticket.open(UUID.randomUUID(), number, req.subject().trim(), req.description().trim(),
                category.code(), priority, wf.system(StatusKind.NEW), requester.id(), user.role(), urgent,
                new ContextRefs(refs.refs()), now);
        t.setSlaTargets(sla.firstResponseDue(now, priority.code()), sla.resolutionDue(now, priority.code()));
        tickets.save(t);
        slaPeriods.open(t.id(), priority.code(), now, t.resolutionDueAt());

        TicketMessage first = new TicketMessage(UUID.randomUUID(), t.id(), user.id(), user.role(),
                MessageVisibility.PUBLIC, t.description(), null, now);
        conversation.save(first);
        storeAttachments(user, t, first, validFiles, now);

        record(t, user, HistoryAction.CREATED, null, t.statusCode(),
                "Priorité " + priority.code() + (urgent ? " — urgence signalée par le demandeur" : ""), true, now);
        publish(TicketEventType.TICKET_CREATED, t, user, null);
        return t;
    }

    // ------------------------------------------------------------------ messages & notes (FR-007, FR-008)

    @Transactional
    public TicketMessage postMessage(Actor user, UUID ticketId, TicketCommands.PostMessage req,
                                     MessageVisibility visibility) {
        Ticket t = lock(ticketId);
        TicketAccessPolicy policy = vocabulary.policy();
        policy.require(user, t, facts(t),
                visibility == MessageVisibility.INTERNAL ? TicketAction.ADD_INTERNAL_NOTE : TicketAction.SEND_MESSAGE);

        String clientId = blankToNull(req.clientMessageId());
        if (clientId != null) {
            var existing = conversation.findByClientMessageId(t.id(), user.id(), clientId);
            if (existing.isPresent()) {
                return existing.get(); // retry of an already-accepted message (AC-SB-006)
            }
        }
        List<ValidatedFile> validFiles = attachments.validate(req.files(), conversation.totalAttachmentBytes(t.id()));

        Instant now = clock.instant();
        TicketMessage m = new TicketMessage(UUID.randomUUID(), t.id(), user.id(), user.role(), visibility,
                req.body().trim(), clientId, now);
        conversation.save(m);
        storeAttachments(user, t, m, validFiles, now);
        t.touch(now);

        if (visibility == MessageVisibility.INTERNAL) {
            record(t, user, HistoryAction.INTERNAL_NOTE_ADDED, null, null, null, false, now);
            tickets.save(t);
            return m;
        }
        record(t, user, HistoryAction.MESSAGE_ADDED, null, null, null, false, now);
        Workflow wf = vocabulary.workflow();
        boolean fromRequester = t.requesterId().equals(user.id()) && !user.role().isSupportStaff();
        if (fromRequester) {
            if (wf.kind(t.statusCode()) == StatusKind.WAITING_REQUESTER) {
                applyStatus(t, user, wf.system(StatusKind.IN_PROGRESS), "Réponse du demandeur", now);
            }
            publish(TicketEventType.REQUESTER_REPLIED, t, user, null);
        } else {
            t.markFirstResponse(now); // first qualifying support response (FR-016)
            if (wf.kind(t.statusCode()) == StatusKind.ASSIGNED) {
                applyStatus(t, user, wf.system(StatusKind.IN_PROGRESS), "Première réponse du support", now);
            }
            publish(TicketEventType.AGENT_REPLIED, t, user, null);
        }
        tickets.save(t);
        return m;
    }

    // ------------------------------------------------------------------ lifecycle (FR-005, FR-015)

    @Transactional
    public Ticket changeStatus(Actor user, UUID ticketId, TicketCommands.ChangeStatus req) {
        Ticket t = lock(ticketId);
        TicketFacts facts = facts(t);
        TicketAccessPolicy policy = vocabulary.policy();
        Workflow wf = vocabulary.workflow();
        policy.requireView(user, t);
        String target = wf.requireUsable(req.status()).code();
        StatusKind targetKind = wf.kind(target);
        if (targetKind == StatusKind.CLOSED && facts.pendingReopen()) {
            throw SupportException.conflict("REOPEN_PENDING",
                    "Une demande de réouverture est en attente : traitez-la avant de fermer le ticket.", "BR-050");
        }
        if (!wf.isManualTransitionAllowed(t.statusCode(), target)) {
            // Authorization is checked first so that unauthorized callers learn nothing about the state.
            if (!policy.hasOperationalAuthority(user, t)) {
                throw SupportException.forbidden("Vous n'êtes pas autorisé à modifier le statut de ce ticket.", "BR-071");
            }
            throw SupportException.businessRule("INVALID_TRANSITION",
                    "Transition impossible : " + wf.status(t.statusCode()).labelFr() + " → "
                            + wf.status(target).labelFr() + ".",
                    wf.kind(t.statusCode()) == StatusKind.RESOLVED && targetKind.isOpen() ? "BR-049" : "BR-010");
        }
        policy.require(user, t, facts, policy.actionFor(target));
        applyStatus(t, user, target, blankToNull(req.comment()), clock.instant());
        tickets.save(t);
        return t;
    }

    /**
     * Changes the status and keeps the SLA in step: the resolution period ends on resolution, and the resolution
     * clock stops/starts again when entering/leaving a status that pauses it (DEC-CS11-03).
     */
    void applyStatus(Ticket t, Actor user, String target, String details, Instant now) {
        Workflow wf = vocabulary.workflow();
        String previous = t.statusCode();
        boolean wasPaused = t.isSlaPaused();
        boolean pausesNow = wf.pausesSla(target);
        StatusKind kind = wf.kind(target);
        if (wasPaused && (!pausesNow || !kind.isOpen())) {
            SlaService.Extension ext = sla.extendAfterPause(t.resolutionDueAt(), t.slaPausedSince(), now);
            t.resumeResolutionClock(ext.newDue());
            slaPeriods.addPauseToCurrent(t.id(), ext.pausedMinutes(), ext.newDue());
            record(t, user, HistoryAction.SLA_RESUMED, null, ext.newDue().toString(),
                    "Délai de résolution prolongé de " + ext.pausedMinutes() + " min", false, now);
        }
        t.changeStatus(target, kind, now);
        if (!wasPaused && pausesNow && kind.isOpen()) {
            t.pauseResolutionClock(now);
            record(t, user, HistoryAction.SLA_PAUSED, null, null, "Délai de résolution suspendu", false, now);
        }
        if (kind == StatusKind.RESOLVED) {
            slaPeriods.endCurrent(t.id(), now, "RESOLVED");
        }
        record(t, user, HistoryAction.STATUS_CHANGED, previous, target, details, true, now);
        publish(TicketEventType.STATUS_CHANGED, t, user, null);
    }

    // ------------------------------------------------------------------ classification (FR-003, FR-004)

    @Transactional
    public Ticket changePriority(Actor user, UUID ticketId, TicketCommands.ChangePriority req) {
        Ticket t = lock(ticketId);
        PriorityDefinition priority = vocabulary.priorities().requireUsable(req.priority());
        vocabulary.policy().require(user, t, facts(t),
                priority.requesterSelectable() ? TicketAction.CHANGE_PRIORITY : TicketAction.SET_CRITICAL);
        String previous = t.priorityCode();
        if (previous.equals(priority.code())) {
            return t;
        }
        Instant now = clock.instant();
        t.changePriority(priority, now);
        // BR-034: recalculate the applicable SLA; history keeps the previous values.
        Instant oldFirst = t.firstResponseDueAt();
        Instant oldResolution = t.resolutionDueAt();
        Instant newFirst = t.firstRespondedAt() == null ? sla.firstResponseDue(t.createdAt(), priority.code()) : oldFirst;
        Instant newResolution = sla.resolutionDue(t.resolutionPeriodStartedAt(), priority.code());
        // time the clock was already stopped in this period still pushes the deadline back (DEC-CS11-03)
        long paused = slaPeriods.current(t.id()).map(p -> p.pausedMinutes()).orElse(0L);
        if (paused > 0) {
            newResolution = sla.calendar().plusBusinessMinutes(newResolution, paused);
        }
        t.setSlaTargets(newFirst, newResolution);
        slaPeriods.recalculateCurrent(t.id(), priority.code(), newResolution);
        record(t, user, HistoryAction.PRIORITY_CHANGED, previous, priority.code(), blankToNull(req.reason()), true, now);
        record(t, user, HistoryAction.SLA_RECALCULATED, oldFirst + " / " + oldResolution,
                newFirst + " / " + newResolution, "Première réponse / résolution", false, now);
        tickets.save(t);
        return t;
    }

    @Transactional
    public Ticket changeCategory(Actor user, UUID ticketId, String categoryCode) {
        Ticket t = lock(ticketId);
        vocabulary.policy().require(user, t, facts(t), TicketAction.CHANGE_CATEGORY);
        Category category = categories.requireActive(categoryCode);
        String previous = t.categoryCode();
        if (previous.equals(category.code())) {
            return t;
        }
        Instant now = clock.instant();
        t.changeCategory(category.code(), now);
        record(t, user, HistoryAction.CATEGORY_CHANGED, previous, category.code(), null, true, now);
        tickets.save(t);
        return t;
    }

    // ------------------------------------------------------------------ assignment (FR-006)

    @Transactional
    public Ticket assign(Actor user, UUID ticketId, TicketCommands.Assign req) {
        Ticket t = lock(ticketId);
        UUID targetId = req.agentId() == null ? user.id() : req.agentId();
        boolean self = targetId.equals(user.id());
        // Taking an unassigned ticket is self-assignment (BR-057); anything else is a reassignment (BR-058/059).
        boolean takeUnassigned = self && !user.role().isSupervisor() && t.assignedAgentId() == null;
        vocabulary.policy().require(user, t, facts(t), takeUnassigned ? TicketAction.ASSIGN_SELF : TicketAction.REASSIGN);
        UserSummary target = directory.require(targetId);
        if (!target.active() || target.role() == null || !target.role().isAssignable()) {
            throw SupportException.businessRule("INVALID_ASSIGNEE",
                    "Le ticket doit être attribué à un agent support actif.", "BR-013");
        }
        if (t.isAssignedTo(targetId)) {
            return t;
        }
        Instant now = clock.instant();
        String previousName = t.assignedAgentId() == null ? null
                : directory.find(t.assignedAgentId()).map(UserSummary::name).orElse(null);
        boolean wasAssigned = t.assignedAgentId() != null;
        t.assignTo(target.id(), now);
        record(t, user, wasAssigned ? HistoryAction.REASSIGNED : HistoryAction.ASSIGNED, previousName, target.name(),
                null, false, now);
        Workflow wf = vocabulary.workflow();
        if (wf.kind(t.statusCode()) == StatusKind.NEW) {
            applyStatus(t, user, wf.system(StatusKind.ASSIGNED), null, now);
        }
        publish(TicketEventType.TICKET_ASSIGNED, t, user, null);
        tickets.save(t);
        return t;
    }

    // ------------------------------------------------------------------ escalation (BR-064)

    @Transactional
    public Ticket escalate(Actor user, UUID ticketId, String reason) {
        Ticket t = lock(ticketId);
        vocabulary.policy().require(user, t, facts(t), TicketAction.ESCALATE);
        Instant now = clock.instant();
        t.escalate(now);
        record(t, user, HistoryAction.ESCALATED, null, null, reason.trim(), false, now);
        publish(TicketEventType.TICKET_ESCALATED, t, user, null);
        tickets.save(t);
        return t;
    }

    // ------------------------------------------------------------------ reopening (FR-017)

    @Transactional
    public ReopenRequest requestReopen(Actor user, UUID ticketId, String reason) {
        Ticket t = lock(ticketId);
        vocabulary.policy().requireView(user, t);
        if (!t.requesterId().equals(user.id())) {
            throw SupportException.forbidden("Seul le demandeur peut demander la réouverture de son ticket.", "BR-048");
        }
        StatusKind kind = vocabulary.workflow().kind(t.statusCode());
        if (kind == StatusKind.CLOSED) {
            throw SupportException.conflict("TICKET_CLOSED",
                    "Un ticket fermé ne peut pas être rouvert. Créez un nouveau ticket.", "BR-052");
        }
        if (kind != StatusKind.RESOLVED) {
            throw SupportException.conflict("TICKET_NOT_RESOLVED",
                    "La réouverture ne peut être demandée que pour un ticket résolu.", "BR-048");
        }
        if (reopenRequests.pending(t.id()).isPresent()) {
            throw SupportException.conflict("REOPEN_ALREADY_PENDING",
                    "Une demande de réouverture est déjà en cours d'examen.", "BR-048");
        }
        Instant now = clock.instant();
        ReopenRequest r = ReopenRequest.pending(UUID.randomUUID(), t.id(), user.id(), reason.trim(), now);
        reopenRequests.save(r);
        t.touch(now);
        record(t, user, HistoryAction.REOPEN_REQUESTED, null, null, reason.trim(), true, now);
        publish(TicketEventType.REOPEN_REQUESTED, t, user, null);
        tickets.save(t);
        return r;
    }

    @Transactional
    public Ticket decideReopen(Actor user, UUID ticketId, UUID requestId, TicketCommands.DecideReopen req) {
        Ticket t = lock(ticketId);
        vocabulary.policy().require(user, t, facts(t), TicketAction.DECIDE_REOPEN);
        ReopenRequest r = reopenRequests.find(requestId)
                .filter(x -> x.ticketId().equals(t.id()) && x.status() == ReopenRequest.Status.PENDING)
                .orElseThrow(() -> SupportException.notFound("Demande de réouverture"));
        Instant now = clock.instant();
        String note = blankToNull(req.note());
        if (req.decision() != TicketCommands.Decision.APPROVE) {
            r.decide(false, user.id(), note, now);
            reopenRequests.save(r);
            t.touch(now);
            record(t, user, HistoryAction.REOPEN_REJECTED, null, null, note, true, now);
            publish(TicketEventType.REOPEN_DECIDED, t, user, "REJECTED");
            tickets.save(t);
            return t;
        }
        Workflow wf = vocabulary.workflow();
        String target = req.targetStatus() == null || req.targetStatus().isBlank()
                ? wf.system(StatusKind.IN_PROGRESS) : req.targetStatus();
        if (!wf.reopenTargets().contains(target)
                || (wf.find(target).map(s -> s.kind() == StatusKind.ASSIGNED).orElse(false) && t.assignedAgentId() == null)) {
            throw SupportException.businessRule("INVALID_REOPEN_TARGET", "Statut de réouverture invalide.", "BR-051");
        }
        r.decide(true, user.id(), note, now);
        reopenRequests.save(r);
        record(t, user, HistoryAction.REOPEN_APPROVED, null, target, note, true, now);
        // BR-055: the original SLA history is kept; a new resolution period starts now.
        Instant due = sla.resolutionDue(now, t.priorityCode());
        t.startResolutionPeriod(now, due);
        applyStatus(t, user, target, "Réouverture approuvée", now);
        slaPeriods.open(t.id(), t.priorityCode(), now, t.resolutionDueAt());
        publish(TicketEventType.REOPEN_DECIDED, t, user, "APPROVED");
        tickets.save(t);
        return t;
    }

    // ------------------------------------------------------------------ satisfaction (DEC-CS11-05)

    @Transactional
    public Satisfaction rate(Actor user, UUID ticketId, TicketCommands.Rate req) {
        Ticket t = lock(ticketId);
        vocabulary.policy().require(user, t, facts(t), TicketAction.RATE);
        Instant now = clock.instant();
        Satisfaction s = new Satisfaction(t.id(), req.rating(), truncate(blankToNull(req.comment()), 1000), user.id(), now);
        feedback.save(s);
        record(t, user, HistoryAction.RATED, null, Integer.toString(req.rating()), null, true, now);
        return s;
    }

    // ------------------------------------------------------------------ automatic closure (DEC-CS11-04)

    /**
     * Closes one resolved ticket when it is still resolved, has no pending reopening request and was resolved before
     * {@code resolvedBefore}. Returns whether it was closed.
     */
    @Transactional
    public boolean autoClose(UUID ticketId, Instant resolvedBefore) {
        Ticket t = tickets.lockForUpdate(ticketId).orElse(null);
        if (t == null) {
            return false;
        }
        Workflow wf = vocabulary.workflow();
        if (wf.kind(t.statusCode()) != StatusKind.RESOLVED || t.resolvedAt() == null
                || !t.resolvedAt().isBefore(resolvedBefore) || reopenRequests.pending(t.id()).isPresent()) {
            return false;
        }
        Instant now = clock.instant();
        Actor system = Actor.system();
        String closed = wf.system(StatusKind.CLOSED);
        String previous = t.statusCode();
        t.changeStatus(closed, StatusKind.CLOSED, now);
        history.append(new HistoryEntry(UUID.randomUUID(), t.id(), null, null, HistoryAction.AUTO_CLOSED, previous,
                closed, "Fermé automatiquement sans demande de réouverture", true, now));
        publish(TicketEventType.STATUS_CHANGED, t, system, null);
        tickets.save(t);
        return true;
    }

    // ------------------------------------------------------------------ helpers

    Ticket lock(UUID ticketId) {
        return tickets.lockForUpdate(ticketId).orElseThrow(() -> SupportException.notFound("Ticket"));
    }

    TicketFacts facts(Ticket t) {
        return new TicketFacts(reopenRequests.pending(t.id()).isPresent(), feedback.find(t.id()).isPresent());
    }

    private void storeAttachments(Actor user, Ticket t, TicketMessage m, List<ValidatedFile> files, Instant now) {
        for (ValidatedFile f : files) {
            UUID id = UUID.randomUUID();
            AttachmentService.StoredFile stored = attachments.store("tickets/" + t.id() + "/" + id, f);
            conversation.save(new AttachmentRecord(id, t.id(), m.id(), user.id(), stored.filename(),
                    stored.contentType(), stored.size(), stored.key(), stored.sha256(), now));
            record(t, user, HistoryAction.ATTACHMENT_ADDED, null, f.filename(), null,
                    m.visibility() == MessageVisibility.PUBLIC, now);
        }
    }

    void record(Ticket t, Actor actor, HistoryAction action, String oldValue, String newValue, String details,
                boolean requesterVisible, Instant now) {
        UUID actorId = actor.isSystem() ? null : actor.id();
        history.append(new HistoryEntry(UUID.randomUUID(), t.id(), actorId, actor.role(), action,
                truncate(oldValue, 255), truncate(newValue, 255), truncate(details, 500), requesterVisible, now));
    }

    void publish(TicketEventType type, Ticket t, Actor actor, String detail) {
        events.publish(new TicketEvent(UUID.randomUUID(), type, t.id(), t.ticketNumber(), t.subject(), t.requesterId(),
                t.assignedAgentId(), actor.isSystem() ? null : actor.id(), t.statusCode(),
                vocabulary.workflow().kind(t.statusCode()), detail, clock.instant()));
    }
}
