package com.maplenou.backend.catalog;

import com.maplenou.backend.notification.NotificationService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Prévient les acheteurs qui « suivent » une boutique quand un nouveau produit est mis en vente.
 * Il n'existe pas (encore) d'abonnement explicite à une boutique : sont considérés comme abonnés
 * les acheteurs qui ont déjà mis un de ses produits en favoris ou qui ont déjà commandé chez elle.
 * Plafonné pour qu'une grosse boutique ne déclenche jamais une rafale d'envois.
 */
@Service
@RequiredArgsConstructor
public class ShopFollowerNotifier {

    static final int MAX_RECIPIENTS = 200;

    @PersistenceContext
    private EntityManager em;

    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public void notifyNewProduct(Shop shop, Product product) {
        UUID ownerId = shop.getOwner().getId();

        @SuppressWarnings("unchecked")
        List<UUID> followers = em.createQuery(
                        "SELECT DISTINCT f.user.id FROM Favorite f WHERE f.product.shop.id = :shopId AND f.user.id <> :ownerId")
                .setParameter("shopId", shop.getId())
                .setParameter("ownerId", ownerId)
                .setMaxResults(MAX_RECIPIENTS)
                .getResultList();

        if (followers.size() < MAX_RECIPIENTS) {
            @SuppressWarnings("unchecked")
            List<UUID> buyers = em.createQuery(
                            "SELECT DISTINCT so.order.buyer.id FROM SubOrder so WHERE so.shop.id = :shopId AND so.order.buyer.id <> :ownerId")
                    .setParameter("shopId", shop.getId())
                    .setParameter("ownerId", ownerId)
                    .setMaxResults(MAX_RECIPIENTS)
                    .getResultList();
            followers = new java.util.ArrayList<>(new java.util.LinkedHashSet<UUID>(
                    java.util.stream.Stream.concat(followers.stream(), buyers.stream()).toList()));
        }

        followers.stream().limit(MAX_RECIPIENTS).forEach(recipientId ->
                notificationService.newProduct(recipientId, shop.getName(), product.getName(), shop.getSlug()));
    }
}
