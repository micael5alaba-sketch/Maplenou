package com.maplenou.backend.newsletter;

import com.maplenou.backend.newsletter.dto.SubscriberResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Newsletter (Admin)")
@RequestMapping("/api/admin/newsletter")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminNewsletterController {

    private final NewsletterService newsletterService;

    /** Liste des abonnés actifs, pour export vers l'outil d'emailing choisi. */
    @GetMapping("/subscribers")
    public Page<SubscriberResponse> listActive(@PageableDefault(size = 50) Pageable pageable) {
        return newsletterService.listActive(pageable);
    }
}
