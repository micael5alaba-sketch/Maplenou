package com.maplenou.backend.delivery;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Filet de sécurité de l'affectation automatique : une livraison créée alors qu'aucun livreur
 * n'était utilisable (tous occupés, hors de portée, position périmée) est retentée régulièrement,
 * dès qu'un livreur se libère ou se rapproche. Sans cela elle attendrait l'admin indéfiniment.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryDispatchScheduler {

    private static final int BATCH = 50;

    private final DeliveryDispatchService dispatchService;

    @Scheduled(fixedDelayString = "${app.dispatch.retry-millis:60000}", initialDelayString = "${app.dispatch.retry-millis:60000}")
    public void retryWaitingDeliveries() {
        if (!dispatchService.isEnabled()) return;
        for (UUID deliveryId : dispatchService.waitingDeliveryIds(BATCH)) {
            try {
                dispatchService.tryAssign(deliveryId);
            } catch (Exception e) {
                log.warn("[Dispatch] Echec de l'affectation automatique de {} : {}", deliveryId, e.getMessage());
            }
        }
    }
}
