package com.maplenou.backend.revenue;

import com.maplenou.backend.revenue.dto.RevenueEntryResponse;
import com.maplenou.backend.revenue.dto.RevenueSource;
import com.maplenou.backend.revenue.dto.RevenueSummaryResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneOffset;

/** Registre des revenus de la plateforme (Super Admin et Auditeur : permission REVENUE_READ). */
@RestController
@RequiredArgsConstructor
@Tag(name = "Revenus de la plateforme")
@RequestMapping("/api/admin/revenue")
@PreAuthorize("hasAuthority('REVENUE_READ')")
@SecurityRequirement(name = "bearerAuth")
public class RevenueController {

    private final RevenueService revenueService;

    /** Totaux par source et série journalière sur la période (30 derniers jours par défaut). */
    @GetMapping
    public RevenueSummaryResponse summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now(ZoneOffset.UTC);
        LocalDate start = from != null ? from : end.minusDays(29);
        return revenueService.summary(start, end);
    }

    /** Lignes du registre (source, référence, montant, date), plus récentes d'abord. */
    @GetMapping("/entries")
    public Page<RevenueEntryResponse> entries(
            @RequestParam RevenueSource source,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20) Pageable pageable) {
        LocalDate end = to != null ? to : LocalDate.now(ZoneOffset.UTC);
        LocalDate start = from != null ? from : end.minusDays(29);
        return revenueService.entries(source, start, end, pageable);
    }
}
