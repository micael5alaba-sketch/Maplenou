package com.maplenou.support.ticket.adapter.in.web;

import com.maplenou.support.platform.security.CurrentActorProvider;
import com.maplenou.support.shared.Role;
import com.maplenou.support.ticket.application.VocabularyService;
import com.maplenou.support.ticket.domain.PermissionRule;
import com.maplenou.support.ticket.domain.PermissionScope;
import com.maplenou.support.ticket.domain.TicketAction;
import com.maplenou.support.ticket.domain.vocabulary.PriorityDefinition;
import com.maplenou.support.ticket.domain.vocabulary.StatusDefinition;
import com.maplenou.support.ticket.domain.vocabulary.StatusKind;
import com.maplenou.support.ticket.domain.vocabulary.Transition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

/** The configurable vocabulary (DEC-CS11-02): read by every client, edited by administrators. */
@RestController
@Tag(name = "Vocabulary")
public class VocabularyController {

    private final VocabularyService vocabulary;
    private final CurrentActorProvider currentUser;

    public VocabularyController(VocabularyService vocabulary, CurrentActorProvider currentUser) {
        this.vocabulary = vocabulary;
        this.currentUser = currentUser;
    }

    @Operation(summary = "Statuses (with kind and FR/EN labels), manual transitions and priorities")
    @GetMapping("/api/v1/support/vocabulary")
    public ResponseEntity<VocabularyService.Vocabulary> get() {
        VocabularyService.Vocabulary body = vocabulary.vocabulary();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePrivate())
                .eTag(Integer.toHexString(body.hashCode()))
                .body(body);
    }

    public record StatusInput(@NotBlank @Size(max = 25) String code, @NotNull StatusKind kind,
                              @NotBlank @Size(max = 60) String labelFr, @Size(max = 60) String labelEn,
                              @Min(0) @Max(999) int sortOrder, boolean slaPaused, boolean active) {
    }

    public record TransitionInput(@NotBlank String from, @NotBlank String to) {
    }

    public record WorkflowInput(@NotNull @Size(min = 6, max = 30) List<@Valid StatusInput> statuses,
                                @NotNull @Size(max = 200) List<@Valid TransitionInput> transitions) {
    }

    @Operation(summary = "Replace statuses and transitions (administrators); the lifecycle rules are checked")
    @PutMapping("/api/v1/support/admin/workflow")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public VocabularyService.Vocabulary workflow(@Valid @RequestBody WorkflowInput body) {
        List<StatusDefinition> statuses = body.statuses().stream().map(s -> new StatusDefinition(
                s.code().trim().toUpperCase(), s.kind(), s.labelFr(), s.labelEn(), s.sortOrder(), s.slaPaused(), false,
                s.active())).toList();
        List<Transition> transitions = body.transitions().stream()
                .map(t -> new Transition(t.from().trim().toUpperCase(), t.to().trim().toUpperCase())).toList();
        return vocabulary.replaceWorkflow(currentUser.get(), statuses, transitions);
    }

    public record PriorityInput(@NotBlank @Size(max = 20) String code, @Min(1) @Max(99) int rank,
                                @NotBlank @Size(max = 40) String labelFr, @Size(max = 40) String labelEn,
                                boolean requesterSelectable, boolean defaultPriority, boolean active) {
    }

    @Operation(summary = "Replace the priorities (administrators); new priorities get default SLA targets")
    @PutMapping("/api/v1/support/admin/priorities")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public VocabularyService.Vocabulary priorities(@Valid @RequestBody @Size(min = 1, max = 20) List<@Valid PriorityInput> body) {
        return vocabulary.replacePriorities(currentUser.get(), body.stream().map(p -> new PriorityDefinition(
                p.code().trim().toUpperCase(), p.rank(), p.labelFr(), p.labelEn(), p.requesterSelectable(),
                p.defaultPriority(), false, p.active())).toList());
    }

    public record PermissionInput(@NotNull Role role, @NotNull TicketAction action, @NotNull PermissionScope scope) {
    }

    public record PermissionsView(List<PermissionRule> rules, List<PermissionRule> defaults) {
    }

    @GetMapping("/api/v1/support/admin/permissions")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public PermissionsView permissions() {
        return new PermissionsView(vocabulary.permissionRules(currentUser.get()), vocabulary.defaultPermissionRules());
    }

    @Operation(summary = "Replace the permission matrix (administrators); safety rules are checked")
    @PutMapping("/api/v1/support/admin/permissions")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public PermissionsView replacePermissions(@Valid @RequestBody @Size(max = 500) List<@Valid PermissionInput> body) {
        List<PermissionRule> rules = vocabulary.replacePermissionRules(currentUser.get(),
                body.stream().map(p -> new PermissionRule(p.role(), p.action(), p.scope())).toList());
        return new PermissionsView(rules, vocabulary.defaultPermissionRules());
    }
}
