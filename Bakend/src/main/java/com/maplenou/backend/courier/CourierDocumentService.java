package com.maplenou.backend.courier;

import com.maplenou.backend.audit.AuditAction;
import com.maplenou.backend.audit.AuditService;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.courier.dto.CourierDocumentResponse;
import com.maplenou.backend.courier.dto.ReviewCourierDocumentRequest;
import com.maplenou.backend.courier.dto.SubmitCourierDocumentRequest;
import com.maplenou.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourierDocumentService {

    private final CourierDocumentRepository courierDocumentRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<CourierDocumentResponse> listMine(User courier) {
        return courierDocumentRepository.findByCourierId(courier.getId()).stream()
                .map(CourierDocumentResponse::from)
                .toList();
    }

    /**
     * Envoi ou remplacement d'un document — un seul par (courier, type) : un nouvel envoi
     * écrase le précédent et repasse en PENDING, même s'il avait déjà été VALID ou REJECTED
     * (ex. renouvellement d'une assurance qui va expirer).
     */
    @Transactional
    public CourierDocumentResponse submit(User courier, SubmitCourierDocumentRequest request) {
        CourierDocument document = courierDocumentRepository
                .findByCourierIdAndType(courier.getId(), request.type())
                .orElseGet(() -> CourierDocument.builder()
                        .courier(courier)
                        .type(request.type())
                        .build());

        document.setFileUrl(request.fileUrl());
        document.setExpiresAt(request.expiresAt());
        document.setStatus(CourierDocumentStatus.PENDING);
        document.setSubmittedAt(Instant.now());
        document.setRejectionReason(null);
        document.setReviewedBy(null);
        document.setReviewedAt(null);

        return CourierDocumentResponse.from(courierDocumentRepository.save(document));
    }

    // ----- Admin -----

    @Transactional(readOnly = true)
    public Page<CourierDocumentResponse> listPendingForAdmin(Pageable pageable) {
        return courierDocumentRepository.findByStatusOrderBySubmittedAtAsc(CourierDocumentStatus.PENDING, pageable)
                .map(CourierDocumentResponse::from);
    }

    @Transactional
    public CourierDocumentResponse review(UUID documentId, ReviewCourierDocumentRequest request, User admin, String ip) {
        CourierDocument document = courierDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Document introuvable"));

        if (request.decision() == CourierDocumentStatus.REJECTED
                && (request.rejectionReason() == null || request.rejectionReason().isBlank())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Un motif de refus est obligatoire");
        }

        document.setStatus(request.decision());
        document.setRejectionReason(request.decision() == CourierDocumentStatus.REJECTED ? request.rejectionReason() : null);
        document.setReviewedBy(admin);
        document.setReviewedAt(Instant.now());
        courierDocumentRepository.save(document);

        auditService.log(admin.getId(), AuditAction.COURIER_DOCUMENT_REVIEWED,
                "COURIER_DOCUMENT", documentId,
                "Livreur : " + document.getCourier().getFullName() +
                        " | Type : " + document.getType() +
                        " | Décision : " + request.decision(),
                ip);

        return CourierDocumentResponse.from(document);
    }
}
