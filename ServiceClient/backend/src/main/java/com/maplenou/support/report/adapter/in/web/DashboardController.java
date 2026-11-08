package com.maplenou.support.report.adapter.in.web;

import com.maplenou.support.platform.security.CurrentActorProvider;
import com.maplenou.support.shared.SupportException;
import com.maplenou.support.ticket.application.TicketStatistics;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Support dashboard (screen B1) and basic report — read models computed by the ticket module. */
@RestController
@Tag(name = "Dashboard & reports")
public class DashboardController {

    private final TicketStatistics statistics;
    private final CurrentActorProvider currentUser;

    public DashboardController(TicketStatistics statistics, CurrentActorProvider currentUser) {
        this.statistics = statistics;
        this.currentUser = currentUser;
    }

    @GetMapping("/api/v1/support/dashboard")
    @PreAuthorize("hasAnyRole('SUPPORT_AGENT','SUPPORT_MANAGER','ADMINISTRATOR')")
    public TicketStatistics.Dashboard dashboard() {
        return statistics.dashboard(currentUser.get());
    }

    @GetMapping("/api/v1/support/reports/summary")
    @PreAuthorize("hasAnyRole('SUPPORT_MANAGER','ADMINISTRATOR')")
    public TicketStatistics.Report report(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > 366) {
            throw SupportException.badRequest("INVALID_PERIOD", "Période invalide (366 jours maximum).", null);
        }
        return statistics.report(from, to);
    }
}
