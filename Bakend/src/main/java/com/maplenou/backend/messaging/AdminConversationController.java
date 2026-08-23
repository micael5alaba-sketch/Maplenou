package com.maplenou.backend.messaging;

import com.maplenou.backend.messaging.dto.ConversationResponse;
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
@Tag(name = "Messagerie (Admin)")
@RequestMapping("/api/admin/conversations")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminConversationController {

    private final MessagingService messagingService;

    @GetMapping("/support")
    public Page<ConversationResponse> listSupportConversations(@PageableDefault(size = 20) Pageable pageable) {
        return messagingService.listSupportConversations(pageable);
    }
}
