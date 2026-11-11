package com.maplenou.backend.delivery;

import com.maplenou.backend.courier.CourierDocumentService;
import com.maplenou.backend.courier.CourierLocation;
import com.maplenou.backend.catalog.Shop;
import com.maplenou.backend.notification.NotificationService;
import com.maplenou.backend.user.Role;
import com.maplenou.backend.user.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Affectation automatique : quand une livraison est à faire, le système connaît déjà la position de
 * chaque livreur (envoyée en permanence par l'application) et choisit le livreur le PLUS PROCHE de
 * la boutique qui est utilisable : compte actif, documents validés, position récente, pas déjà
 * occupé par trop de courses, dans le rayon maximum, et qui n'a pas déjà refusé cette course.
 * L'admin garde toujours la possibilité d'affecter ou de réaffecter à la main.
 *
 * L'affectation elle-même passe par {@link DeliveryRepository#claim} (mise à jour atomique) : si
 * deux affectations se croisent, une seule gagne, jamais de course à deux livreurs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryDispatchService {

    @PersistenceContext
    private EntityManager em;

    private final DeliveryRepository deliveryRepository;
    private final DeliveryDeclineRepository declineRepository;
    private final CourierDocumentService courierDocumentService;
    private final NotificationService notificationService;

    /** Interrupteur général (création automatique de la livraison + affectation automatique). */
    @Value("${app.dispatch.enabled:true}")
    private boolean enabled;

    /** Au-delà de cette distance (km à vol d'oiseau) de la boutique, un livreur n'est pas retenu. */
    @Value("${app.dispatch.max-distance-km:15}")
    private double maxDistanceKm;

    /** Une position plus ancienne que cela est considérée périmée (livreur injoignable). */
    @Value("${app.dispatch.location-max-age-minutes:10}")
    private long locationMaxAgeMinutes;

    /** Nombre maximum de courses en cours (affectées ou démarrées) par livreur. */
    @Value("${app.dispatch.max-active-deliveries:1}")
    private int maxActiveDeliveries;

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Tente d'affecter la livraison au livreur le plus proche. Renvoie le livreur retenu, ou vide
     * si aucun n'est utilisable pour l'instant (la livraison reste PENDING : l'admin la voit, et la
     * tâche périodique réessaie).
     */
    @Transactional
    public Optional<User> tryAssign(UUID deliveryId) {
        if (!enabled) return Optional.empty();

        Delivery delivery = deliveryRepository.findByIdWithDetails(deliveryId).orElse(null);
        if (delivery == null || delivery.getStatus() != DeliveryStatus.PENDING || delivery.getAgent() != null) {
            return Optional.empty();
        }

        Shop shop = delivery.getSubOrder().getShop();
        if (shop.getLatitude() == null || shop.getLongitude() == null) {
            log.info("[Dispatch] Boutique {} sans coordonnées GPS : affectation manuelle requise", shop.getName());
            return Optional.empty();
        }
        double shopLat = shop.getLatitude().doubleValue();
        double shopLon = shop.getLongitude().doubleValue();

        Set<UUID> declined = Set.copyOf(declineRepository.findDeclinedAgentIds(deliveryId));
        Map<UUID, Long> activeByAgent = activeDeliveriesByAgent();

        @SuppressWarnings("unchecked")
        List<CourierLocation> locations = em.createQuery(
                        "SELECT l FROM CourierLocation l JOIN FETCH l.agent a "
                                + "WHERE a.active = true AND a.role = :role AND l.updatedAt >= :since")
                .setParameter("role", Role.DELIVERY_AGENT)
                .setParameter("since", Instant.now().minus(Duration.ofMinutes(locationMaxAgeMinutes)))
                .getResultList();

        List<CourierLocation> ranked = locations.stream()
                .filter(l -> !declined.contains(l.getAgentId()))
                .filter(l -> activeByAgent.getOrDefault(l.getAgentId(), 0L) < maxActiveDeliveries)
                .filter(l -> distanceKm(l, shopLat, shopLon) <= maxDistanceKm)
                .sorted(Comparator.comparingDouble(l -> distanceKm(l, shopLat, shopLon)))
                .toList();

        for (CourierLocation candidate : ranked) {
            User agent = candidate.getAgent();
            // Les documents sont vérifiés seulement pour les candidats, du plus proche au plus éloigné.
            if (!courierDocumentService.isFullyVerified(agent.getId())) continue;

            if (deliveryRepository.claim(deliveryId, agent, Instant.now()) == 1) {
                log.info("[Dispatch] Livraison {} affectée à {} ({} km de la boutique)",
                        deliveryId, agent.getFullName(), String.format("%.1f", distanceKm(candidate, shopLat, shopLon)));
                notificationService.deliveryAssigned(agent.getId(), deliveryId, shop.getName());
                return Optional.of(agent);
            }
            return Optional.empty(); // quelqu'un d'autre l'a déjà prise entre-temps
        }
        return Optional.empty();
    }

    /** Livraisons en attente d'un livreur, les plus anciennes d'abord (tâche périodique). */
    @Transactional(readOnly = true)
    public List<UUID> waitingDeliveryIds(int limit) {
        @SuppressWarnings("unchecked")
        List<UUID> ids = em.createQuery("SELECT d.id FROM Delivery d WHERE d.status = :s AND d.agent IS NULL ORDER BY d.createdAt ASC")
                .setParameter("s", DeliveryStatus.PENDING)
                .setMaxResults(limit)
                .getResultList();
        return ids;
    }

    private Map<UUID, Long> activeDeliveriesByAgent() {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createQuery("SELECT d.agent.id, COUNT(d) FROM Delivery d "
                        + "WHERE d.agent IS NOT NULL AND d.status IN :st GROUP BY d.agent.id")
                .setParameter("st", List.of(DeliveryStatus.ASSIGNED, DeliveryStatus.IN_TRANSIT))
                .getResultList();
        return rows.stream().collect(Collectors.toMap(r -> (UUID) r[0], r -> (Long) r[1], (a, b) -> a, HashMap::new));
    }

    private double distanceKm(CourierLocation l, double shopLat, double shopLon) {
        return GeoUtils.haversineKm(l.getLatitude(), l.getLongitude(), shopLat, shopLon);
    }
}
