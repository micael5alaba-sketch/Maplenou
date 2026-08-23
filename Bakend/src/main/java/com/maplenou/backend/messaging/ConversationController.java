package com.maplenou.backend.messaging;

import com.maplenou.backend.messaging.dto.ConversationResponse;
import com.maplenou.backend.messaging.dto.MessageResponse;
import com.maplenou.backend.messaging.dto.SendMessageRequest;
import com.maplenou.backend.user.User;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Messagerie")
@RequestMapping("/api/conversations")
@SecurityRequirement(name = "bearerAuth")
public class ConversationController {

    private final MessagingService messagingService;

    @PostMapping("/shops/{shopId}")
    public ResponseEntity<ConversationResponse> startWithShop(@AuthenticationPrincipal User currentUser,
                                                                @PathVariable UUID shopId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(messagingService.startOrGetShopConversation(currentUser, shopId));
    }

    @PostMapping("/support")
    public ResponseEntity<ConversationResponse> startSupport(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(messagingService.startOrGetSupportConversation(currentUser));
    }

    @GetMapping
    public Page<ConversationResponse> listMine(@AuthenticationPrincipal User currentUser,
                                                @PageableDefault(size = 20) Pageable pageable) {
        return messagingService.listMyConversations(currentUser, pageable);
    }

    @GetMapping("/{conversationId}/messages")
    public Page<MessageResponse> listMessages(@AuthenticationPrincipal User currentUser,
                                               @PathVariable UUID conversationId,
                                               @PageableDefault(size = 30) Pageable pageable) {
        return messagingService.listMessages(currentUser, conversationId, pageable);
    }

    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<MessageResponse> sendMessage(@AuthenticationPrincipal User currentUser,
                                                        @PathVariable UUID conversationId,
                                                        @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(messagingService.sendMessage(currentUser, conversationId, request));
    }

    @PatchMapping("/{conversationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAsRead(@AuthenticationPrincipal User currentUser, @PathVariable UUID conversationId) {
        messagingService.markAsRead(currentUser, conversationId);
    }
}
