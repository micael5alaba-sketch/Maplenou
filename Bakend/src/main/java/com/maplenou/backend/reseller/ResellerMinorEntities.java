package com.maplenou.backend.reseller;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

// Petites entités du programme partenaire regroupées ici (même package, usage interne au module).

/** Clic sur un lien partenaire : sert à l'attribution (valid) et aux statistiques (counted). */
@Entity
@Table(name = "reseller_clicks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
class ResellerClick {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "link_id", nullable = false)
    private UUID linkId;

    @Column(name = "partner_id", nullable = false)
    private UUID partnerId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "visitor_hash", nullable = false, length = 64)
    private String visitorHash;

    @Column(nullable = false)
    private boolean valid;

    @Column(nullable = false)
    private boolean counted;

    @Column(name = "clicked_at", nullable = false)
    private Instant clickedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}

/** Attribution GELEE à la création de la commande, une par ligne : aucun clic ultérieur ne la change. */
@Entity
@Table(name = "reseller_attributions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
class ResellerAttribution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_item_id", nullable = false, unique = true)
    private UUID orderItemId;

    @Column(name = "partner_id", nullable = false)
    private UUID partnerId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "click_id", nullable = false)
    private UUID clickId;

    @Column(name = "buyer_id", nullable = false)
    private UUID buyerId;

    @Column(name = "clicked_at", nullable = false)
    private Instant clickedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}

/** Journal des actions administratives du programme (ancienne et nouvelle valeur conservées). */
@Entity
@Table(name = "reseller_audit_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
class ResellerAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 40)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}

/** Acceptation des conditions du programme : date et version conservées. */
@Entity
@Table(name = "reseller_terms_acceptances")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
class ResellerTermsAcceptance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "terms_version", nullable = false, length = 20)
    private String termsVersion;

    @Column(name = "accepted_at", nullable = false, updatable = false)
    private Instant acceptedAt;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @PrePersist
    void onCreate() {
        if (acceptedAt == null) acceptedAt = Instant.now();
    }
}

/** Exclusion manuelle d'un produit du programme. Absent = non exclu. */
@Entity
@Table(name = "reseller_product_eligibility")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
class ResellerProductEligibility {

    @Id
    @Column(name = "product_id")
    private UUID productId;

    @Column(nullable = false)
    private boolean excluded;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
