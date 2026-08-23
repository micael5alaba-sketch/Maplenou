package com.maplenou.backend.newsletter.dto;

import com.maplenou.backend.newsletter.NewsletterSubscriber;

import java.time.Instant;
import java.util.UUID;

public record SubscriberResponse(
        UUID id,
        String email,
        boolean active,
        Instant subscribedAt
) {
    public static SubscriberResponse from(NewsletterSubscriber s) {
        return new SubscriberResponse(s.getId(), s.getEmail(), s.isActive(), s.getSubscribedAt());
    }
}
