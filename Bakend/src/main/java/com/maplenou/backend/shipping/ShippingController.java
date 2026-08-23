package com.maplenou.backend.shipping;

import com.maplenou.backend.shipping.dto.CarrierTrackingResponse;
import com.maplenou.backend.shipping.dto.ShipmentLabelResponse;
import com.maplenou.backend.user.User;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Bordereau d'expédition et suivi via le transporteur tiers assurant le transport principal (§3.2/§4.3). */
@RestController
@RequiredArgsConstructor
@Tag(name = "Transporteur")
@RequestMapping("/api/shops/mine/sub-orders/{subOrderId}/shipping")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('SELLER')")
public class ShippingController {

    private final ShippingService shippingService;

    @PostMapping("/label")
    public ShipmentLabelResponse generateLabel(@AuthenticationPrincipal User currentUser,
                                                @PathVariable UUID subOrderId) {
        return shippingService.generateLabel(currentUser, subOrderId);
    }

    @GetMapping("/tracking")
    public CarrierTrackingResponse track(@AuthenticationPrincipal User currentUser,
                                          @PathVariable UUID subOrderId) {
        return shippingService.trackShipment(currentUser, subOrderId);
    }
}
