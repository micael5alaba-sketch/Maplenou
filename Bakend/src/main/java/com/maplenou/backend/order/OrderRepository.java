package com.maplenou.backend.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Liste paginée des commandes d'un acheteur.
     * Sans EntityGraph sur la collection subOrders pour éviter le in-memory pagination.
     * La taille de la collection (subOrderCount) est chargée via batch_fetch_size.
     */
    Page<Order> findByBuyerIdOrderByCreatedAtDesc(UUID buyerId, Pageable pageable);

    // "so.items" reste lazy (chargé via hibernate.default_batch_fetch_size dans la transaction
    // ouverte de l'appelant) : fetch-joindre à la fois o.subOrders et so.items lève
    // org.hibernate.loader.MultipleBagFetchException (deux collections List/"bags" à la fois).
    // Ce bug cassait TOUT webhook de paiement (PaymentWebhookService) et GET /api/orders/{id}.
    @Query("""
            SELECT o FROM Order o
            LEFT JOIN FETCH o.subOrders so
            LEFT JOIN FETCH so.shop
            WHERE o.id = :id
            """)
    Optional<Order> findByIdWithDetails(@Param("id") UUID id);

    // ----- KPI queries -----

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status IN (com.maplenou.backend.order.OrderStatus.PAID, com.maplenou.backend.order.OrderStatus.CLOSED)")
    BigDecimal sumTotalRevenue();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status IN (com.maplenou.backend.order.OrderStatus.PAID, com.maplenou.backend.order.OrderStatus.CLOSED) AND o.createdAt >= :since")
    BigDecimal sumRevenueSince(@Param("since") Instant since);

    long countByStatus(OrderStatus status);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :since")
    long countCreatedSince(@Param("since") Instant since);
}
