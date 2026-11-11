package com.maplenou.support.ticket.adapter.out.persistence;

import com.maplenou.support.ticket.application.port.AiSuggestionStore;
import com.maplenou.support.ticket.application.port.CannedReplyStore;
import com.maplenou.support.ticket.application.port.ExternalActionStore;
import com.maplenou.support.ticket.application.port.FeedbackStore;
import com.maplenou.support.ticket.domain.AiSuggestion;
import com.maplenou.support.ticket.domain.CannedReply;
import com.maplenou.support.ticket.domain.ExternalAction;
import com.maplenou.support.ticket.domain.Satisfaction;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class JpaFeedbackStores implements FeedbackStore, CannedReplyStore, AiSuggestionStore, ExternalActionStore {

    private final SatisfactionJpaRepository ratings;
    private final CannedReplyJpaRepository replies;
    private final AiSuggestionJpaRepository suggestions;
    private final ExternalActionJpaRepository actions;

    JpaFeedbackStores(SatisfactionJpaRepository ratings, CannedReplyJpaRepository replies,
                      AiSuggestionJpaRepository suggestions, ExternalActionJpaRepository actions) {
        this.ratings = ratings;
        this.replies = replies;
        this.suggestions = suggestions;
        this.actions = actions;
    }

    // ------------------------------------------------------------------ FeedbackStore

    @Override
    public Optional<Satisfaction> find(UUID ticketId) {
        return ratings.findById(ticketId).map(SatisfactionEntity::toDomain);
    }

    @Override
    public void save(Satisfaction satisfaction) {
        ratings.save(new SatisfactionEntity(satisfaction));
    }

    @Override
    public Stats stats(Instant from, Instant to) {
        List<Object[]> rows = ratings.stats(from, to);
        Object[] row = rows.isEmpty() ? new Object[]{0L, null, null} : rows.getFirst();
        long count = row[0] == null ? 0 : ((Number) row[0]).longValue();
        if (count == 0) {
            return new Stats(0, null, null);
        }
        double avg = ((Number) row[1]).doubleValue();
        long satisfied = row[2] == null ? 0 : ((Number) row[2]).longValue();
        return new Stats(count, Math.round(avg * 100) / 100.0, Math.round(1000.0 * satisfied / count) / 10.0);
    }

    // ------------------------------------------------------------------ CannedReplyStore

    @Override
    public List<CannedReply> all() {
        return replies.findAll().stream().map(CannedReplyEntity::toDomain).toList();
    }

    @Override
    public Optional<CannedReply> findReply(UUID id) {
        return replies.findById(id).map(CannedReplyEntity::toDomain);
    }

    @Override
    public void save(CannedReply reply) {
        CannedReplyEntity e = replies.findById(reply.id()).orElseGet(() -> new CannedReplyEntity(reply));
        e.copyFrom(reply);
        replies.save(e);
    }

    @Override
    public void delete(UUID id) {
        replies.deleteById(id);
    }

    // ------------------------------------------------------------------ AiSuggestionStore

    @Override
    public Optional<AiSuggestion> findSuggestion(UUID ticketId) {
        return suggestions.findById(ticketId).map(AiSuggestionEntity::toDomain);
    }

    @Override
    public void save(AiSuggestion suggestion) {
        AiSuggestionEntity e = suggestions.findById(suggestion.ticketId())
                .orElseGet(() -> new AiSuggestionEntity(suggestion));
        e.copyFrom(suggestion);
        suggestions.save(e);
    }

    // ------------------------------------------------------------------ ExternalActionStore

    @Override
    public void save(ExternalAction action) {
        actions.save(new ExternalActionEntity(action));
    }

    @Override
    public List<ExternalAction> list(UUID ticketId) {
        return actions.findByTicketIdOrderByCreatedAtAsc(ticketId).stream().map(ExternalActionEntity::toDomain).toList();
    }
}
