package com.maplenou.support.ticket.application.port;

import java.util.List;
import java.util.Optional;

/**
 * The documentation assistant used as an AI helper for support (DEC-CS11-08). Only the ticket's subject, description
 * and public messages are sent — no name, phone, e-mail or payment data. Unavailable or not configured: empty.
 */
public interface AiAssistant {

    record Option(String code, String label, String description) {
    }

    record Triage(String categoryCode, String priorityCode, double confidence, String reason, String model) {
    }

    record ConversationLine(String from, String text) {
    }

    record Draft(String text, List<String> references, String model) {
    }

    boolean available();

    Optional<Triage> triage(String subject, String description, String language, List<Option> categories,
                            List<Option> priorities);

    Optional<Draft> draftReply(String subject, String description, List<ConversationLine> conversation,
                               String language, String agentName);
}
