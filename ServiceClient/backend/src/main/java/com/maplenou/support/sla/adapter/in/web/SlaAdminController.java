package com.maplenou.support.sla.adapter.in.web;

import com.maplenou.support.sla.application.SlaAdminService;
import com.maplenou.support.sla.application.SlaService;
import com.maplenou.support.sla.domain.Holiday;
import com.maplenou.support.sla.domain.SlaPolicy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Business calendar and SLA targets (DEC-001). Reading: managers and administrators; changes: administrators. */
@RestController
@Tag(name = "Administration")
public class SlaAdminController {

    private final SlaAdminService admin;
    private final SlaService sla;

    public SlaAdminController(SlaAdminService admin, SlaService sla) {
        this.admin = admin;
        this.sla = sla;
    }

    /** Counting mode of the SLA clock (always = 24/7, or business hours with holidays). */
    @GetMapping("/api/v1/support/admin/sla-settings")
    @PreAuthorize("hasAnyRole('SUPPORT_MANAGER','ADMINISTRATOR')")
    public SlaService.CalendarSettings slaSettings() {
        return sla.settings();
    }

    @GetMapping("/api/v1/support/admin/sla-policies")
    @PreAuthorize("hasAnyRole('SUPPORT_MANAGER','ADMINISTRATOR')")
    public List<SlaPolicy> policies() {
        return admin.policies();
    }

    public record PolicyUpdate(@Min(1) @Max(43200) int firstResponseMinutes, @Min(1) @Max(129600) Integer resolutionMinutes,
                               @Min(1) @Max(90) Integer resolutionDays) {
    }

    @Operation(summary = "Change the targets of one priority: first response in minutes, resolution in minutes or days")
    @PutMapping("/api/v1/support/admin/sla-policies/{priority}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public SlaPolicy updatePolicy(@PathVariable String priority, @Valid @RequestBody PolicyUpdate body) {
        return admin.updatePolicy(new SlaPolicy(priority, body.firstResponseMinutes(), body.resolutionMinutes(),
                body.resolutionDays()));
    }

    public record HolidayView(LocalDate date, String label) {
    }

    public record HolidayUpdate(@NotBlank @Size(max = 120) String label) {
    }

    @GetMapping("/api/v1/support/admin/holidays")
    @PreAuthorize("hasAnyRole('SUPPORT_MANAGER','ADMINISTRATOR')")
    public List<HolidayView> listHolidays() {
        return admin.holidays().stream().map(h -> new HolidayView(h.date(), h.label())).toList();
    }

    @PutMapping("/api/v1/support/admin/holidays/{date}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public HolidayView putHoliday(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                  @Valid @RequestBody HolidayUpdate body) {
        Holiday h = admin.putHoliday(date, body.label());
        return new HolidayView(h.date(), h.label());
    }

    @DeleteMapping("/api/v1/support/admin/holidays/{date}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHoliday(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        admin.deleteHoliday(date);
    }
}
