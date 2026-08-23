package com.maplenou.backend.newsletter;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "newsletter_subscribers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsletterSubscriber {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "subscribed_at", nullable = false, updatable = false)
    private Instant subscribedAt;

    @Column(name = "unsubscribed_at")
    private Instant unsubscribedAt;

    @PrePersist
    void onCreate() {
        this.subscribedAt = Instant.now();
    }
}
