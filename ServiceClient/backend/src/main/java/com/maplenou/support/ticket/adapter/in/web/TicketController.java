package com.maplenou.support.ticket.adapter.in.web;

import com.maplenou.support.attachment.domain.AttachmentPolicy.IncomingFile;
import com.maplenou.support.platform.security.CurrentActorProvider;
import com.maplenou.support.shared.Actor;
import com.maplenou.support.shared.PageQuery;
import com.maplenou.support.shared.PageResult;
import com.maplenou.support.shared.Role;
import com.maplenou.support.shared.SupportException;
import com.maplenou.support.ticket.application.TicketCommandService;
import com.maplenou.support.ticket.application.TicketCommands;
import com.maplenou.support.ticket.application.TicketQueryService;
import com.maplenou.support.ticket.application.TicketSearchCriteria;
import com.maplenou.support.ticket.application.view.TicketViews.HistoryView;
import com.maplenou.support.ticket.application.view.TicketViews.MessageView;
import com.maplenou.support.ticket.application.view.TicketViews.ReopenView;
import com.maplenou.support.ticket.application.view.TicketViews.SatisfactionView;
import com.maplenou.support.ticket.application.view.TicketViews.TicketDetail;
import com.maplenou.support.ticket.application.view.TicketViews.TicketSummary;
import com.maplenou.support.ticket.domain.MessageVisibility;
import com.maplenou.support.ticket.domain.ReopenRequest;
import com.maplenou.support.ticket.domain.Satisfaction;
import com.maplenou.support.ticket.domain.Ticket;
import com.maplenou.support.ticket.domain.TicketMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Ticket API — docs/architecture/api-specification.md. */
@RestController
@RequestMapping("/api/v1/support/tickets")
@Tag(name = "Tickets")
public class TicketController {

    private final TicketCommandService commands;
    private final TicketQueryService queries;
    private final CurrentActorProvider currentUser;

    public TicketController(TicketCommandService commands, TicketQueryService queries, CurrentActorProvider currentUser) {
        this.commands = commands;
        this.queries = queries;
        this.currentUser = currentUser;
    }

    // ---------------------------------------------------------------- create

    @Operation(summary = "Create a ticket (JSON, without attachments)")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TicketDetail> create(@Valid @RequestBody TicketRequests.CreateTicket body) {
        return created(body, List.of());
    }

    @Operation(summary = "Create a ticket with up to 5 attachments (multipart: 'ticket' JSON part + 'files')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TicketDetail> createMultipart(
            @Valid @RequestPart("ticket") TicketRequests.CreateTicket body,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return created(body, toIncoming(files));
    }

    private ResponseEntity<TicketDetail> created(TicketRequests.CreateTicket b, List<IncomingFile> files) {
        Actor user = currentUser.get();
        Ticket t = commands.create(user, new TicketCommands.CreateTicket(b.subject(), b.description(), b.categoryCode(),
                b.priority(), b.urgent(), b.orderRef(), b.deliveryRef(), files));
        return ResponseEntity.created(URI.create("/api/v1/support/tickets/" + t.id())).body(queries.detail(user, t.id()));
    }

    // ---------------------------------------------------------------- read

    @Operation(summary = "List / search / filter tickets (requesters only get their own)")
    @GetMapping
    public PageResult<TicketSummary> list(
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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        TicketSearchCriteria criteria = criteria(q, status, priority, category, assignee, requesterId, requesterRole,
                escalated, slaBreached, open, createdFrom, createdTo);
        return queries.search(currentUser.get(), criteria, new PageQuery(page, size), sort);
    }

    static TicketSearchCriteria criteria(String q, Set<String> status, Set<String> priority, Set<String> category,
                                         String assignee, UUID requesterId, Role requesterRole, Boolean escalated,
                                         Boolean slaBreached, Boolean open, LocalDate createdFrom, LocalDate createdTo) {
        if (assignee != null && !assignee.isBlank() && !"me".equalsIgnoreCase(assignee)
                && !"unassigned".equalsIgnoreCase(assignee)) {
            try {
                UUID.fromString(assignee);
            } catch (IllegalArgumentException e) {
                throw SupportException.badRequest("INVALID_PARAMETER",
                        "assignee doit être un identifiant, 'me' ou 'unassigned'.", null);
            }
        }
        return new TicketSearchCriteria(q, status, priority, category, assignee, requesterId, requesterRole, escalated,
                slaBreached, open, createdFrom, createdTo, null, null);
    }

    @GetMapping("/{id}")
    public TicketDetail get(@PathVariable UUID id) {
        return queries.detail(currentUser.get(), id);
    }

    @Operation(summary = "Conversation. Support may pass visibility=PUBLIC|INTERNAL; requesters only get PUBLIC")
    @GetMapping("/{id}/messages")
    public List<MessageView> messages(@PathVariable UUID id,
                                      @RequestParam(required = false) MessageVisibility visibility) {
        return queries.messages(currentUser.get(), id, visibility);
    }

    @GetMapping("/{id}/history")
    public List<HistoryView> history(@PathVariable UUID id) {
        return queries.history(currentUser.get(), id);
    }

    @Operation(summary = "Support only: requester, order, payment, delivery, seller and refund context of a ticket")
    @GetMapping("/{id}/context")
    public TicketQueryService.TicketContext context(@PathVariable UUID id) {
        return queries.context(currentUser.get(), id);
    }

    @Operation(summary = "Download an attachment (authorized users only)")
    @GetMapping("/{id}/attachments/{attachmentId}")
    public ResponseEntity<Resource> download(@PathVariable UUID id, @PathVariable UUID attachmentId) {
        TicketQueryService.AttachmentDownload d = queries.download(currentUser.get(), id, attachmentId);
        try {
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(d.attachment().contentType()))
                    .contentLength(d.attachment().sizeBytes())
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename(d.attachment().filename(), StandardCharsets.UTF_8).build().toString())
                    .header("X-Content-Type-Options", "nosniff")
                    .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                    .body(new InputStreamResource(d.content().open()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ---------------------------------------------------------------- conversation

    @PostMapping(path = "/{id}/messages", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MessageView postMessage(@PathVariable UUID id, @Valid @RequestBody TicketRequests.PostMessage body) {
        return post(id, body, null, MessageVisibility.PUBLIC);
    }

    @PostMapping(path = "/{id}/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MessageView postMessageMultipart(@PathVariable UUID id,
                                            @Valid @RequestPart("message") TicketRequests.PostMessage body,
                                            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return post(id, body, files, MessageVisibility.PUBLIC);
    }

    @PostMapping(path = "/{id}/internal-notes", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MessageView postNote(@PathVariable UUID id, @Valid @RequestBody TicketRequests.PostMessage body) {
        return post(id, body, null, MessageVisibility.INTERNAL);
    }

    @PostMapping(path = "/{id}/internal-notes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MessageView postNoteMultipart(@PathVariable UUID id,
                                         @Valid @RequestPart("message") TicketRequests.PostMessage body,
                                         @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return post(id, body, files, MessageVisibility.INTERNAL);
    }

    private MessageView post(UUID id, TicketRequests.PostMessage body, List<MultipartFile> files,
                             MessageVisibility visibility) {
        Actor user = currentUser.get();
        TicketMessage m = commands.postMessage(user, id,
                new TicketCommands.PostMessage(body.body(), body.clientMessageId(), toIncoming(files)), visibility);
        return queries.message(user, id, m.id());
    }

    // ---------------------------------------------------------------- support actions

    @PutMapping("/{id}/status")
    public TicketDetail changeStatus(@PathVariable UUID id, @Valid @RequestBody TicketRequests.ChangeStatus body) {
        Actor user = currentUser.get();
        commands.changeStatus(user, id, new TicketCommands.ChangeStatus(body.status(), body.comment()));
        return queries.detail(user, id);
    }

    @PutMapping("/{id}/priority")
    public TicketDetail changePriority(@PathVariable UUID id, @Valid @RequestBody TicketRequests.ChangePriority body) {
        Actor user = currentUser.get();
        commands.changePriority(user, id, new TicketCommands.ChangePriority(body.priority(), body.reason()));
        return queries.detail(user, id);
    }

    @PutMapping("/{id}/category")
    public TicketDetail changeCategory(@PathVariable UUID id, @Valid @RequestBody TicketRequests.ChangeCategory body) {
        Actor user = currentUser.get();
        commands.changeCategory(user, id, body.categoryCode());
        return queries.detail(user, id);
    }

    @Operation(summary = "Assign: empty body or agentId=null assigns to the caller; another agent requires a manager")
    @PutMapping("/{id}/assign")
    public TicketDetail assign(@PathVariable UUID id, @RequestBody(required = false) TicketRequests.Assign body) {
        Actor user = currentUser.get();
        commands.assign(user, id, new TicketCommands.Assign(body == null ? null : body.agentId()));
        return queries.detail(user, id);
    }

    @PostMapping("/{id}/escalate")
    public TicketDetail escalate(@PathVariable UUID id, @Valid @RequestBody TicketRequests.Escalate body) {
        Actor user = currentUser.get();
        commands.escalate(user, id, body.reason());
        return queries.detail(user, id);
    }

    // ---------------------------------------------------------------- reopening and satisfaction

    @PostMapping("/{id}/reopen-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ReopenView requestReopen(@PathVariable UUID id, @Valid @RequestBody TicketRequests.RequestReopen body) {
        ReopenRequest r = commands.requestReopen(currentUser.get(), id, body.reason());
        return ReopenView.of(r);
    }

    @PutMapping("/{id}/reopen-requests/{requestId}")
    public TicketDetail decideReopen(@PathVariable UUID id, @PathVariable UUID requestId,
                                     @Valid @RequestBody TicketRequests.DecideReopen body) {
        Actor user = currentUser.get();
        commands.decideReopen(user, id, requestId,
                new TicketCommands.DecideReopen(body.decision(), body.note(), body.targetStatus()));
        return queries.detail(user, id);
    }

    @Operation(summary = "Requester: rate the answer of a resolved or closed ticket, once (1 to 5)")
    @PostMapping("/{id}/satisfaction")
    @ResponseStatus(HttpStatus.CREATED)
    public SatisfactionView rate(@PathVariable UUID id, @Valid @RequestBody TicketRequests.Rate body) {
        Satisfaction s = commands.rate(currentUser.get(), id, new TicketCommands.Rate(body.rating(), body.comment()));
        return new SatisfactionView(s.rating(), s.comment(), s.ratedAt());
    }

    // ---------------------------------------------------------------- helpers

    static List<IncomingFile> toIncoming(List<MultipartFile> files) {
        if (files == null) {
            return List.of();
        }
        return files.stream().filter(f -> !f.isEmpty() || f.getOriginalFilename() != null && !f.getOriginalFilename().isBlank())
                .map(f -> {
                    try {
                        return new IncomingFile(f.getOriginalFilename(), f.getBytes());
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }).toList();
    }
}
