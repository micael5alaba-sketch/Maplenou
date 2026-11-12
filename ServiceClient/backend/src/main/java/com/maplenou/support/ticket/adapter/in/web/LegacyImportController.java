package com.maplenou.support.ticket.adapter.in.web;

import com.maplenou.support.platform.security.CurrentActorProvider;
import com.maplenou.support.ticket.application.LegacyImportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Imports the former Maplenou support tickets (Bakend V46) — safe to run again, already imported ones are skipped. */
@RestController
@Tag(name = "Administration")
public class LegacyImportController {

    private final LegacyImportService importer;
    private final CurrentActorProvider currentUser;

    public LegacyImportController(LegacyImportService importer, CurrentActorProvider currentUser) {
        this.importer = importer;
        this.currentUser = currentUser;
    }

    @PostMapping("/api/v1/support/admin/legacy-import")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public LegacyImportService.ImportResult importLegacyTickets() {
        return importer.importAll(currentUser.get());
    }
}
