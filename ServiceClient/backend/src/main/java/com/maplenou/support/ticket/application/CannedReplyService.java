package com.maplenou.support.ticket.application;

import com.maplenou.support.shared.Actor;
import com.maplenou.support.shared.SupportException;
import com.maplenou.support.ticket.application.port.CannedReplyStore;
import com.maplenou.support.ticket.domain.CannedReply;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** « Réponses types » (mockup 13): support reads them, managers and administrators maintain them. */
@Service
public class CannedReplyService {

    private final CannedReplyStore store;
    private final CategoryService categories;
    private final Clock clock;

    public CannedReplyService(CannedReplyStore store, CategoryService categories, Clock clock) {
        this.store = store;
        this.categories = categories;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<CannedReply> list(Actor user, boolean includeInactive) {
        requireStaff(user);
        return store.all().stream().filter(r -> includeInactive || r.active())
                .sorted(Comparator.comparing(CannedReply::title, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    @Transactional
    public CannedReply save(Actor user, UUID id, String title, String body, String language, String categoryCode,
                            boolean active) {
        if (!user.role().isSupervisor()) {
            throw SupportException.forbidden("Réservé aux responsables et administrateurs.", "BR-065");
        }
        if (!"fr".equals(language) && !"en".equals(language)) {
            throw SupportException.badRequest("INVALID_LANGUAGE", "Langue attendue : fr ou en.", null);
        }
        if (categoryCode != null && !categoryCode.isBlank() && categories.find(categoryCode) == null) {
            throw SupportException.badRequest("UNKNOWN_CATEGORY", "Catégorie inconnue.", "BR-006");
        }
        if (id != null && store.findReply(id).isEmpty()) {
            throw SupportException.notFound("Réponse type");
        }
        CannedReply reply = new CannedReply(id == null ? UUID.randomUUID() : id, title.trim(), body.trim(), language,
                categoryCode == null || categoryCode.isBlank() ? null : categoryCode, active, user.id(), clock.instant());
        store.save(reply);
        return reply;
    }

    @Transactional
    public void delete(Actor user, UUID id) {
        if (!user.role().isSupervisor()) {
            throw SupportException.forbidden("Réservé aux responsables et administrateurs.", "BR-065");
        }
        store.delete(id);
    }

    private static void requireStaff(Actor user) {
        if (!user.role().isSupportStaff()) {
            throw SupportException.forbidden("Réservé au support.", "BR-016");
        }
    }
}
