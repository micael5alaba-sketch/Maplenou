package com.maplenou.backend.content;

import com.maplenou.backend.content.dto.ContentPageResponse;
import com.maplenou.backend.content.dto.CreateContentPageRequest;
import com.maplenou.backend.content.dto.UpdateContentPageRequest;
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
@Tag(name = "Pages de contenu (Admin)")
@RequestMapping("/api/admin/pages")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminContentPageController {

    private final ContentPageService contentPageService;

    @GetMapping
    public Page<ContentPageResponse> listAll(@PageableDefault(size = 20) Pageable pageable) {
        return contentPageService.listAll(pageable);
    }

    @GetMapping("/{id}")
    public ContentPageResponse getById(@PathVariable UUID id) {
        return contentPageService.getById(id);
    }

    @PostMapping
    public ResponseEntity<ContentPageResponse> create(@Valid @RequestBody CreateContentPageRequest request,
                                                        @AuthenticationPrincipal User actor,
                                                        HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(contentPageService.create(request, actor.getId(), httpRequest.getRemoteAddr()));
    }

    @PutMapping("/{id}")
    public ContentPageResponse update(@PathVariable UUID id,
                                       @Valid @RequestBody UpdateContentPageRequest request,
                                       @AuthenticationPrincipal User actor,
                                       HttpServletRequest httpRequest) {
        return contentPageService.update(id, request, actor.getId(), httpRequest.getRemoteAddr());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id,
                        @AuthenticationPrincipal User actor,
                        HttpServletRequest httpRequest) {
        contentPageService.delete(id, actor.getId(), httpRequest.getRemoteAddr());
    }
}
