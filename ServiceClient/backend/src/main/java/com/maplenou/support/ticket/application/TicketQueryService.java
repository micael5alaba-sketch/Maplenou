package com.maplenou.support.ticket.application;

import com.maplenou.support.attachment.application.AttachmentService;
import com.maplenou.support.attachment.application.port.BinaryStorage;
import com.maplenou.support.context.application.EcosystemService;
import com.maplenou.support.context.domain.Ecosystem;
import com.maplenou.support.identity.application.UserDirectory;
import com.maplenou.support.identity.application.UserDirectory.UserSummary;
import com.maplenou.support.shared.Actor;
import com.maplenou.support.shared.PageQuery;
import com.maplenou.support.shared.PageResult;
import com.maplenou.support.shared.Role;
import com.maplenou.support.shared.SupportException;
import com.maplenou.support.sla.application.SlaPeriodService;
import com.maplenou.support.sla.application.SlaService;
import com.maplenou.support.sla.domain.SlaState;
import com.maplenou.support.ticket.application.port.AiSuggestionStore;
import com.maplenou.support.ticket.application.port.ConversationStore;
import com.maplenou.support.ticket.application.port.ExternalActionStore;
import com.maplenou.support.ticket.application.port.FeedbackStore;
import com.maplenou.support.ticket.application.port.HistoryStore;
import com.maplenou.support.ticket.application.port.ReopenRequestStore;
import com.maplenou.support.ticket.application.port.TicketSearch;
import com.maplenou.support.ticket.application.port.TicketStore;
import com.maplenou.support.ticket.application.view.TicketViews.*;
import com.maplenou.support.ticket.domain.AttachmentRecord;
import com.maplenou.support.ticket.domain.ContextRefs;
import com.maplenou.support.ticket.domain.HistoryEntry;
import com.maplenou.support.ticket.domain.MessageVisibility;
import com.maplenou.support.ticket.domain.ReopenRequest;
import com.maplenou.support.ticket.domain.Ticket;
import com.maplenou.support.ticket.domain.TicketAccessPolicy;
import com.maplenou.support.ticket.domain.TicketAccessPolicy.TicketFacts;
import com.maplenou.support.ticket.domain.TicketAction;
import com.maplenou.support.ticket.domain.TicketMessage;
import com.maplenou.support.ticket.domain.vocabulary.StatusKind;
import com.maplenou.support.ticket.domain.vocabulary.Workflow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Read side of the ticket API; every read is filtered through the access policy. */
@Service
@Transactional(readOnly = true)
public class TicketQueryService {

    private static final Set<String> SORTABLE = Set.of("createdAt", "updatedAt", "priorityRank", "ticketNumber", "status");

    private final TicketStore tickets;
    private final TicketSearch search;
    private final ConversationStore conversation;
    private final HistoryStore history;
    private final ReopenRequestStore reopenRequests;
    private final FeedbackStore feedback;
    private final AiSuggestionStore aiSuggestions;
    private final ExternalActionStore externalActions;
    private final VocabularyService vocabulary;
    private final CategoryService categories;
    private final SlaService sla;
    private final SlaPeriodService slaPeriods;
    private final UserDirectory directory;
    private final AttachmentService attachments;
    private final EcosystemService ecosystem;

    public TicketQueryService(TicketStore tickets, TicketSearch search, ConversationStore conversation,
                              HistoryStore history, ReopenRequestStore reopenRequests, FeedbackStore feedback,
                              AiSuggestionStore aiSuggestions, ExternalActionStore externalActions,
                              VocabularyService vocabulary, CategoryService categories, SlaService sla,
                              SlaPeriodService slaPeriods, UserDirectory directory, AttachmentService attachments,
                              EcosystemService ecosystem) {
        this.tickets = tickets;
        this.search = search;
        this.conversation = conversation;
        this.history = history;
        this.reopenRequests = reopenRequests;
        this.feedback = feedback;
        this.aiSuggestions = aiSuggestions;
        this.externalActions = externalActions;
        this.vocabulary = vocabulary;
        this.categories = categories;
        this.sla = sla;
        this.slaPeriods = slaPeriods;
        this.directory = directory;
        this.attachments = attachments;
        this.ecosystem = ecosystem;
    }

    // ------------------------------------------------------------------ list / search (FR-014)

    public PageResult<TicketSummary> search(Actor user, TicketSearchCriteria criteria, PageQuery page, String sort) {
        requireKnownCodes(criteria);
        TicketSearchCriteria effective = restrict(user, criteria);
        PageResult<Ticket> result;
        try {
            result = search.search(effective, user.id(), page, parseSort(sort));
        } catch (IllegalArgumentException e) {
            throw SupportException.badRequest("INVALID_FILTER", "Filtre invalide.", null);
        }
        return summaries(result.items(), user, result);
    }

    /** Requesters only ever see their own tickets, whatever filters they send (BR-015, AC-003). */
    TicketSearchCriteria restrict(Actor user, TicketSearchCriteria criteria) {
        TicketSearchCriteria c = user.role().isSupportStaff() ? criteria : criteria.withRequester(user.id());
        return c.withContext(vocabulary.workflow().openCodes(), sla.calendar().zone());
    }

    private PageResult<TicketSummary> summaries(List<Ticket> list, Actor user, PageResult<Ticket> page) {
        Map<UUID, UserSummary> people = people(list);
        boolean staff = user.role().isSupportStaff();
        List<TicketSummary> items = list.stream().map(t -> summary(t, staff, people)).toList();
        return new PageResult<>(items, page.page(), page.size(), page.totalItems(), page.totalPages());
    }

    /** Tickets for the CSV export (support managers and administrators), at most {@code limit}. */
    public List<TicketSummary> export(Actor user, TicketSearchCriteria criteria, int limit) {
        if (!user.role().isSupervisor()) {
            throw SupportException.forbidden("L'export est réservé aux responsables et administrateurs.", "BR-065");
        }
        requireKnownCodes(criteria);
        List<Ticket> list = search.list(restrict(user, criteria), user.id(), limit);
        Map<UUID, UserSummary> people = people(list);
        return list.stream().map(t -> summary(t, true, people)).toList();
    }

    private Map<UUID, UserSummary> people(Collection<Ticket> list) {
        Set<UUID> ids = new HashSet<>();
        for (Ticket t : list) {
            ids.add(t.requesterId());
            if (t.assignedAgentId() != null) {
                ids.add(t.assignedAgentId());
            }
        }
        return directory.summaries(ids);
    }

    /** Filter values come from the configurable vocabulary: an unknown code is a client error, not "no result". */
    private void requireKnownCodes(TicketSearchCriteria c) {
        if (c.statuses() != null && c.statuses().stream().anyMatch(code -> vocabulary.workflow().find(code).isEmpty())) {
            throw SupportException.badRequest("INVALID_FILTER", "Statut inconnu dans le filtre.", null);
        }
        if (c.priorities() != null
                && c.priorities().stream().anyMatch(code -> vocabulary.priorities().find(code).isEmpty())) {
            throw SupportException.badRequest("INVALID_FILTER", "Priorité inconnue dans le filtre.", null);
        }
    }

    private static TicketSearch.Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return new TicketSearch.Sort("createdAt", false);
        }
        String[] parts = sort.split(",");
        String field = parts[0].trim();
        if (!SORTABLE.contains(field)) {
            throw SupportException.badRequest("INVALID_SORT", "Tri non supporté : " + field, null);
        }
        return new TicketSearch.Sort(field, parts.length > 1 && parts[1].trim().equalsIgnoreCase("asc"));
    }

    TicketSummary summary(Ticket t, boolean staff, Map<UUID, UserSummary> people) {
        Workflow wf = vocabulary.workflow();
        return new TicketSummary(t.id(), t.ticketNumber(), t.subject(), CategoryView.of(categories.find(t.categoryCode())),
                t.priorityCode(), t.statusCode(), wf.kind(t.statusCode()), person(people.get(t.requesterId())),
                staff ? person(people.get(t.assignedAgentId())) : assignedForRequester(people.get(t.assignedAgentId())),
                staff && t.isEscalated(), staff ? overallSla(t) : null, t.contextRefs().get(ContextRefs.ORDER),
                t.contextRefs().get(ContextRefs.DELIVERY), t.createdAt(), t.updatedAt());
    }

    static PersonView person(UserSummary u) {
        return u == null ? null : new PersonView(u.id(), u.name(), u.role());
    }

    /** Requesters see that the ticket is handled, by name only. */
    private static PersonView assignedForRequester(UserSummary a) {
        return a == null ? null : new PersonView(a.id(), a.name(), null);
    }

    // ------------------------------------------------------------------ detail

    public Ticket require(Actor user, UUID id) {
        Ticket t = tickets.find(id).orElseThrow(() -> SupportException.notFound("Ticket"));
        vocabulary.policy().requireView(user, t);
        return t;
    }

    public TicketDetail detail(Actor user, UUID id) {
        Ticket t = require(user, id);
        boolean staff = user.role().isSupportStaff();
        List<ReopenRequest> reopenHistory = reopenRequests.list(t.id());
        ReopenRequest pending = reopenHistory.stream()
                .filter(r -> r.status() == ReopenRequest.Status.PENDING).findFirst().orElse(null);
        ReopenRequest lastDecision = reopenHistory.stream()
                .filter(r -> r.status() != ReopenRequest.Status.PENDING && r.decidedAt() != null)
                .max(Comparator.comparing(ReopenRequest::decidedAt)).orElse(null);
        var rating = feedback.find(t.id());
        TicketAccessPolicy policy = vocabulary.policy();
        Set<TicketAction> actions = policy.allowedActions(user, t, new TicketFacts(pending != null, rating.isPresent()));
        Set<String> targets = allowedTargets(t, actions);
        Map<UUID, UserSummary> people = people(List.of(t));
        ContextRefs refs = t.contextRefs();
        SatisfactionView satisfaction = rating.filter(r -> staff || t.requesterId().equals(user.id()))
                .map(r -> new SatisfactionView(r.rating(), r.comment(), r.ratedAt())).orElse(null);
        AiSuggestionView ai = actions.contains(TicketAction.USE_AI_ASSIST)
                ? aiSuggestions.findSuggestion(t.id()).map(s -> new AiSuggestionView(s.categoryCode(), s.priorityCode(),
                s.confidence(), s.reason(), s.createdAt(), s.decision())).orElse(null) : null;
        List<ExternalActionView> external = staff ? externalActions(t.id()) : List.of();
        return new TicketDetail(t.id(), t.ticketNumber(), t.subject(), t.description(),
                CategoryView.of(categories.find(t.categoryCode())), t.priorityCode(), t.statusCode(),
                vocabulary.workflow().kind(t.statusCode()), person(people.get(t.requesterId())),
                staff ? person(people.get(t.assignedAgentId())) : assignedForRequester(people.get(t.assignedAgentId())),
                t.isRequesterReportedUrgent(), staff && t.isEscalated(), staff ? t.escalatedAt() : null,
                new References(refs.get(ContextRefs.ORDER), staff ? refs.get(ContextRefs.PAYMENT) : null,
                        refs.get(ContextRefs.DELIVERY), staff ? refs.get(ContextRefs.SELLER) : null,
                        staff ? refs.get(ContextRefs.REFUND) : null, staff ? refs.asMap() : Map.of()),
                staff ? slaView(t) : null, ReopenView.of(pending), ReopenView.of(lastDecision), actions, targets,
                t.createdAt(), t.updatedAt(), t.resolvedAt(), t.closedAt(), t.version(), t.legacyNumber(),
                satisfaction, ai, external);
    }

    private List<ExternalActionView> externalActions(UUID ticketId) {
        var list = externalActions.list(ticketId);
        Map<UUID, UserSummary> people = directory.summaries(list.stream().map(a -> a.requestedBy())
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet()));
        return list.stream().map(a -> new ExternalActionView(a.id(), a.type(), a.targetRef(), a.succeeded(),
                a.resultRef(), a.error(), person(people.get(a.requestedBy())), a.createdAt())).toList();
    }

    private Set<String> allowedTargets(Ticket t, Set<TicketAction> actions) {
        Workflow wf = vocabulary.workflow();
        Set<String> targets = new LinkedHashSet<>();
        for (String s : wf.manualTargets(t.statusCode())) {
            boolean ok = switch (wf.kind(s)) {
                case RESOLVED -> actions.contains(TicketAction.RESOLVE);
                case CLOSED -> actions.contains(TicketAction.CLOSE);
                default -> actions.contains(TicketAction.CHANGE_STATUS);
            };
            if (ok) {
                targets.add(s);
            }
        }
        return targets;
    }

    SlaView slaView(Ticket t) {
        boolean open = vocabulary.workflow().isOpen(t.statusCode());
        SlaClock first = new SlaClock(t.createdAt(), t.firstResponseDueAt(), t.firstRespondedAt(),
                sla.state(t.createdAt(), t.firstResponseDueAt(), t.firstRespondedAt()));
        Instant resolvedAt = open ? null : t.resolvedAt();
        SlaClock resolution = new SlaClock(t.resolutionPeriodStartedAt(), t.resolutionDueAt(), resolvedAt,
                sla.state(t.resolutionPeriodStartedAt(), t.resolutionDueAt(), resolvedAt, t.slaPausedSince()));
        List<SlaPeriodView> periods = slaPeriods.periods(t.id()).stream()
                .map(p -> new SlaPeriodView(p.periodNumber(), p.priorityCode(), p.startedAt(), p.resolutionDueAt(),
                        p.endedAt(), p.endReason(), p.pausedMinutes()))
                .toList();
        return new SlaView(first, resolution, SlaState.worst(first.state(), resolution.state()), t.slaPausedSince(),
                periods);
    }

    SlaState overallSla(Ticket t) {
        boolean open = vocabulary.workflow().isOpen(t.statusCode());
        SlaState first = sla.state(t.createdAt(), t.firstResponseDueAt(), t.firstRespondedAt());
        SlaState resolution = sla.state(t.resolutionPeriodStartedAt(), t.resolutionDueAt(),
                open ? null : t.resolvedAt(), t.slaPausedSince());
        return SlaState.worst(first, resolution);
    }

    // ------------------------------------------------------------------ conversation (FR-007, FR-008)

    /**
     * @param visibility for support staff: PUBLIC, INTERNAL or null for both. Requesters always get PUBLIC only
     *                   — internal notes are never returned to them (BR-016, AC-007).
     */
    public List<MessageView> messages(Actor user, UUID ticketId, MessageVisibility visibility) {
        Ticket t = require(user, ticketId);
        Set<MessageVisibility> wanted;
        if (!canViewInternal(user, t)) {
            wanted = EnumSet.of(MessageVisibility.PUBLIC);
        } else if (visibility == null) {
            wanted = EnumSet.allOf(MessageVisibility.class);
        } else {
            wanted = EnumSet.of(visibility);
        }
        List<TicketMessage> list = conversation.messages(t.id(), wanted);
        Map<UUID, List<AttachmentView>> byMessage = list.isEmpty() ? Map.of()
                : conversation.attachmentsOf(list.stream().map(TicketMessage::id).toList()).stream()
                .collect(Collectors.groupingBy(AttachmentRecord::messageId,
                        Collectors.mapping(a -> new AttachmentView(a.id(), a.originalFilename(), a.contentType(),
                                a.sizeBytes(), a.createdAt()), Collectors.toList())));
        Map<UUID, UserSummary> authors = directory.summaries(list.stream().map(TicketMessage::authorId)
                .collect(Collectors.toSet()));
        return list.stream().map(m -> new MessageView(m.id(), person(authors.get(m.authorId())), m.visibility(),
                m.body(), m.createdAt(), byMessage.getOrDefault(m.id(), List.of()))).toList();
    }

    public MessageView message(Actor user, UUID ticketId, UUID messageId) {
        return messages(user, ticketId, null).stream().filter(m -> m.id().equals(messageId)).findFirst()
                .orElseThrow(() -> SupportException.notFound("Message"));
    }

    private boolean canViewInternal(Actor user, Ticket t) {
        return vocabulary.policy().allowedActions(user, t, TicketFacts.NONE).contains(TicketAction.VIEW_INTERNAL);
    }

    // ------------------------------------------------------------------ history (FR-009)

    public List<HistoryView> history(Actor user, UUID ticketId) {
        Ticket t = require(user, ticketId);
        boolean staff = user.role().isSupportStaff();
        List<HistoryEntry> entries = history.list(t.id(), !staff);
        Map<UUID, UserSummary> actors = directory.summaries(entries.stream().map(HistoryEntry::actorId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet()));
        return entries.stream().map(e -> new HistoryView(e.id(), e.action(), person(actors.get(e.actorId())),
                e.oldValue(), e.newValue(), staff ? e.details() : requesterSafeDetails(e), e.createdAt())).toList();
    }

    private static String requesterSafeDetails(HistoryEntry e) {
        return switch (e.action()) {
            case REOPEN_REQUESTED, REOPEN_REJECTED, REOPEN_APPROVED, AUTO_CLOSED -> e.details();
            default -> null;
        };
    }

    // ------------------------------------------------------------------ attachments (BR-046)

    public record AttachmentDownload(AttachmentView attachment, BinaryStorage.Content content) {
    }

    public AttachmentDownload download(Actor user, UUID ticketId, UUID attachmentId) {
        Ticket t = require(user, ticketId);
        AttachmentRecord a = conversation.findAttachment(attachmentId)
                .filter(x -> x.ticketId().equals(t.id()))
                .orElseThrow(() -> SupportException.notFound("Pièce jointe"));
        TicketMessage m = conversation.findMessage(a.messageId())
                .orElseThrow(() -> SupportException.notFound("Pièce jointe"));
        if (m.visibility() == MessageVisibility.INTERNAL && !canViewInternal(user, t)) {
            throw SupportException.notFound("Pièce jointe");
        }
        BinaryStorage.Content content = attachments.load(a.storageKey())
                .orElseThrow(() -> SupportException.notFound("Pièce jointe"));
        return new AttachmentDownload(new AttachmentView(a.id(), a.originalFilename(), a.contentType(), a.sizeBytes(),
                a.createdAt()), content);
    }

    // ------------------------------------------------------------------ business context (FR-011)

    public record RequesterProfile(UUID id, String name, Role role, String email, String phone, boolean active,
                                   Instant knownSince, long ticketCount) {
    }

    public record TicketContext(RequesterProfile requester, EcosystemService.Section<Ecosystem.OrderSummary> order,
                                EcosystemService.Section<Ecosystem.PaymentSummary> payment,
                                EcosystemService.Section<Ecosystem.DeliverySummary> delivery,
                                EcosystemService.Section<Ecosystem.SellerSummary> seller,
                                EcosystemService.Section<Ecosystem.RefundSummary> refund) {
    }

    /**
     * Operational context shown to support next to a ticket: requester profile, order, payment/payout, delivery,
     * seller and refund. Support-only (BR-019); each section degrades independently when a module is unavailable.
     */
    public TicketContext context(Actor user, UUID ticketId) {
        Ticket t = require(user, ticketId);
        if (!vocabulary.policy().allowedActions(user, t, TicketFacts.NONE).contains(TicketAction.VIEW_CONTEXT)) {
            throw SupportException.forbidden("Le contexte opérationnel est réservé au support.", "BR-019");
        }
        UserSummary r = directory.require(t.requesterId());
        RequesterProfile profile = new RequesterProfile(r.id(), r.name(), t.requesterRole(), r.email(), r.phone(),
                r.active(), r.knownSince(), search.countByRequester(r.id()));
        ContextRefs refs = t.contextRefs();
        return new TicketContext(profile, ecosystem.order(refs.get(ContextRefs.ORDER)),
                ecosystem.payment(refs.get(ContextRefs.PAYMENT)), ecosystem.delivery(refs.get(ContextRefs.DELIVERY)),
                ecosystem.seller(refs.get(ContextRefs.SELLER)), ecosystem.refund(refs.get(ContextRefs.REFUND)));
    }

    /** Whether a status counts as resolved (for clients that only know codes). */
    public StatusKind kindOf(String statusCode) {
        return vocabulary.workflow().kind(statusCode);
    }
}
