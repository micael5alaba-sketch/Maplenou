package com.maplenou.backend.notification;

import com.maplenou.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Historique persistant des notifications — absent jusqu'ici (signalé par l'audit externe du
 * 16/10/2026) : seul le jeton FCM était enregistré ({@link com.maplenou.backend.notification.DeviceToken}),
 * pas la notification elle-même. Conséquence : rien après réinstallation de l'app, rien sur un
 * second appareil, rien si l'utilisateur a refusé les notifications push. Chaque méthode de
 * {@link NotificationService} écrit maintenant ici en plus (ou à la place) de l'envoi FCM, fire
 * -and-forget comme le reste de cette façade — jamais bloquant pour l'appelant.
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    /** Même valeur que le champ "type" déjà envoyé dans les données FCM (ex: "ORDER_PAID",
     * "NEW_MESSAGE") — réutilisé tel quel pour que l'app route pareil, push ou liste. */
    @Column(nullable = false, length = 40)
    private String type;

    /** Identifiant de la ressource concernée (commande, livraison, conversation...), selon
     * {@code type} — générique plutôt qu'une colonne par type de référence possible. */
    @Column(name = "reference_id")
    private String referenceId;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
