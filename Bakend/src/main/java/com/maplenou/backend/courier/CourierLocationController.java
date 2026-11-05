package com.maplenou.backend.courier;

import com.maplenou.backend.courier.dto.UpdateCourierLocationRequest;
import com.maplenou.backend.user.User;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Position livreur")
@SecurityRequirement(name = "bearerAuth")
public class CourierLocationController {

    private final CourierLocationService courierLocationService;

    /** Ping de position du livreur connecté (app ouverte), pour la carte d'affectation admin. */
    @PutMapping("/api/delivery-agents/me/location")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateMine(@AuthenticationPrincipal User currentUser,
                           @Valid @RequestBody UpdateCourierLocationRequest request) {
        courierLocationService.update(currentUser, request);
    }
}
