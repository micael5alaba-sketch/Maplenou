package com.maplenou.backend.delivery;

import com.maplenou.backend.catalog.Shop;
import com.maplenou.backend.catalog.ShopService;
import com.maplenou.backend.delivery.dto.CreateDeliveryPricingTierRequest;
import com.maplenou.backend.delivery.dto.DeliveryFeeQuote;
import com.maplenou.backend.delivery.dto.DeliveryPricingTierResponse;
import com.maplenou.backend.delivery.dto.UpdateDeliveryPricingTierRequest;
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
@RequestMapping("/api/delivery-pricing-tiers")
@RequiredArgsConstructor
@Tag(name = "Tarification livraison (paliers de distance)")
public class DeliveryPricingController {

    private final DeliveryPricingTierService tierService;
    private final DeliveryPricingService deliveryPricingService;
    private final ShopService shopService;

    /** Public : le barème complet, utilisé pour afficher une estimation avant adresse choisie. */
    @GetMapping
    public List<DeliveryPricingTierResponse> list() {
        return tierService.list();
    }

    /** Admin : créer un palier. */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<DeliveryPricingTierResponse> create(@Valid @RequestBody CreateDeliveryPricingTierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tierService.create(request));
    }

    /** Admin : modifier un palier existant (distance max et/ou prix). */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public DeliveryPricingTierResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateDeliveryPricingTierRequest request) {
        return tierService.update(id, request);
    }

    /** Admin : supprimer un palier. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        tierService.delete(id);
    }

    /**
     * Devis de livraison avant paiement (checkout) — même calcul et mêmes chiffres que
     * ceux réellement figés sur le SubOrder par {@code OrderService.placeOrder}, pour que
     * ce que l'acheteur voit corresponde exactement à ce qui sera facturé.
     */
    @GetMapping("/quote")
    public DeliveryFeeQuote quote(@RequestParam UUID shopId,
                                   @RequestParam(required = false) BigDecimal latitude,
                                   @RequestParam(required = false) BigDecimal longitude) {
        Shop shop = shopService.getById(shopId);
        return deliveryPricingService.quote(shop, latitude, longitude);
    }
}
