package com.maplenou.support.notification.domain;

import java.util.Map;

/**
 * Text of a notification for one event and one language (table {@code notification_templates}, DEC-CS11-09).
 * Placeholders: {@code {ticket}}, {@code {subject}}, {@code {status}}, {@code {decision}}.
 */
public record MessageTemplate(NotificationType type, String language, String title, String body) {

    public String renderTitle(Map<String, String> values) {
        return render(title, values, 160);
    }

    public String renderBody(Map<String, String> values) {
        return render(body, values, 500);
    }

    static String render(String template, Map<String, String> values, int max) {
        String out = template;
        for (Map.Entry<String, String> e : values.entrySet()) {
            out = out.replace("{" + e.getKey() + "}", e.getValue() == null ? "" : e.getValue());
        }
        return out.length() <= max ? out : out.substring(0, max - 1) + "…";
    }
}
