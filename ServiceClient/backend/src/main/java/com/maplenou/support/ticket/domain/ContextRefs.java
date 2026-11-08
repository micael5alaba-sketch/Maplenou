package com.maplenou.support.ticket.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Objects of the business system a ticket refers to, as {@code (type, reference)} pairs (table
 * {@code ticket_context_refs}). Maplenou uses ORDER, PAYMENT, DELIVERY, SELLER and REFUND; another system declares
 * its own types (asset, enrolment, parcel…) without changing the ticket model (reference architecture §4.3).
 */
public final class ContextRefs {

    public static final String ORDER = "ORDER";
    public static final String PAYMENT = "PAYMENT";
    public static final String DELIVERY = "DELIVERY";
    public static final String SELLER = "SELLER";
    public static final String REFUND = "REFUND";

    public static final ContextRefs NONE = new ContextRefs(Map.of());

    private final Map<String, String> refs;

    public ContextRefs(Map<String, String> refs) {
        Map<String, String> copy = new LinkedHashMap<>();
        refs.forEach((type, ref) -> {
            if (ref != null && !ref.isBlank()) {
                copy.put(type, ref.trim());
            }
        });
        this.refs = Collections.unmodifiableMap(copy);
    }

    public String get(String type) {
        return refs.get(type);
    }

    public Map<String, String> asMap() {
        return refs;
    }

    public boolean isEmpty() {
        return refs.isEmpty();
    }
}
