package com.maplenou.backend.messaging;

import com.maplenou.backend.catalog.Shop;
import com.maplenou.backend.catalog.ShopRepository;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.messaging.dto.ConversationResponse;
import com.maplenou.backend.messaging.dto.MessageResponse;
import com.maplenou.backend.messaging.dto.SendMessageRequest;
import com.maplenou.backend.notification.NotificationService;
import com.maplenou.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessagingService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ShopRepository shopRepository;
    private final NotificationService notificationService;

    @Transactional
    public ConversationResponse startOrGetShopConversation(User buyer, UUID shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Boutique introuvable"));

        if (shop.getOwner().getId().equals(buyer.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Impossible de démarrer une conversation avec sa propre boutique");
        }

        return ConversationResponse.from(
                conversationRepository.findByTypeAndShopIdAndBuyerId(ConversationType.BUYER_SELLER, shopId, buyer.getId())
                        .orElseGet(() -> conversationRepository.save(Conversation.builder()
                                .type(ConversationType.BUYER_SELLER)
                                .shop(shop)
                                .buyer(buyer)
                                .seller(shop.getOwner())
                                .build()))
        );
    }

    @Transactional
    public ConversationResponse startOrGetSupportConversation(User requester) {
        return ConversationResponse.from(
                conversationRepository.findByTypeAndBuyerId(ConversationType.SUPPORT, requester.getId())
                        .orElseGet(() -> conversationRepository.save(Conversation.builder()
                                .type(ConversationType.SUPPORT)
                                .buyer(requester)
                                .build()))
        );
    }

    @Transactional(readOnly = true)
    public Page<ConversationResponse> listMyConversations(User user, Pageable pageable) {
        return conversationRepository.findAllForUser(user.getId(), pageable).map(ConversationResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ConversationResponse> listSupportConversations(Pageable pageable) {
        return conversationRepository.findByTypeOrderByLastMessageAtDesc(ConversationType.SUPPORT, pageable)
                .map(ConversationResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<MessageResponse> listMessages(User user, UUID conversationId, Pageable pageable) {
        Conversation conversation = getAccessibleConversation(user, conversationId);
        return messageRepository.findByConversationIdOrderByCreatedAtDesc(conversation.getId(), pageable)
                .map(MessageResponse::from);
    }

    @Transactional
    public MessageResponse sendMessage(User sender, UUID conversationId, SendMessageRequest request) {
        Conversation conversation = getAccessibleConversation(sender, conversationId);

        if (conversation.getType() == ConversationType.BUYER_SELLER
                && PersonalDataFilter.containsPersonalData(request.content())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Votre message ne peut pas contenir de coordonnées personnelles (téléphone, email, réseaux sociaux). " +
                            "Les échanges doivent rester sur la plateforme Maplenou.");
        }

        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(request.content())
                .build();
        messageRepository.save(message);

        conversation.setLastMessageAt(message.getCreatedAt());
        conversationRepository.save(conversation);

        UUID recipientId = resolveRecipient(conversation, sender.getId());
        if (recipientId != null) {
            notificationService.newMessage(recipientId, conversation.getId(), sender.getFullName());
        }

        return MessageResponse.from(message);
    }

    @Transactional
    public void markAsRead(User user, UUID conversationId) {
        Conversation conversation = getAccessibleConversation(user, conversationId);
        messageRepository.markAllAsRead(conversation.getId(), user.getId(), Instant.now());
    }

    // ----- Utilitaires -----

    private Conversation getAccessibleConversation(User user, UUID conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Conversation introuvable"));

        boolean isParticipant = conversation.getBuyer().getId().equals(user.getId())
                || (conversation.getSeller() != null && conversation.getSeller().getId().equals(user.getId()));
        boolean isAdminOnSupport = conversation.getType() == ConversationType.SUPPORT
                && user.getRole() == com.maplenou.backend.user.Role.ADMIN;

        if (!isParticipant && !isAdminOnSupport) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Accès refusé à cette conversation");
        }
        return conversation;
    }

    private UUID resolveRecipient(Conversation conversation, UUID senderId) {
        if (conversation.getType() == ConversationType.BUYER_SELLER) {
            return conversation.getBuyer().getId().equals(senderId)
                    ? conversation.getSeller().getId()
                    : conversation.getBuyer().getId();
        }
        // SUPPORT : seul le demandeur reçoit une notif (l'équipe admin surveille son propre tableau de bord)
        return conversation.getBuyer().getId().equals(senderId) ? null : conversation.getBuyer().getId();
    }
}
