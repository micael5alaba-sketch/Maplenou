package com.maplenou.support.ticket.application;

import com.maplenou.support.shared.Actor;
import com.maplenou.support.shared.Role;
import com.maplenou.support.shared.SupportException;
import com.maplenou.support.ticket.application.port.CategoryStore;
import com.maplenou.support.ticket.domain.Category;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.UUID;

/**
 * Ticket categories (BR-006, amended by DEC-CS11-06): administrators may add, rename, reorder and deactivate them;
 * they are never deleted and « Autre » stays active. Active categories are cached (read by every create form).
 */
@Service
public class CategoryService {

    private final CategoryStore store;
    private final Clock clock;
    private final Duration ttl;
    private volatile List<Category> cached;
    /** Same snapshot by code: {@link #find} is called for every ticket of a list or an export (O(1), not O(n)). */
    private volatile Map<String, Category> byCode = Map.of();
    private volatile Instant cachedAt = Instant.EPOCH;

    public CategoryService(CategoryStore store, Clock clock, TicketSettings settings) {
        this.store = store;
        this.clock = clock;
        this.ttl = settings.referenceTtl();
    }

    public List<Category> all() {
        List<Category> c = cached;
        if (c == null || cachedAt.plus(ttl).isBefore(clock.instant())) {
            c = store.all().stream().sorted(Comparator.comparingInt(Category::sortOrder)).toList();
            byCode = c.stream().collect(Collectors.toUnmodifiableMap(Category::code, x -> x, (a, b) -> a));
            cached = c;
            cachedAt = clock.instant();
        }
        return c;
    }

    public List<Category> active() {
        return all().stream().filter(Category::active).toList();
    }

    public Category find(String code) {
        all(); // refreshes the snapshot when it expired
        return code == null ? null : byCode.get(code);
    }

    /** A category a ticket may receive: known and active. */
    public Category requireActive(String code) {
        Category c = find(code);
        if (c == null || !c.active()) {
            throw SupportException.badRequest("UNKNOWN_CATEGORY", "Catégorie inconnue.", "BR-006");
        }
        return c;
    }

    @Transactional
    public Category create(Actor admin, String code, String name, String nameEn, String description,
                           String descriptionEn) {
        requireAdmin(admin);
        String normalized = code == null ? "" : code.trim().toUpperCase();
        if (!normalized.matches("[A-Z][A-Z0-9_]{1,29}")) {
            throw SupportException.badRequest("INVALID_CATEGORY_CODE",
                    "Code de catégorie : lettres majuscules, chiffres et _, 2 à 30 caractères.", "BR-006");
        }
        if (store.find(normalized).isPresent()) {
            throw SupportException.conflict("CATEGORY_EXISTS", "Cette catégorie existe déjà.", "BR-006");
        }
        int order = all().stream().mapToInt(Category::sortOrder).max().orElse(0) + 1;
        Category c = new Category(UUID.randomUUID(), normalized, name.trim(), blank(nameEn), blank(description),
                blank(descriptionEn), order, true);
        store.save(c);
        invalidate();
        return c;
    }

    @Transactional
    /** {@code descriptionEn} null = unchanged (older clients), blank = removed. */
    public Category update(Actor admin, String code, String name, String nameEn, String description,
                           String descriptionEn, Integer sortOrder, boolean active) {
        requireAdmin(admin);
        Category c = store.find(code).orElseThrow(() -> SupportException.notFound("Catégorie"));
        if (!active && Category.OTHER.equals(c.code())) {
            throw SupportException.businessRule("CATEGORY_REQUIRED", "La catégorie « Autre » doit rester active.", "BR-006");
        }
        Category updated = new Category(c.id(), c.code(), name.trim(), blank(nameEn), blank(description),
                descriptionEn == null ? c.descriptionEn() : blank(descriptionEn),
                sortOrder == null ? c.sortOrder() : sortOrder, active);
        store.save(updated);
        invalidate();
        return updated;
    }

    private void invalidate() {
        cached = null;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cached = null;
                }
            });
        }
    }

    private static void requireAdmin(Actor admin) {
        if (admin.role() != Role.ADMINISTRATOR) {
            throw SupportException.forbidden("Réservé aux administrateurs.", "BR-065");
        }
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
