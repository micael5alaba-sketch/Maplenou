package com.maplenou.backend.catalog;

import com.maplenou.backend.audit.AuditAction;
import com.maplenou.backend.audit.AuditService;
import com.maplenou.backend.catalog.dto.CreateShopRequest;
import com.maplenou.backend.catalog.dto.UpdateShopRequest;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.notification.NotificationService;
import com.maplenou.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ShopService {

    private final ShopRepository shopRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");

    // ----- Vendeur -----

    @Transactional
    public Shop create(User owner, CreateShopRequest request) {
        if (shopRepository.existsByOwnerId(owner.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ce compte possède déjà une boutique");
        }

        String slug = generateUniqueSlug(request.name());

        Shop shop = Shop.builder()
                .owner(owner)
                .name(request.name())
                .slug(slug)
                .description(request.description())
                .city(request.city())
                .district(request.district())
                .taxId(request.taxId())
                .status(ShopStatus.PENDING)
                .build();

        return shopRepository.save(shop);
    }

    public Shop getMine(User owner) {
        return shopRepository.findByOwnerId(owner.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Aucune boutique pour ce compte"));
    }

    @Transactional
    public Shop updateMine(User owner, UpdateShopRequest request) {
        Shop shop = getMine(owner);

        if (request.name() != null && !request.name().isBlank()) {
            shop.setName(request.name());
        }
        if (request.description() != null) {
            shop.setDescription(request.description());
        }
        if (request.logoUrl() != null) {
            shop.setLogoUrl(request.logoUrl());
        }
        if (request.coverUrl() != null) {
            shop.setCoverUrl(request.coverUrl());
        }
        if (request.city() != null && !request.city().isBlank()) {
            shop.setCity(request.city());
        }
        if (request.district() != null) {
            shop.setDistrict(request.district());
        }
        if (request.taxId() != null) {
            shop.setTaxId(request.taxId().isBlank() ? null : request.taxId());
        }

        return shopRepository.save(shop);
    }

    // ----- Public -----

    public Shop getBySlug(String slug) {
        return shopRepository.findBySlug(slug)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Boutique introuvable"));
    }

    public Shop getById(UUID id) {
        return shopRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Boutique introuvable"));
    }

    // ----- Admin -----

    public Page<Shop> listAll(Pageable pageable) {
        return shopRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public Page<Shop> listByStatus(ShopStatus status, Pageable pageable) {
        return shopRepository.findByStatus(status, pageable);
    }

    @Transactional
    public Shop updateStatus(UUID actorId, UUID shopId, ShopStatus newStatus, String ip) {
        Shop shop = getById(shopId);
        shop.setStatus(newStatus);
        Shop saved = shopRepository.save(shop);
        notificationService.shopStatusChanged(
                saved.getOwner().getId(), saved.getName(), newStatus.name());
        auditService.log(actorId, AuditAction.SHOP_STATUS_CHANGED, "SHOP", shopId,
                "Status changed to " + newStatus, ip);
        return saved;
    }

    // ----- Utilitaire -----

    private String generateUniqueSlug(String name) {
        String base = slugify(name);
        String slug = base;
        int suffix = 1;
        while (shopRepository.existsBySlug(slug)) {
            slug = base + "-" + suffix++;
        }
        return slug;
    }

    private String slugify(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .trim();
        String slug = NON_ALPHANUMERIC.matcher(normalized).replaceAll("-");
        return slug.replaceAll("^-+|-+$", "");
    }
}
