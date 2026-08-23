package com.maplenou.backend.delivery;

import com.maplenou.backend.delivery.dto.CreateDeliveryZoneRequest;
import com.maplenou.backend.delivery.dto.DeliveryZoneResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/delivery-zones")
@RequiredArgsConstructor
@Tag(name = "Zones de livraison")
public class DeliveryZoneController {

    private final DeliveryZoneService zoneService;

    /** Public : liste les zones actives (le client choisit sa zone de livraison). */
    @GetMapping
    public List<DeliveryZoneResponse> listActive() {
        return zoneService.listActive();
    }

    /** Admin : créer une zone. */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<DeliveryZoneResponse> create(@Valid @RequestBody CreateDeliveryZoneRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(zoneService.create(request));
    }

    /** Admin : activer / désactiver une zone. */
    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public DeliveryZoneResponse toggle(@PathVariable UUID id) {
        return zoneService.toggleActive(id);
    }

    /** Admin : supprimer une zone. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        zoneService.delete(id);
    }
}
