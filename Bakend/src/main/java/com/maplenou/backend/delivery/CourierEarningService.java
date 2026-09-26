package com.maplenou.backend.delivery;

import com.maplenou.backend.audit.AuditAction;
import com.maplenou.backend.audit.AuditService;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.delivery.dto.CourierEarningResponse;
import com.maplenou.backend.order.AddressSnapshot;
import com.maplenou.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Rémunération livreur : un {@link CourierEarning} est créé automatiquement
 * quand une {@link Delivery} passe à DELIVERED (voir {@link DeliveryService#updateStatus}).
 *
 * Montant provisoire — décision actée avec le porteur du projet plutôt que
 * de laisser ce module mocké : les frais de la {@link DeliveryZone}
 * correspondant à l'adresse de livraison, déduits par correspondance de
 * ville/quartier puisque l'`Order` ne mémorise pas encore quelle zone a été
 * appliquée au moment du checkout. Barème à formaliser plus tard (voir
 * `CLAUDE.md` §5, même logique que la commission vendeur).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourierEarningService {

    private static final BigDecimal DEFAULT_FEE = new BigDecimal("1000.00");

    private final CourierEarningRepository courierEarningRepository;
    private final DeliveryZoneRepository deliveryZoneRepository;
    private final AuditService auditService;

    @Transactional
    public void createForDelivery(Delivery delivery) {
        AddressSnapshot address = delivery.getSubOrder().getOrder().getDeliveryAddress();
        BigDecimal amount = resolveFee(address);

        CourierEarning earning = CourierEarning.builder()
                .delivery(delivery)
                .agent(delivery.getAgent())
                .amount(amount)
                .status(CourierEarningStatus.PENDING)
                .build();
        courierEarningRepository.save(earning);
    }

    @Transactional(readOnly = true)
    public Page<CourierEarningResponse> listMine(User agent, Pageable pageable) {
        return courierEarningRepository.findByAgentIdOrderByCreatedAtDesc(agent.getId(), pageable)
                .map(CourierEarningResponse::from);
    }

    @Transactional(readOnly = true)
    public BigDecimal getAvailableBalance(User agent) {
        return courierEarningRepository.sumAvailableBalanceByAgentId(agent.getId());
    }

    // ----- Admin -----

    /** Admin : tous les gains livreurs, filtrables par statut. */
    @Transactional(readOnly = true)
    public Page<CourierEarningResponse> listAllForAdmin(CourierEarningStatus status, Pageable pageable) {
        return courierEarningRepository.findByOptionalStatus(status, pageable).map(CourierEarningResponse::from);
    }

    /** Admin : marquer un gain comme versé (constaté manuellement, comme les payouts vendeur). */
    @Transactional
    public CourierEarningResponse markAsPaid(UUID id, UUID actorId, String ip) {
        CourierEarning earning = courierEarningRepository.findByIdWithAgent(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Gain introuvable"));

        if (earning.getStatus() == CourierEarningStatus.PAID) {
            throw new ApiException(HttpStatus.CONFLICT, "Ce gain est déjà marqué PAID");
        }

        earning.setStatus(CourierEarningStatus.PAID);
        earning.setPaidAt(Instant.now());
        courierEarningRepository.save(earning);

        auditService.log(actorId, AuditAction.COURIER_EARNING_MARKED_PAID,
                "COURIER_EARNING", id,
                "Livreur : " + earning.getAgent().getFullName() +
                " | Montant : " + earning.getAmount() + " FCFA", ip);

        return CourierEarningResponse.from(earning);
    }

    private BigDecimal resolveFee(AddressSnapshot address) {
        Optional<DeliveryZone> zone = Optional.<DeliveryZone>empty()
                .or(() -> address.getCity() != null
                        ? deliveryZoneRepository.findByNameIgnoreCase(address.getCity())
                        : Optional.empty())
                .or(() -> address.getDistrict() != null
                        ? deliveryZoneRepository.findByNameIgnoreCase(address.getDistrict())
                        : Optional.empty());

        if (zone.isPresent()) {
            return zone.get().getDeliveryFee();
        }

        // Aucune correspondance exacte (ex. orthographe différente) : la zone
        // phare du lancement sert de repli, sinon la première zone active.
        List<DeliveryZone> activeZones = deliveryZoneRepository.findByActiveTrueOrderByNameAsc();
        return activeZones.stream()
                .filter(z -> z.getName().equalsIgnoreCase("Grand Lomé"))
                .findFirst()
                .or(() -> activeZones.stream().findFirst())
                .map(DeliveryZone::getDeliveryFee)
                .orElse(DEFAULT_FEE);
    }
}
