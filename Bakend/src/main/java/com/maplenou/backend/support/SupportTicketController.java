package com.maplenou.backend.support;

import com.maplenou.backend.support.dto.CreateSupportTicketRequest;
import com.maplenou.backend.support.dto.ResolveSupportTicketRequest;
import com.maplenou.backend.support.dto.SupportTicketResponse;
import com.maplenou.backend.user.User;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Tickets de support")
@SecurityRequirement(name = "bearerAuth")
public class SupportTicketController {

    private final SupportTicketService supportTicketService;

    /** Tout compte connecté (livreur ou acheteur) peut créer un ticket. */
    @PostMapping("/api/support-tickets")
    public ResponseEntity<SupportTicketResponse> create(@AuthenticationPrincipal User requester,
                                                          @Valid @RequestBody CreateSupportTicketRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supportTicketService.create(requester, request));
    }

    @GetMapping("/api/support-tickets/mine")
    public Page<SupportTicketResponse> listMine(@AuthenticationPrincipal User requester,
                                                 @PageableDefault(size = 20) Pageable pageable) {
        return supportTicketService.listMine(requester, pageable);
    }

    // ----- Admin -----

    @GetMapping("/api/admin/support-tickets")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<SupportTicketResponse> listForAdmin(@RequestParam(required = false) SupportTicketStatus status,
                                                     @PageableDefault(size = 20) Pageable pageable) {
        return supportTicketService.listForAdmin(status, pageable);
    }

    @PatchMapping("/api/admin/support-tickets/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public SupportTicketResponse resolve(@PathVariable UUID id,
                                          @Valid @RequestBody ResolveSupportTicketRequest request,
                                          @AuthenticationPrincipal User admin,
                                          HttpServletRequest httpRequest) {
        return supportTicketService.resolve(id, request, admin, httpRequest.getRemoteAddr());
    }
}
