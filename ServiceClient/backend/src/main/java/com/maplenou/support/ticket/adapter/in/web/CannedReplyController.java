package com.maplenou.support.ticket.adapter.in.web;

import com.maplenou.support.platform.security.CurrentActorProvider;
import com.maplenou.support.ticket.application.CannedReplyService;
import com.maplenou.support.ticket.domain.CannedReply;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** « Réponses types » (mockup 13). */
@RestController
@Tag(name = "Canned replies")
public class CannedReplyController {

    private final CannedReplyService replies;
    private final CurrentActorProvider currentUser;

    public CannedReplyController(CannedReplyService replies, CurrentActorProvider currentUser) {
        this.replies = replies;
        this.currentUser = currentUser;
    }

    public record ReplyInput(@NotBlank @Size(max = 120) String title, @NotBlank @Size(max = 5000) String body,
                             @NotBlank @Pattern(regexp = "^(fr|en)$") String language,
                             @Size(max = 30) String categoryCode, boolean active) {
    }

    @GetMapping("/api/v1/support/canned-replies")
    @PreAuthorize("hasAnyRole('SUPPORT_AGENT','SUPPORT_MANAGER','ADMINISTRATOR')")
    public List<CannedReply> list(@RequestParam(defaultValue = "false") boolean all) {
        return replies.list(currentUser.get(), all);
    }

    @PostMapping("/api/v1/support/canned-replies")
    @PreAuthorize("hasAnyRole('SUPPORT_MANAGER','ADMINISTRATOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public CannedReply create(@Valid @RequestBody ReplyInput body) {
        return replies.save(currentUser.get(), null, body.title(), body.body(), body.language(), body.categoryCode(),
                body.active());
    }

    @PutMapping("/api/v1/support/canned-replies/{id}")
    @PreAuthorize("hasAnyRole('SUPPORT_MANAGER','ADMINISTRATOR')")
    public CannedReply update(@PathVariable UUID id, @Valid @RequestBody ReplyInput body) {
        return replies.save(currentUser.get(), id, body.title(), body.body(), body.language(), body.categoryCode(),
                body.active());
    }

    @DeleteMapping("/api/v1/support/canned-replies/{id}")
    @PreAuthorize("hasAnyRole('SUPPORT_MANAGER','ADMINISTRATOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        replies.delete(currentUser.get(), id);
    }
}
