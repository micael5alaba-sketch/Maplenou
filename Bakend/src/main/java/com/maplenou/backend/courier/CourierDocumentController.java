package com.maplenou.backend.courier;

import com.maplenou.backend.courier.dto.CourierDocumentResponse;
import com.maplenou.backend.courier.dto.ReviewCourierDocumentRequest;
import com.maplenou.backend.courier.dto.SubmitCourierDocumentRequest;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Documents livreur")
@SecurityRequirement(name = "bearerAuth")
public class CourierDocumentController {

    private final CourierDocumentService courierDocumentService;

    // ----- Livreur -----

    /** Les 4 documents attendus, ceux non encore envoyés sont simplement absents de la liste. */
    @GetMapping("/api/delivery-agents/me/documents")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    public List<CourierDocumentResponse> listMine(@AuthenticationPrincipal User currentUser) {
        return courierDocumentService.listMine(currentUser);
    }

    /** Envoi ou remplacement d'un document (repasse toujours en PENDING). */
    @PostMapping("/api/delivery-agents/me/documents")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    @ResponseStatus(HttpStatus.CREATED)
    public CourierDocumentResponse submit(@AuthenticationPrincipal User currentUser,
                                          @Valid @RequestBody SubmitCourierDocumentRequest request) {
        return courierDocumentService.submit(currentUser, request);
    }

    // ----- Admin -----

    /** File d'attente de vérification, la plus ancienne soumission en premier. */
    @GetMapping("/api/admin/courier-documents")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<CourierDocumentResponse> listPending(@PageableDefault(size = 20) Pageable pageable) {
        return courierDocumentService.listPendingForAdmin(pageable);
    }

    @PatchMapping("/api/admin/courier-documents/{id}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public CourierDocumentResponse review(@PathVariable UUID id,
                                          @Valid @RequestBody ReviewCourierDocumentRequest request,
                                          @AuthenticationPrincipal User admin,
                                          HttpServletRequest httpRequest) {
        return courierDocumentService.review(id, request, admin, resolveIp(httpRequest));
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
