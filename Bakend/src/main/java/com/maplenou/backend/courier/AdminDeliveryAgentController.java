package com.maplenou.backend.courier;

import com.maplenou.backend.courier.dto.CourierLocationResponse;
import com.maplenou.backend.courier.dto.CreateDeliveryAgentRequest;
import com.maplenou.backend.user.User;
import com.maplenou.backend.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Livreurs (admin)")
@RequestMapping("/api/admin/delivery-agents")
@SecurityRequirement(name = "bearerAuth")
public class AdminDeliveryAgentController {

    private final CourierAdminService courierAdminService;
    private final CourierLocationService courierLocationService;
    private final com.maplenou.backend.user.UserService userService;

    /** Dernière position connue des livreurs actifs — alimente la carte d'affectation de l'admin. */
    @GetMapping("/locations")
    @PreAuthorize("hasAuthority('COURIERS_READ')")
    public Page<CourierLocationResponse> locations(@PageableDefault(size = 100) Pageable pageable) {
        return courierLocationService.listForAdmin(pageable);
    }

    /** Livreurs (comptes DELIVERY_AGENT) : liste pour l'affectation des livraisons. */
    @GetMapping
    @PreAuthorize("hasAuthority('COURIERS_READ')")
    public Page<com.maplenou.backend.user.dto.AdminUserResponse> list(
            @org.springframework.web.bind.annotation.RequestParam(required = false) Boolean active,
            @PageableDefault(size = 100) Pageable pageable) {
        return userService.listUsers(com.maplenou.backend.user.Role.DELIVERY_AGENT, active, pageable)
                .map(com.maplenou.backend.user.dto.AdminUserResponse::from);
    }

    /** Création directe d'un compte livreur interne (cahier des charges §3.3, "option B"). */
    @PostMapping
    @PreAuthorize("hasAuthority('COURIERS_WRITE')")
    public ResponseEntity<UserResponse> create(@AuthenticationPrincipal User admin,
                                                @Valid @RequestBody CreateDeliveryAgentRequest request,
                                                HttpServletRequest httpRequest) {
        String ip = resolveIp(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(courierAdminService.createDeliveryAgent(request, admin, ip));
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
