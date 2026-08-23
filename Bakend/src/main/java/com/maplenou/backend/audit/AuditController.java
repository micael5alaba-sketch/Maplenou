package com.maplenou.backend.audit;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/audit")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Audit (Admin)")
@SecurityRequirement(name = "bearerAuth")
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    public Page<AuditLogResponse> listAll(@PageableDefault(size = 50) Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(AuditLogResponse::from);
    }

    @GetMapping("/actor/{actorId}")
    public Page<AuditLogResponse> byActor(@PathVariable UUID actorId,
                                           @PageableDefault(size = 50) Pageable pageable) {
        return auditLogRepository.findByActorIdOrderByCreatedAtDesc(actorId, pageable)
                .map(AuditLogResponse::from);
    }

    @GetMapping("/entity/{entityId}")
    public Page<AuditLogResponse> byEntity(@PathVariable UUID entityId,
                                            @PageableDefault(size = 50) Pageable pageable) {
        return auditLogRepository.findByEntityIdOrderByCreatedAtDesc(entityId, pageable)
                .map(AuditLogResponse::from);
    }
}
