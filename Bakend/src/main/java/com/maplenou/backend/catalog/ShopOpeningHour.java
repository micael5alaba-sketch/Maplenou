package com.maplenou.backend.catalog;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Horaire d'ouverture de la boutique pour un jour de la semaine — signalé manquant dans le
 * rapport de modification du 28/10/2026 : un acheteur visitant une boutique ne pouvait pas
 * savoir si/quand elle est ouverte. Au plus 7 lignes par boutique (une par jour), voir
 * {@link Shop#getOpeningHours()}.
 */
@Entity
@Table(name = "shop_opening_hours")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopOpeningHour {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    /** 1 = lundi ... 7 = dimanche (convention ISO-8601, {@code DayOfWeek.getValue()}). short plutôt
     * qu'int : Hibernate le mappe nativement en SMALLINT, un columnDefinition explicite donnait
     * un faux positif de validation de schéma (comparaison textuelle "smallint" vs "int2", le nom
     * interne Postgres du même type — simple quirk d'affichage, pas un vrai désaccord de type). */
    @Column(name = "day_of_week", nullable = false)
    private short dayOfWeek;

    @Column(name = "opens_at")
    private LocalTime opensAt;

    @Column(name = "closes_at")
    private LocalTime closesAt;

    /** Fermé ce jour-là — opensAt/closesAt restent alors à null. */
    @Column(nullable = false)
    @Builder.Default
    private boolean closed = false;
}
