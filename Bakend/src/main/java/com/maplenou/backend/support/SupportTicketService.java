package com.maplenou.backend.support;

import com.maplenou.backend.audit.AuditAction;
import com.maplenou.backend.audit.AuditService;
import com.maplenou.backend.common.HtmlSanitizer;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.delivery.Delivery;
import com.maplenou.backend.delivery.DeliveryRepository;
import com.maplenou.backend.messaging.ConversationRepository;
import com.maplenou.backend.messaging.MessagingService;
import com.maplenou.backend.messaging.dto.SendMessageRequest;
import com.maplenou.backend.support.dto.CreateSupportTicketRequest;
import com.maplenou.backend.support.dto.ResolveSupportTicketRequest;
import com.maplenou.backend.support.dto.SupportTicketResponse;
import com.maplenou.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Tickets de support structurés (livreur/acheteur), avec un vrai numéro de suivi (contrairement
 * à l'ancien système qui envoyait juste un message de messagerie avec un numéro inventé côté app
 * — voir l'historique de `SupportTicketService` côté frontend). Le message initial est aussi
 * posté dans la conversation support classique pour que les réponses restent dans la messagerie.
 */
@Service
@RequiredArgsConstructor
public class SupportTicketService {

    private final SupportTicketRepository supportTicketRepository;
    private final DeliveryRepository deliveryRepository;
    private final ConversationRepository conversationRepository;
    private final MessagingService messagingService;
    private final AuditService auditService;

    @Transactional
    public SupportTicketResponse create(User requester, CreateSupportTicketRequest request) {
        Delivery delivery = null;
        if (request.relatedDeliveryId() != null) {
            delivery = deliveryRepository.findById(request.relatedDeliveryId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Livraison introuvable"));
        }

        String ticketNumber = generateTicketNumber();

        SupportTicket ticket = SupportTicket.builder()
                .ticketNumber(ticketNumber)
                .requester(requester)
                .category(request.category())
                .subject(HtmlSanitizer.stripTags(request.subject()))
                .description(HtmlSanitizer.stripTags(request.description()))
                .relatedDelivery(delivery)
                .photoUrl(request.photoUrl())
                .status(SupportTicketStatus.OPEN)
                .build();

        // Poste aussi le message dans la conversation support classique, pour que les réponses
        // de l'équipe restent visibles dans la messagerie déjà existante côté app.
        var conversation = messagingService.startOrGetSupportConversation(requester);
        var conversationRef = conversationRepository.getReferenceById(conversation.id());
        ticket.setConversation(conversationRef);

        SupportTicket saved = supportTicketRepository.save(ticket);

        StringBuilder message = new StringBuilder();
        message.append("[Ticket ").append(ticketNumber).append("] ").append(request.subject()).append('\n');
        message.append("Catégorie : ").append(request.category()).append('\n');
        if (delivery != null) {
            message.append("Livraison concernée : ").append(delivery.getId()).append('\n');
        }
        message.append('\n').append(request.description());
        if (request.photoUrl() != null) {
            message.append("\n\nPhoto jointe : ").append(request.photoUrl());
        }
        messagingService.sendMessage(requester, conversation.id(), new SendMessageRequest(message.toString()));

        return SupportTicketResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<SupportTicketResponse> listMine(User requester, Pageable pageable) {
        return supportTicketRepository.findByRequesterIdOrderByCreatedAtDesc(requester.getId(), pageable)
                .map(SupportTicketResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<SupportTicketResponse> listForAdmin(SupportTicketStatus status, Pageable pageable) {
        if (status != null) {
            return supportTicketRepository.findByStatusOrderByCreatedAtAsc(status, pageable)
                    .map(SupportTicketResponse::from);
        }
        return supportTicketRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(SupportTicketResponse::from);
    }

    @Transactional
    public SupportTicketResponse resolve(UUID ticketId, ResolveSupportTicketRequest request, User admin, String ip) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ticket introuvable"));

        ticket.setStatus(request.status());
        ticket.setResolutionNotes(request.resolutionNotes());
        if (request.status() == SupportTicketStatus.RESOLVED) {
            ticket.setResolvedAt(Instant.now());
        }
        SupportTicket saved = supportTicketRepository.save(ticket);

        auditService.log(admin.getId(), AuditAction.SUPPORT_TICKET_RESOLVED,
                "SUPPORT_TICKET", ticketId,
                "Ticket " + ticket.getTicketNumber() + " | Statut : " + request.status(), ip);

        return SupportTicketResponse.from(saved);
    }

    /** "TKT-2026-000123" : année courante + numéro séquentiel sur 6 chiffres (séquence Postgres). */
    String generateTicketNumber() {
        int year = Instant.now().atZone(ZoneOffset.UTC).getYear();
        long sequence = supportTicketRepository.nextSequenceValue();
        return "TKT-%d-%06d".formatted(year, sequence);
    }
}
