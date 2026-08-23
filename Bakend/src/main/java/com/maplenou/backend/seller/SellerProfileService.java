package com.maplenou.backend.seller;

import com.maplenou.backend.audit.AuditAction;
import com.maplenou.backend.audit.AuditService;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.seller.dto.ApplySellerRequest;
import com.maplenou.backend.seller.dto.UpdateSellerStatusRequest;
import com.maplenou.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellerProfileService {

    private final SellerProfileRepository sellerProfileRepository;
    private final AuditService auditService;

    @Transactional
    public SellerProfile apply(User currentUser, ApplySellerRequest request) {
        if (sellerProfileRepository.existsByUserId(currentUser.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ce compte a deja un profil vendeur");
        }

        SellerProfile profile = SellerProfile.builder()
                .user(currentUser)
                .shopName(request.shopName())
                .status(SellerStatus.PENDING)
                .build();

        return sellerProfileRepository.save(profile);
    }

    public SellerProfile getMine(User currentUser) {
        return sellerProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Aucun profil vendeur pour ce compte"));
    }

    // ----- Admin -----

    @Transactional(readOnly = true)
    public Page<SellerProfile> listByStatus(SellerStatus status, Pageable pageable) {
        if (status != null) {
            return sellerProfileRepository.findByStatus(status, pageable);
        }
        return sellerProfileRepository.findAll(pageable);
    }

    @Transactional
    public SellerProfile updateStatus(UUID profileId, UpdateSellerStatusRequest request,
                                      UUID actorId, String ip) {
        SellerProfile profile = sellerProfileRepository.findById(profileId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Profil vendeur introuvable"));

        SellerStatus newStatus = request.status();
        if (newStatus == SellerStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Impossible de repasser un profil à PENDING");
        }
        profile.setStatus(newStatus);
        SellerProfile saved = sellerProfileRepository.save(profile);

        AuditAction action = newStatus == SellerStatus.APPROVED
                ? AuditAction.SELLER_APPROVED : AuditAction.SELLER_REJECTED;
        auditService.log(actorId, action, "SELLER_PROFILE", profileId,
                "Status → " + newStatus + (request.adminNote() != null ? " | " + request.adminNote() : ""), ip);

        return saved;
    }
}
