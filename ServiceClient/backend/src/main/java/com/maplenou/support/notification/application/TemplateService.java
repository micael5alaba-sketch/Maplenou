package com.maplenou.support.notification.application;

import com.maplenou.support.notification.application.port.TemplateStore;
import com.maplenou.support.notification.domain.MessageTemplate;
import com.maplenou.support.notification.domain.NotificationType;
import com.maplenou.support.shared.Actor;
import com.maplenou.support.shared.Role;
import com.maplenou.support.shared.SupportException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Notification texts per event and language (DEC-CS11-09), editable by administrators, cached. */
@Service
public class TemplateService {

    private final TemplateStore store;
    private final Clock clock;
    private final NotificationSettings settings;
    private volatile Map<String, MessageTemplate> cached;
    private volatile Instant cachedAt = Instant.EPOCH;

    public TemplateService(TemplateStore store, Clock clock, NotificationSettings settings) {
        this.store = store;
        this.clock = clock;
        this.settings = settings;
    }

    /** The template in the language, else in French. */
    public Optional<MessageTemplate> find(NotificationType type, String language) {
        Map<String, MessageTemplate> all = all();
        MessageTemplate t = all.get(key(type, language));
        return Optional.ofNullable(t != null ? t : all.get(key(type, "fr")));
    }

    @Transactional(readOnly = true)
    public List<MessageTemplate> list() {
        return all().values().stream()
                .sorted(Comparator.comparing((MessageTemplate t) -> t.type().ordinal()).thenComparing(MessageTemplate::language))
                .toList();
    }

    @Transactional
    public MessageTemplate update(Actor admin, NotificationType type, String language, String title, String body) {
        if (admin.role() != Role.ADMINISTRATOR) {
            throw SupportException.forbidden("Réservé aux administrateurs.", "BR-065");
        }
        if (!"fr".equals(language) && !"en".equals(language)) {
            throw SupportException.badRequest("INVALID_LANGUAGE", "Langue attendue : fr ou en.", null);
        }
        MessageTemplate t = new MessageTemplate(type, language, title.trim(), body.trim());
        store.save(t);
        cachedAt = Instant.EPOCH;
        return t;
    }

    private Map<String, MessageTemplate> all() {
        Map<String, MessageTemplate> c = cached;
        if (c == null || cachedAt.plus(settings.templateCacheTtl()).isBefore(clock.instant())) {
            c = store.allTemplates().stream().collect(Collectors.toMap(t -> key(t.type(), t.language()),
                    Function.identity()));
            cached = c;
            cachedAt = clock.instant();
        }
        return c;
    }

    private static String key(NotificationType type, String language) {
        return type.name() + ":" + language;
    }
}
