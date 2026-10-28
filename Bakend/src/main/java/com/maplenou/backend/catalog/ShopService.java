package com.maplenou.backend.catalog;

import com.maplenou.backend.audit.AuditAction;
import com.maplenou.backend.audit.AuditService;
import com.maplenou.backend.catalog.dto.AdminCreateShopRequest;
import com.maplenou.backend.catalog.dto.CreateShopRequest;
import com.maplenou.backend.catalog.dto.OpeningHourEntry;
import com.maplenou.backend.catalog.dto.UpdateOpeningHoursRequest;
import com.maplenou.backend.catalog.dto.UpdateShopRequest;
import com.maplenou.backend.common.HtmlSanitizer;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.notification.NotificationService;
import com.maplenou.backend.seller.SellerStatus;
import com.maplenou.backend.user.User;
import com.maplenou.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalTime;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * {@code @Transactional(readOnly = true)} au niveau classe : spring.jpa.open-in-view=false ferme
 * la session dès que la méthode de service retourne, avant que ShopController construise
 * ShopResponse — sans une transaction active ici (certaines méthodes, comme {@code getMine},
 * n'en avaient pas du tout), le champ EAGER {@code Shop.openingHours} levait
 * LazyInitializationException dès qu'un deuxième accès (hors session) y touchait. Les méthodes
 * d'écriture gardent leur propre {@code @Transactional} (non readOnly), qui prend le dessus.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShopService {

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");

    // ----- Vendeur -----

    /**
     * Fusion des deux validations admin signalée par l'audit externe du 16/10/2026 : ce point
     * n'est atteignable qu'avec {@code ROLE_SELLER} (voir {@code @PreAuthorize} du contrôleur),
     * donc uniquement si {@code SellerProfile.status == APPROVED} (User.getAuthorities(), voir
     * CLAUDE.md §3) -- l'admin a déjà vérifié ce compte une fois. Exiger une DEUXIÈME approbation
     * rien que pour la boutique était redondant ; la boutique est désormais approuvée directement
     * à la création. L'admin garde la main pour suspendre/rejeter après coup si besoin.
     */
    @Transactional
    public Shop create(User owner, CreateShopRequest request) {
        if (shopRepository.existsByOwnerId(owner.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ce compte possède déjà une boutique");
        }

        String slug = generateUniqueSlug(request.name());

        Shop shop = Shop.builder()
                .owner(owner)
                .name(HtmlSanitizer.stripTags(request.name()))
                .slug(slug)
                .description(HtmlSanitizer.stripTags(request.description()))
                .city(request.city())
                .district(request.district())
                .taxId(request.taxId())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .status(ShopStatus.APPROVED)
                .build();

        Shop saved = shopRepository.save(shop);
        saved.getOpeningHours().size();
        return saved;
    }

    public Shop getMine(User owner) {
        Shop shop = shopRepository.findByOwnerId(owner.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Aucune boutique pour ce compte"));
        shop.getOpeningHours().size(); // force l'initialisation pendant que la session est active
        return shop;
    }

    @Transactional
    public Shop updateMine(User owner, UpdateShopRequest request) {
        Shop shop = getMine(owner);

        if (request.name() != null && !request.name().isBlank()) {
            shop.setName(HtmlSanitizer.stripTags(request.name()));
        }
        if (request.description() != null) {
            shop.setDescription(HtmlSanitizer.stripTags(request.description()));
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
        if (request.latitude() != null) {
            shop.setLatitude(request.latitude());
        }
        if (request.longitude() != null) {
            shop.setLongitude(request.longitude());
        }

        Shop saved = shopRepository.save(shop);
        saved.getOpeningHours().size();
        return saved;
    }

    /** Remplace l'horaire d'ouverture complet — signalé manquant dans le rapport de
     * modification du 28/10/2026. orphanRemoval=true sur Shop.openingHours : vider puis
     * repeupler la collection supprime correctement les anciennes lignes en base. */
    @Transactional
    public Shop updateOpeningHours(User owner, UpdateOpeningHoursRequest request) {
        Shop shop = getMine(owner);

        shop.getOpeningHours().clear();
        for (OpeningHourEntry entry : request.hours()) {
            shop.getOpeningHours().add(ShopOpeningHour.builder()
                    .shop(shop)
                    .dayOfWeek((short) entry.dayOfWeek())
                    .opensAt(entry.closed() ? null : parseTime(entry.opensAt()))
                    .closesAt(entry.closed() ? null : parseTime(entry.closesAt()))
                    .closed(entry.closed())
                    .build());
        }

        Shop saved = shopRepository.save(shop);
        saved.getOpeningHours().size();
        return saved;
    }

    private LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) return null;
        return LocalTime.parse(value);
    }

    // ----- Public -----

    public Shop getBySlug(String slug) {
        Shop shop = shopRepository.findBySlug(slug)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Boutique introuvable"));
        shop.getOpeningHours().size();
        return shop;
    }

    public Shop getById(UUID id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Boutique introuvable"));
        shop.getOpeningHours().size();
        return shop;
    }

    /** Public : uniquement les boutiques approuvées, jamais PENDING/REJECTED/SUSPENDED
     * (contrairement à {@link #listAll}/{@link #listByStatus}, réservées à l'admin). */
    public Page<Shop> listPublic(Pageable pageable) {
        Page<Shop> page = shopRepository.findByStatus(ShopStatus.APPROVED, pageable);
        page.forEach(s -> s.getOpeningHours().size());
        return page;
    }

    // ----- Admin -----

    public Page<Shop> listAll(Pageable pageable) {
        Page<Shop> page = shopRepository.findAllByOrderByCreatedAtDesc(pageable);
        page.forEach(s -> s.getOpeningHours().size());
        return page;
    }

    public Page<Shop> listByStatus(ShopStatus status, Pageable pageable) {
        Page<Shop> page = shopRepository.findByStatus(status, pageable);
        page.forEach(s -> s.getOpeningHours().size());
        return page;
    }

    /** Admin : crée une boutique pour un vendeur déjà approuvé — directement APPROVED,
     * puisque c'est l'admin lui-même qui en décide (pas de validation supplémentaire requise). */
    @Transactional
    public Shop adminCreate(AdminCreateShopRequest request, UUID actorId, String ip) {
        User owner = userRepository.findById(request.ownerId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));

        if (owner.getSellerProfile() == null || owner.getSellerProfile().getStatus() != SellerStatus.APPROVED) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Ce compte n'a pas encore de profil vendeur approuvé — approuve-le d'abord dans \"Candidatures vendeurs\"");
        }
        if (shopRepository.existsByOwnerId(owner.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ce compte possède déjà une boutique");
        }

        String slug = generateUniqueSlug(request.name());

        Shop shop = Shop.builder()
                .owner(owner)
                .name(HtmlSanitizer.stripTags(request.name()))
                .slug(slug)
                .description(HtmlSanitizer.stripTags(request.description()))
                .city(request.city())
                .district(request.district())
                .taxId(request.taxId())
                .status(ShopStatus.APPROVED)
                .build();

        Shop saved = shopRepository.save(shop);
        saved.getOpeningHours().size();
        auditService.log(actorId, AuditAction.SHOP_STATUS_CHANGED, "SHOP", saved.getId(),
                "Boutique créée directement par l'admin pour " + owner.getFullName(), ip);
        return saved;
    }

    @Transactional
    public Shop updateStatus(UUID actorId, UUID shopId, ShopStatus newStatus, String ip) {
        Shop shop = getById(shopId);
        shop.setStatus(newStatus);
        Shop saved = shopRepository.save(shop);
        saved.getOpeningHours().size();
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
