package com.maplenou.support.ticket.application;

import com.maplenou.support.context.application.EcosystemService;
import com.maplenou.support.context.application.port.LegacyTicketSource.LegacyTicket;
import com.maplenou.support.context.application.port.LegacyTicketSource.LegacyTicketPage;
import com.maplenou.support.identity.application.UserDirectory;
import com.maplenou.support.identity.application.UserDirectory.UserSummary;
import com.maplenou.support.shared.Actor;
import com.maplenou.support.shared.Role;
import com.maplenou.support.shared.SupportException;
import com.maplenou.support.sla.application.SlaPeriodService;
import com.maplenou.support.sla.application.SlaService;
import com.maplenou.support.ticket.application.port.ConversationStore;
import com.maplenou.support.ticket.application.port.HistoryStore;
import com.maplenou.support.ticket.application.port.LegacySyncStore;
import com.maplenou.support.ticket.application.port.LegacySyncStore.LegacyLink;
import com.maplenou.support.ticket.application.port.TicketStore;
import com.maplenou.support.ticket.domain.Category;
import com.maplenou.support.ticket.domain.ContextRefs;
import com.maplenou.support.ticket.domain.HistoryAction;
import com.maplenou.support.ticket.domain.HistoryEntry;
import com.maplenou.support.ticket.domain.MessageVisibility;
import com.maplenou.support.ticket.domain.Ticket;
import com.maplenou.support.ticket.domain.TicketMessage;
import com.maplenou.support.ticket.domain.TicketNumbers;
import com.maplenou.support.ticket.domain.vocabulary.PriorityDefinition;
import com.maplenou.support.ticket.domain.vocabulary.StatusKind;
import com.maplenou.support.ticket.domain.vocabulary.Workflow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Import of the core's former support tickets (Bakend V46) so that nothing is lost when the support service replaces
 * that module (DEC-CS10-17), run by an administrator or every {@code LEGACY_SYNC_INTERVAL} while the Flutter app
 * still uses the old module (DEC-CS12-02). Idempotent: a ticket already imported (same legacy id) is not imported
 * again; when its status changed in the old module since the last run, one staff-only history entry says so (the
 * agent decides — nothing is changed automatically). Imported tickets keep their creation date, requester, subject,
 * description, related delivery and the old number; no notification is sent.
 *
 * <p>Cost: one call to Bakend per page of {@value #PAGE_SIZE} and one lookup query per page (not one per ticket).
 */
@Service
public class LegacyImportService {

    static final int PAGE_SIZE = 50;

    public record ImportResult(int imported, int skipped, int updated, long totalInSource) {
    }

    private final EcosystemService ecosystem;
    private final TicketStore tickets;
    private final ConversationStore conversation;
    private final HistoryStore history;
    private final LegacySyncStore links;
    private final VocabularyService vocabulary;
    private final CategoryService categories;
    private final SlaService sla;
    private final SlaPeriodService slaPeriods;
    private final UserDirectory directory;
    private final TransactionTemplate tx;
    private final Clock clock;

    public LegacyImportService(EcosystemService ecosystem, TicketStore tickets, ConversationStore conversation,
                               HistoryStore history, LegacySyncStore links, VocabularyService vocabulary, CategoryService categories,
                               SlaService sla, SlaPeriodService slaPeriods, UserDirectory directory,
                               PlatformTransactionManager txManager, Clock clock) {
        this.ecosystem = ecosystem;
        this.tickets = tickets;
        this.conversation = conversation;
        this.history = history;
        this.links = links;
        this.vocabulary = vocabulary;
        this.categories = categories;
        this.sla = sla;
        this.slaPeriods = slaPeriods;
        this.directory = directory;
        this.tx = new TransactionTemplate(txManager);
        this.clock = clock;
    }

    /** Run by an administrator (« Importer les anciens tickets »). */
    public ImportResult importAll(Actor admin) {
        if (admin.role() != Role.ADMINISTRATOR) {
            throw SupportException.forbidden("Réservé aux administrateurs.", "BR-076");
        }
        return run(admin);
    }

    /** Run by the scheduler (system actor): new legacy tickets are imported, status changes are noted. */
    public ImportResult synchronize() {
        return run(Actor.system());
    }

    private ImportResult run(Actor actor) {
        int imported = 0;
        int skipped = 0;
        int updated = 0;
        long total = 0;
        for (int page = 0; page < 10_000; page++) {
            LegacyTicketPage p = ecosystem.legacyTickets(page, PAGE_SIZE);
            total = p.totalItems();
            List<LegacyTicket> items = p.items().stream().filter(l -> l.id() != null).toList();
            Map<UUID, LegacyLink> known = links.find(items.stream().map(LegacyTicket::id).toList());
            for (LegacyTicket legacy : items) {
                LegacyLink link = known.get(legacy.id());
                if (link == null) {
                    Boolean created = tx.execute(status -> importOne(legacy, actor));
                    if (Boolean.TRUE.equals(created)) {
                        imported++;
                    } else {
                        skipped++;
                    }
                } else {
                    skipped++;
                    if (!Objects.equals(link.legacyStatus(), legacy.status())) {
                        Boolean noted = tx.execute(status -> noteChange(link, legacy));
                        if (Boolean.TRUE.equals(noted)) {
                            updated++;
                        }
                    }
                }
            }
            if (p.last() || p.items().isEmpty()) {
                break;
            }
        }
        return new ImportResult(imported, skipped, updated, total);
    }

    /** Status changed in the old module since the last run: record it, and tell the agents once. */
    private boolean noteChange(LegacyLink link, LegacyTicket l) {
        Instant now = clock.instant();
        links.save(new LegacyLink(link.legacyId(), link.ticketId(), l.status(), now));
        if (link.legacyStatus() == null) {
            return false; // imported before V6: the status at import time is unknown, only record the current one
        }
        String details = "Ancien support : " + link.legacyStatus() + " → " + l.status()
                + (l.resolutionNotes() == null || l.resolutionNotes().isBlank() ? "" : " — " + l.resolutionNotes().trim());
        history.append(new HistoryEntry(UUID.randomUUID(), link.ticketId(), null, null, HistoryAction.LEGACY_UPDATED,
                link.legacyStatus(), l.status(), truncate(details, 500), false, now));
        return true;
    }

    private boolean importOne(LegacyTicket l, Actor admin) {
        if (tickets.existsByLegacyId(l.id())) { // imported by a concurrent run
            return false;
        }
        Instant created = l.createdAt() == null ? clock.instant() : l.createdAt();
        Role requesterRole = "DELIVERY_AGENT".equals(l.requesterRole()) ? Role.DELIVERY_DRIVER : Role.CUSTOMER;
        UserSummary requester = directory.ensure(l.requesterId(), l.requesterName(), l.requesterEmail(),
                l.requesterPhone(), requesterRole, created);
        Category category = categories.find(categoryCode(l.category(), l.subject()));
        if (category == null) {
            category = categories.find(Category.OTHER);
        }
        Workflow wf = vocabulary.workflow();
        PriorityDefinition priority = vocabulary.priorities().defaultPriority();
        String number = TicketNumbers.format(created.atZone(ZoneOffset.UTC).getYear(), tickets.nextNumber());
        String subject = truncate(blankTo(l.subject(), "Demande importée"), 200);
        String description = blankTo(l.description(), subject);
        Role role = requester.role() == Role.DELIVERY_DRIVER ? Role.DELIVERY_DRIVER : requesterRole;
        Ticket t = Ticket.open(UUID.randomUUID(), number, subject, description, category.code(), priority,
                wf.system(StatusKind.NEW), requester.id(), role, false,
                new ContextRefs(java.util.Collections.singletonMap(ContextRefs.DELIVERY, l.relatedDeliveryRef())), created);
        t.setSlaTargets(sla.firstResponseDue(created, priority.code()), sla.resolutionDue(created, priority.code()));
        t.markImported(l.id(), l.ticketNumber());
        String body = l.photoUrl() == null ? description : description + "\n\nPièce jointe (ancien support) : " + l.photoUrl();
        UUID adminId = admin.isSystem() ? null : admin.id();
        boolean importerKnown = adminId != null && directory.find(adminId).isPresent();
        if ("RESOLVED".equals(l.status())) {
            Instant resolvedAt = l.resolvedAt() == null ? created : l.resolvedAt();
            t.markFirstResponse(resolvedAt);
            t.changeStatus(wf.system(StatusKind.RESOLVED), StatusKind.RESOLVED, resolvedAt);
        }
        tickets.save(t);
        links.save(new LegacyLink(l.id(), t.id(), l.status(), clock.instant()));
        slaPeriods.open(t.id(), priority.code(), created, t.resolutionDueAt());
        conversation.save(new TicketMessage(UUID.randomUUID(), t.id(), requester.id(), role, MessageVisibility.PUBLIC,
                body, null, created));
        history.append(new HistoryEntry(UUID.randomUUID(), t.id(), adminId, adminId == null ? null : admin.role(), HistoryAction.CREATED, null,
                wf.system(StatusKind.NEW), truncate("Importé de l'ancien support Maplenou (" + l.ticketNumber()
                + ", statut " + l.status() + ")", 500), true, created));
        if ("RESOLVED".equals(l.status())) {
            Instant resolvedAt = t.resolvedAt();
            if (importerKnown && l.resolutionNotes() != null && !l.resolutionNotes().isBlank()) {
                conversation.save(new TicketMessage(UUID.randomUUID(), t.id(), adminId, admin.role(),
                        MessageVisibility.PUBLIC, l.resolutionNotes().trim(), null, resolvedAt));
            }
            slaPeriods.endCurrent(t.id(), resolvedAt, "RESOLVED");
            history.append(new HistoryEntry(UUID.randomUUID(), t.id(), adminId, adminId == null ? null : admin.role(),
                    HistoryAction.STATUS_CHANGED, wf.system(StatusKind.NEW), t.statusCode(),
                    importerKnown || l.resolutionNotes() == null || l.resolutionNotes().isBlank()
                            ? "Résolu dans l'ancien support"
                            : truncate("Résolu dans l'ancien support — " + l.resolutionNotes().trim(), 500),
                    true, resolvedAt));
        } else if ("IN_PROGRESS".equals(l.status()) && importerKnown) {
            conversation.save(new TicketMessage(UUID.randomUUID(), t.id(), adminId, admin.role(),
                    MessageVisibility.INTERNAL,
                    "Ce ticket était « en cours » dans l'ancien support : il est à reprendre (non attribué).", null,
                    clock.instant()));
        }
        return true;
    }

    /** Old free-text categories (e.g. « Problème de livraison ») → category codes of the support service. */
    static String categoryCode(String category, String subject) {
        String text = Normalizer.normalize((blankTo(category, "") + " " + blankTo(subject, "")).toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        if (text.contains("rembours") || text.contains("retour")) {
            return "RETURN_REFUND";
        }
        if (text.contains("livraison") || text.contains("livreur") || text.contains("colis")) {
            return "DELIVERY";
        }
        if (text.contains("paiement") || text.contains("payment") || text.contains("mobile money")) {
            return "PAYMENT";
        }
        if (text.contains("commande")) {
            return "ORDER";
        }
        if (text.contains("vendeur") || text.contains("boutique")) {
            return "SELLER";
        }
        if (text.contains("compte") || text.contains("connexion") || text.contains("mot de passe")) {
            return "ACCOUNT";
        }
        return Category.OTHER;
    }

    private static String blankTo(String s, String fallback) {
        return s == null || s.isBlank() ? fallback : s.trim();
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
