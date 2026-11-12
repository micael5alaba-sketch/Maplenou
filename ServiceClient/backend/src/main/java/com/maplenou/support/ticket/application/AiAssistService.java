package com.maplenou.support.ticket.application;

import com.maplenou.support.shared.Actor;
import com.maplenou.support.shared.SupportException;
import com.maplenou.support.ticket.application.port.AiAssistant;
import com.maplenou.support.ticket.application.port.AiSuggestionStore;
import com.maplenou.support.ticket.application.port.ConversationStore;
import com.maplenou.support.ticket.application.port.TicketStore;
import com.maplenou.support.ticket.domain.AiSuggestion;
import com.maplenou.support.ticket.domain.Category;
import com.maplenou.support.ticket.domain.HistoryAction;
import com.maplenou.support.ticket.domain.MessageVisibility;
import com.maplenou.support.ticket.domain.Ticket;
import com.maplenou.support.ticket.domain.TicketAction;
import com.maplenou.support.ticket.domain.TicketMessage;
import com.maplenou.support.ticket.domain.vocabulary.PriorityDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * AI help for support (DEC-CS11-08), always advisory:
 * <ul>
 *   <li>after a ticket is created, the assistant proposes a category and a priority; an agent applies or dismisses
 *       the proposal (nothing changes by itself);</li>
 *   <li>on request, the assistant drafts an answer from the help centre; the agent edits it before sending.</li>
 * </ul>
 * Only the subject, the description and public messages are sent to the assistant (no name, phone or e-mail).
 */
@Service
public class AiAssistService {

    private static final Logger log = LoggerFactory.getLogger(AiAssistService.class);
    private static final int MAX_LINES = 12;

    private final AiAssistant assistant;
    private final AiSuggestionStore suggestions;
    private final TicketStore tickets;
    private final ConversationStore conversation;
    private final TicketCommandService commands;
    private final VocabularyService vocabulary;
    private final CategoryService categories;
    private final Clock clock;

    public AiAssistService(AiAssistant assistant, AiSuggestionStore suggestions, TicketStore tickets,
                           ConversationStore conversation, TicketCommandService commands, VocabularyService vocabulary,
                           CategoryService categories, Clock clock) {
        this.assistant = assistant;
        this.suggestions = suggestions;
        this.tickets = tickets;
        this.conversation = conversation;
        this.commands = commands;
        this.vocabulary = vocabulary;
        this.categories = categories;
        this.clock = clock;
    }

    public boolean available() {
        return assistant.available();
    }

    /**
     * Asks the assistant for a category and a priority (called after the ticket creation is committed). The call
     * may take seconds: no transaction is held meanwhile.
     */
    public Optional<AiSuggestion> suggest(UUID ticketId) {
        if (!assistant.available()) {
            return Optional.empty();
        }
        Ticket t = tickets.find(ticketId).orElse(null);
        if (t == null || suggestions.findSuggestion(ticketId).isPresent()) {
            return Optional.empty();
        }
        List<AiAssistant.Option> cats = categories.active().stream()
                .map(c -> new AiAssistant.Option(c.code(), c.name(), c.description())).toList();
        // the assistant reads the priorities most urgent first (a higher rank is more urgent)
        List<AiAssistant.Option> prios = vocabulary.priorities().all().reversed().stream()
                .filter(PriorityDefinition::active)
                .map(p -> new AiAssistant.Option(p.code(), p.labelFr(), null)).toList();
        Optional<AiAssistant.Triage> triage;
        try {
            triage = assistant.triage(t.subject(), t.description(), "fr", cats, prios);
        } catch (RuntimeException e) {
            log.warn("AI triage failed for {}: {}", t.ticketNumber(), e.getMessage());
            return Optional.empty();
        }
        return triage.filter(x -> categories.find(x.categoryCode()) != null
                        && vocabulary.priorities().find(x.priorityCode()).isPresent())
                .map(x -> {
                    AiSuggestion s = new AiSuggestion(t.id(), x.categoryCode(), x.priorityCode(),
                            Math.max(0, Math.min(1, x.confidence())), x.reason(), x.model(), clock.instant(), null,
                            null, null);
                    suggestions.save(s);
                    return s;
                });
    }

    /** Applies or dismisses the proposal; applying changes category and priority like the agent would by hand. */
    @Transactional
    public void decide(Actor user, UUID ticketId, boolean apply) {
        Ticket t = commands.lock(ticketId);
        vocabulary.policy().require(user, t, commands.facts(t), TicketAction.USE_AI_ASSIST);
        AiSuggestion s = suggestions.findSuggestion(ticketId).filter(x -> x.decision() == null)
                .orElseThrow(() -> SupportException.notFound("Suggestion"));
        if (apply) {
            Category c = categories.find(s.categoryCode());
            if (c != null && c.active() && !c.code().equals(t.categoryCode())) {
                commands.changeCategory(user, ticketId, c.code());
            }
            if (!s.priorityCode().equals(t.priorityCode())) {
                commands.changePriority(user, ticketId,
                        new TicketCommands.ChangePriority(s.priorityCode(), "Suggestion de l'assistant appliquée"));
            }
            Ticket after = commands.lock(ticketId);
            commands.record(after, user, HistoryAction.AI_SUGGESTION_APPLIED, null,
                    s.categoryCode() + " / " + s.priorityCode(), s.reason(), false, clock.instant());
        }
        suggestions.save(s.decide(apply ? AiSuggestion.Decision.APPLIED : AiSuggestion.Decision.DISMISSED, user.id(),
                clock.instant()));
    }

    public record DraftView(String text, List<String> references) {
    }

    /** A reply draft grounded on the help centre; never sent automatically. */
    public DraftView draftReply(Actor user, UUID ticketId, String language) {
        Ticket t = tickets.find(ticketId).orElseThrow(() -> SupportException.notFound("Ticket"));
        vocabulary.policy().require(user, t, commands.facts(t), TicketAction.USE_AI_ASSIST);
        if (!assistant.available()) {
            throw SupportException.unavailable("AI_UNAVAILABLE",
                    "L'assistant n'est pas configuré ou ne répond pas (voir docs/guide/ai-in-tickets.md).");
        }
        List<TicketMessage> messages = conversation.messages(t.id(), EnumSet.of(MessageVisibility.PUBLIC));
        List<AiAssistant.ConversationLine> lines = messages.stream()
                .skip(Math.max(0, messages.size() - MAX_LINES))
                .map(m -> new AiAssistant.ConversationLine(
                        m.authorRole() != null && m.authorRole().isSupportStaff() ? "support" : "client", m.body()))
                .toList();
        String lang = "en".equals(language) ? "en" : "fr";
        return assistant.draftReply(t.subject(), t.description(), lines, lang, user.displayName())
                .map(d -> new DraftView(d.text(), d.references()))
                .orElseThrow(() -> SupportException.unavailable("AI_UNAVAILABLE",
                        "L'assistant n'a pas pu proposer de réponse. Réessayez ou répondez sans aide."));
    }
}
