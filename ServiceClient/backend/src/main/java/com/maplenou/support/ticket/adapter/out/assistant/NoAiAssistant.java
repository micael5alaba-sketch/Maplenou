package com.maplenou.support.ticket.adapter.out.assistant;

import com.maplenou.support.ticket.application.port.AiAssistant;

import java.util.List;
import java.util.Optional;

/** AI help switched off ({@code AI_ASSIST_ENABLED=false} or no assistant address/token). */
public class NoAiAssistant implements AiAssistant {

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public Optional<Triage> triage(String subject, String description, String language, List<Option> categories,
                                   List<Option> priorities) {
        return Optional.empty();
    }

    @Override
    public Optional<Draft> draftReply(String subject, String description, List<ConversationLine> conversation,
                                      String language, String agentName) {
        return Optional.empty();
    }
}
