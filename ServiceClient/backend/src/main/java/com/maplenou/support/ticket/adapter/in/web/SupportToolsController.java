package com.maplenou.support.ticket.adapter.in.web;

import com.maplenou.support.platform.security.CurrentActorProvider;
import com.maplenou.support.shared.Actor;
import com.maplenou.support.shared.Role;
import com.maplenou.support.ticket.application.AiAssistService;
import com.maplenou.support.ticket.application.ExternalActionService;
import com.maplenou.support.ticket.application.TicketCommands;
import com.maplenou.support.ticket.application.TicketQueryService;
import com.maplenou.support.ticket.application.TicketSettings;
import com.maplenou.support.ticket.application.view.TicketViews.TicketDetail;
import com.maplenou.support.ticket.application.view.TicketViews.TicketSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Support tools on a ticket: actions on Maplenou, AI help, CSV export. */
@RestController
@Tag(name = "Support tools")
public class SupportToolsController {

    private final ExternalActionService actions;
    private final AiAssistService ai;
    private final TicketQueryService queries;
    private final TicketSettings settings;
    private final CurrentActorProvider currentUser;

    public SupportToolsController(ExternalActionService actions, AiAssistService ai, TicketQueryService queries,
                                  TicketSettings settings, CurrentActorProvider currentUser) {
        this.actions = actions;
        this.ai = ai;
        this.queries = queries;
        this.settings = settings;
        this.currentUser = currentUser;
    }

    // ---------------------------------------------------------------- actions on Maplenou (DEC-CS11-07)

    @Operation(summary = "Ask Maplenou to open a return/refund request for the order of the ticket")
    @PostMapping("/api/v1/support/tickets/{id}/actions/refund-request")
    public TicketDetail refund(@PathVariable UUID id, @Valid @RequestBody TicketRequests.RequestRefund body) {
        Actor user = currentUser.get();
        actions.requestRefund(user, id,
                new TicketCommands.RequestRefund(body.subOrderId(), body.reason(), body.productCondition()));
        return queries.detail(user, id);
    }

    @Operation(summary = "Release the delivery of the ticket so that a courier can take it again")
    @PostMapping("/api/v1/support/tickets/{id}/actions/reschedule-delivery")
    public TicketDetail reschedule(@PathVariable UUID id, @Valid @RequestBody TicketRequests.RescheduleDelivery body) {
        Actor user = currentUser.get();
        actions.rescheduleDelivery(user, id, body.reason());
        return queries.detail(user, id);
    }

    @Operation(summary = "Send a message to the courier of the delivery, in the Maplenou app")
    @PostMapping("/api/v1/support/tickets/{id}/actions/contact-driver")
    public TicketDetail contactDriver(@PathVariable UUID id, @Valid @RequestBody TicketRequests.ContactDriver body) {
        Actor user = currentUser.get();
        actions.contactDriver(user, id, body.message());
        return queries.detail(user, id);
    }

    // ---------------------------------------------------------------- AI help (DEC-CS11-08)

    public record AiStatus(boolean available) {
    }

    @GetMapping("/api/v1/support/ai/status")
    @PreAuthorize("hasAnyRole('SUPPORT_AGENT','SUPPORT_MANAGER','ADMINISTRATOR')")
    public AiStatus aiStatus() {
        return new AiStatus(ai.available());
    }

    @Operation(summary = "Apply (true) or dismiss (false) the category/priority suggested by the assistant")
    @PostMapping("/api/v1/support/tickets/{id}/ai/suggestion")
    public TicketDetail decideSuggestion(@PathVariable UUID id, @Valid @RequestBody TicketRequests.AiDecision body) {
        Actor user = currentUser.get();
        ai.decide(user, id, body.apply());
        return queries.detail(user, id);
    }

    @Operation(summary = "A reply draft from the help centre; the agent edits it before sending")
    @PostMapping("/api/v1/support/tickets/{id}/ai/draft-reply")
    public AiAssistService.DraftView draft(@PathVariable UUID id,
                                           @Valid @RequestBody(required = false) TicketRequests.DraftRequest body) {
        return ai.draftReply(currentUser.get(), id, body == null ? null : body.language());
    }

    // ---------------------------------------------------------------- CSV export (mockup 14)

    @Operation(summary = "CSV export of the tickets matching the filters (managers and administrators)")
    @GetMapping(value = "/api/v1/support/tickets/export.csv", produces = "text/csv")
    @PreAuthorize("hasAnyRole('SUPPORT_MANAGER','ADMINISTRATOR')")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Set<String> status,
            @RequestParam(required = false) Set<String> priority,
            @RequestParam(required = false) Set<String> category,
            @RequestParam(required = false) String assignee,
            @RequestParam(required = false) UUID requesterId,
            @RequestParam(required = false) Role requesterRole,
            @RequestParam(required = false) Boolean escalated,
            @RequestParam(required = false) Boolean slaBreached,
            @RequestParam(required = false) Boolean open,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo,
            @RequestParam(defaultValue = "fr") String lang) {
        List<TicketSummary> rows = queries.export(currentUser.get(), TicketController.criteria(q, status, priority,
                category, assignee, requesterId, requesterRole, escalated, slaBreached, open, createdFrom, createdTo),
                settings.exportLimit());
        byte[] csv = CsvWriter.tickets(rows, "en".equals(lang), ZoneId.of("Africa/Lome"));
        String filename = "tickets-" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".csv";
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(csv);
    }
}
