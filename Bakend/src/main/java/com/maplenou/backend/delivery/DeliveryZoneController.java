package com.maplenou.backend.delivery;

import com.maplenou.backend.catalog.Shop;
import com.maplenou.backend.catalog.ShopService;
import com.maplenou.backend.delivery.dto.CreateDeliveryZoneRequest;
import com.maplenou.backend.delivery.dto.DeliveryFeeQuote;
import com.maplenou.backend.delivery.dto.DeliveryZoneResponse;
import com.maplenou.backend.delivery.dto.UpdateDeliveryZoneRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/delivery-zones")
@RequiredArgsConstructor
@Tag(name = "Zones de livraison")
public class DeliveryZoneController {

    private final DeliveryZoneService zoneService;
    private final DeliveryPricingService deliveryPricingService;
    private final ShopService shopService;

    /** Public : liste les zones actives (le client choisit sa zone de livraison). */
    @GetMapping
    public List<DeliveryZoneResponse> listActive() {
        return zoneService.listActive();
    }

    /** Admin : toutes les zones, y compris désactivées (sinon impossible de les réactiver). */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public List<DeliveryZoneResponse> listAllForAdmin() {
        return zoneService.listAllForAdmin();
    }

    /** Admin : créer une zone. */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<DeliveryZoneResponse> create(@Valid @RequestBody CreateDeliveryZoneRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(zoneService.create(request));
    }

    /** Admin : ajuster le tarif de base et/ou le prix au km d'une zone existante. */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public DeliveryZoneResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateDeliveryZoneRequest request) {
        return zoneService.update(id, request);
    }

    /**
     * Public : devis de livraison avant paiement (checkout) — mêmes calcul et chiffres que
     * ceux réellement figés sur le SubOrder par {@code OrderService.placeOrder}, pour que ce
     * que l'acheteur voit corresponde exactement à ce qui sera facturé.
     */
    @GetMapping("/quote")
    public DeliveryFeeQuote quote(@RequestParam UUID shopId,
                                   @RequestParam(required = false) String city,
                                   @RequestParam(required = false) String district,
                                   @RequestParam(required = false) BigDecimal latitude,
                                   @RequestParam(required = false) BigDecimal longitude) {
        Shop shop = shopService.getById(shopId);
        return deliveryPricingService.quote(shop, city, district, latitude, longitude);
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
