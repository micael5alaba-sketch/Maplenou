package com.maplenou.backend.delivery;

import com.maplenou.backend.delivery.dto.CourierWithdrawalResponse;
import com.maplenou.backend.delivery.dto.RequestCourierWithdrawalRequest;
import com.maplenou.backend.payout.PayoutStatus;
import com.maplenou.backend.user.User;
import com.maplenou.backend.withdrawal.WithdrawalAction;
import com.maplenou.backend.withdrawal.dto.WithdrawalActionRequest;
import com.maplenou.backend.withdrawal.dto.WithdrawalHistoryResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Retraits livreurs")
@SecurityRequirement(name = "bearerAuth")
public class CourierWithdrawalController {

    private final CourierWithdrawalService service;

    // ----- Livreur -----

    @PostMapping("/api/delivery-agents/me/withdrawals")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    @ResponseStatus(HttpStatus.CREATED)
    public CourierWithdrawalResponse request(@AuthenticationPrincipal User agent,
                                             @Valid @RequestBody RequestCourierWithdrawalRequest body) {
        return service.request(agent, body);
    }

    @GetMapping("/api/delivery-agents/me/withdrawals")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    public Page<CourierWithdrawalResponse> listMine(@AuthenticationPrincipal User agent,
                                                    @PageableDefault(size = 20) Pageable pageable) {
        return service.listMine(agent, pageable);
    }

    // ----- Personnel : lecture -----

    @GetMapping("/api/admin/courier-withdrawals")
    @PreAuthorize("hasAuthority('PAYOUTS_READ')")
    public Page<CourierWithdrawalResponse> listAll(@RequestParam(required = false) PayoutStatus status,
                                                   @PageableDefault(size = 20) Pageable pageable) {
        return service.listAll(status, pageable);
    }

    @GetMapping("/api/admin/courier-withdrawals/{id}")
    @PreAuthorize("hasAuthority('PAYOUTS_READ')")
    public CourierWithdrawalResponse getById(@PathVariable UUID id) {
        return service.getById(id);
    }

    @GetMapping("/api/admin/courier-withdrawals/{id}/history")
    @PreAuthorize("hasAuthority('PAYOUTS_READ')")
    public List<WithdrawalHistoryResponse> history(@PathVariable UUID id) {
        return service.history(id);
    }

    // ----- Préparation (Agent support et retraits) -----

    @PatchMapping("/api/admin/courier-withdrawals/{id}/prepare")
    @PreAuthorize("hasAuthority('PAYOUTS_PREPARE')")
    public CourierWithdrawalResponse prepare(@PathVariable UUID id, @RequestBody(required = false) WithdrawalActionRequest body,
                                             @AuthenticationPrincipal User actor, HttpServletRequest request) {
        return service.transition(id, WithdrawalAction.PREPARE, body, actor, request.getRemoteAddr());
    }

    @PatchMapping("/api/admin/courier-withdrawals/{id}/return")
    @PreAuthorize("hasAuthority('PAYOUTS_PREPARE')")
    public CourierWithdrawalResponse returnForCorrection(@PathVariable UUID id, @RequestBody WithdrawalActionRequest body,
                                                         @AuthenticationPrincipal User actor, HttpServletRequest request) {
        return service.transition(id, WithdrawalAction.RETURN_FOR_CORRECTION, body, actor, request.getRemoteAddr());
    }

    @PatchMapping("/api/admin/courier-withdrawals/{id}/submit")
    @PreAuthorize("hasAuthority('PAYOUTS_PREPARE')")
    public CourierWithdrawalResponse submit(@PathVariable UUID id, @RequestBody(required = false) WithdrawalActionRequest body,
                                            @AuthenticationPrincipal User actor, HttpServletRequest request) {
        return service.transition(id, WithdrawalAction.SUBMIT, body, actor, request.getRemoteAddr());
    }

    // ----- Décision et exécution (Super Admin uniquement) -----

    @PostMapping("/api/admin/courier-withdrawals/{id}/approve")
    @PreAuthorize("hasAuthority('PAYOUTS_APPROVE')")
    public CourierWithdrawalResponse approve(@PathVariable UUID id, @RequestBody WithdrawalActionRequest body,
                                             @AuthenticationPrincipal User actor, HttpServletRequest request) {
        return service.transition(id, WithdrawalAction.APPROVE, body, actor, request.getRemoteAddr());
    }

    @PatchMapping("/api/admin/courier-withdrawals/{id}/reject")
    @PreAuthorize("hasAuthority('PAYOUTS_APPROVE')")
    public CourierWithdrawalResponse reject(@PathVariable UUID id, @RequestBody WithdrawalActionRequest body,
                                            @AuthenticationPrincipal User actor, HttpServletRequest request) {
        return service.transition(id, WithdrawalAction.REJECT, body, actor, request.getRemoteAddr());
    }

    @PatchMapping("/api/admin/courier-withdrawals/{id}/processing")
    @PreAuthorize("hasAuthority('PAYOUTS_APPROVE')")
    public CourierWithdrawalResponse processing(@PathVariable UUID id, @RequestBody(required = false) WithdrawalActionRequest body,
                                                @AuthenticationPrincipal User actor, HttpServletRequest request) {
        return service.transition(id, WithdrawalAction.PROCESS, body, actor, request.getRemoteAddr());
    }

    @PatchMapping("/api/admin/courier-withdrawals/{id}/paid")
    @PreAuthorize("hasAuthority('PAYOUTS_APPROVE')")
    public CourierWithdrawalResponse paid(@PathVariable UUID id, @RequestBody(required = false) WithdrawalActionRequest body,
                                          @AuthenticationPrincipal User actor, HttpServletRequest request) {
        return service.transition(id, WithdrawalAction.PAY, body, actor, request.getRemoteAddr());
    }

    @PatchMapping("/api/admin/courier-withdrawals/{id}/failed")
    @PreAuthorize("hasAuthority('PAYOUTS_APPROVE')")
    public CourierWithdrawalResponse failed(@PathVariable UUID id, @RequestBody WithdrawalActionRequest body,
                                            @AuthenticationPrincipal User actor, HttpServletRequest request) {
        return service.transition(id, WithdrawalAction.FAIL, body, actor, request.getRemoteAddr());
    }
}
