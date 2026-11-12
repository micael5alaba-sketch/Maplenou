package com.maplenou.backend.supportbridge;

import com.maplenou.backend.supportbridge.SupportBridgeDtos.*;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * API interne pour le Service client : lecture (adaptateur HttpEcosystemGateway) et quatre actions demandées depuis un ticket
 * (HttpEcosystemActions, BakendPushRelay — SupportBridgeActionService).
 * Protégée par {@link SupportBridgeTokenFilter} ; absente de Swagger (usage de service à service uniquement).
 */
@Hidden
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1")
public class SupportBridgeController {

    private final SupportBridgeService bridge;
    private final SupportBridgeActionService actions;

    @GetMapping("/orders/{ref}")
    public ResponseEntity<OrderSummary> order(@PathVariable String ref) {
        return ResponseEntity.of(bridge.order(ref));
    }

    @GetMapping("/payments/{ref}")
    public ResponseEntity<PaymentSummary> payment(@PathVariable String ref) {
        return ResponseEntity.of(bridge.payment(ref));
    }

    @GetMapping("/deliveries/{ref}")
    public ResponseEntity<DeliverySummary> delivery(@PathVariable String ref) {
        return ResponseEntity.of(bridge.delivery(ref));
    }

    @GetMapping("/sellers/{ref}")
    public ResponseEntity<SellerSummary> seller(@PathVariable String ref) {
        return ResponseEntity.of(bridge.seller(ref));
    }

    @GetMapping("/refunds/{ref}")
    public ResponseEntity<RefundSummary> refund(@PathVariable String ref) {
        return ResponseEntity.of(bridge.refund(ref));
    }

    @GetMapping("/users/{id}/orders")
    public List<OrderSummary> ordersOf(@PathVariable UUID id) {
        return bridge.ordersOf(id);
    }

    @GetMapping("/drivers/{id}/deliveries")
    public List<DeliverySummary> deliveriesOf(@PathVariable UUID id) {
        return bridge.deliveriesOf(id);
    }

    // ---------------------------------------------------------------- actions (DEC-CS11-07)

    @PostMapping("/orders/{ref}/return-requests")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public ActionResult returnRequest(@PathVariable String ref, @jakarta.validation.Valid @RequestBody ReturnRequestAction body) {
        return actions.requestReturn(ref, body);
    }

    @PostMapping("/deliveries/{ref}/reschedule")
    public ActionResult reschedule(@PathVariable String ref, @jakarta.validation.Valid @RequestBody RescheduleAction body) {
        return actions.rescheduleDelivery(ref, body);
    }

    @PostMapping("/drivers/{id}/support-messages")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public ActionResult driverMessage(@PathVariable UUID id, @jakarta.validation.Valid @RequestBody DriverMessageAction body) {
        return actions.messageDriver(id, body);
    }

    @PostMapping("/users/{id}/push")
    @ResponseStatus(org.springframework.http.HttpStatus.ACCEPTED)
    public void push(@PathVariable UUID id, @jakarta.validation.Valid @RequestBody PushRelay body) {
        actions.relayPush(id, body);
    }

    @GetMapping("/support-tickets")
    public LegacyTicketPage legacyTickets(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "50") int size) {
        return bridge.legacyTickets(page, size);
    }
}
