package com.maplenou.backend.reseller;

import com.maplenou.backend.audit.AuditAction;
import com.maplenou.backend.audit.AuditService;
import com.maplenou.backend.catalog.Product;
import com.maplenou.backend.catalog.ProductRepository;
import com.maplenou.backend.catalog.ProductStatus;
import com.maplenou.backend.catalog.ShopStatus;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.order.CommissionService;
import com.maplenou.backend.reseller.dto.ResellerDtos.*;
import com.maplenou.backend.user.User;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Catalogue partenaire, liens personnels, enregistrement des clics et jeton d'attribution signé.
 * Le jeton ({@code clickId.signature}, HMAC-SHA256) est renvoyé à l'appli, qui le joint à la commande ;
 * il ne contient aucun montant : partenaire, produit et expiration sont relus en base au moment de la
 * commande, ce qui rend toute falsification inutile.
 */
@Service
@RequiredArgsConstructor
public class ResellerLinkService {

    private static final String CODE_ALPHABET = "abcdefghjkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration CLICK_DEDUP = Duration.ofMinutes(10);

    private final ResellerReferralLinkRepository linkRepository;
    private final ResellerClickRepository clickRepository;
    private final ResellerCommissionRepository commissionRepository;
    private final ResellerProductEligibilityRepository eligibilityRepository;
    private final ResellerAuditLogRepository auditRepository;
    private final ResellerSettingsService settingsService;
    private final ResellerService resellerService;
    private final ProductRepository productRepository;
    private final AuditService auditService;
    private final EntityManager entityManager;
    private final JdbcTemplate jdbc;

    @Value("${app.jwt.secret}")
    private String signingSecret;

    @Value("${app.reseller.link-base-url:http://localhost:8080}")
    private String linkBaseUrl;

    @Value("${app.reseller.web-url:http://localhost:3000}")
    private String webUrl;

    // ----- Catalogue partenaire -----

    @Transactional(readOnly = true)
    public Page<CatalogItem> catalog(User user, Pageable pageable) {
        ResellerProfile partner = resellerService.requireActive(user);
        ResellerSettings settings = settingsService.current();
        List<UUID> excluded = eligibilityRepository.findExcludedProductIds();

        // Borne basse volontairement large sur le prix de base ; l'éligibilité exacte (prix affiché,
        // commission incluse) est vérifiée produit par produit juste après.
        long minBase = Math.max(1, (long) Math.floor(settings.getMinUnitPrice() / 1.06));
        StringBuilder jpql = new StringBuilder("select p from Product p join fetch p.shop s "
                + "where p.status = :status and p.deleted = false and s.status = :shopStatus "
                + "and p.basePrice >= :minBase");
        if (!excluded.isEmpty()) jpql.append(" and p.id not in :excluded");
        jpql.append(" order by p.createdAt desc, p.id desc");
        var query = entityManager.createQuery(jpql.toString(), Product.class)
                .setParameter("status", ProductStatus.ACTIVE)
                .setParameter("shopStatus", ShopStatus.APPROVED)
                .setParameter("minBase", BigDecimal.valueOf(minBase));
        if (!excluded.isEmpty()) query.setParameter("excluded", excluded);
        query.setFirstResult((int) pageable.getOffset()).setMaxResults(pageable.getPageSize());
        List<Product> products = query.getResultList();

        List<CatalogItem> items = products.stream()
                .filter(p -> ResellerCommissionCalculator.isEligible(unitPriceOf(p), settings))
                .map(p -> {
                    long price = unitPriceOf(p);
                    var calc = ResellerCommissionCalculator.compute(price, settings);
                    Optional<ResellerReferralLink> link = linkRepository.findByPartnerIdAndProductId(partner.getId(), p.getId());
                    return new CatalogItem(p.getId(), p.getName(), p.getShop().getName(),
                            p.getImages().isEmpty() ? null : p.getImages().get(0).getUrl(), price,
                            calc.partnerShare(),
                            link.map(ResellerReferralLink::getCode).orElse(null),
                            link.map(l -> urlFor(l.getCode())).orElse(null),
                            link.map(l -> clickRepository.countByLinkIdAndCountedTrue(l.getId())).orElse(0L),
                            commissionRepository.countUnitsByPartnerAndProduct(partner.getId(), p.getId()));
                })
                .toList();
        return new PageImpl<>(items, pageable, items.size() + pageable.getOffset()
                + (products.size() == pageable.getPageSize() ? 1 : 0));
    }

    // ----- Liens -----

    @Transactional
    public LinkResponse createLink(User user, UUID productId) {
        ResellerProfile partner = resellerService.requireActive(user);
        ResellerSettings settings = settingsService.current();
        if (!settings.isProgramActive()) {
            throw new ApiException(HttpStatus.CONFLICT, "Le programme partenaire est temporairement fermé");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Produit introuvable"));
        if (!isProductEligible(product, settings)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ce produit n'est pas éligible au programme partenaire");
        }
        ResellerReferralLink link = linkRepository.findByPartnerIdAndProductId(partner.getId(), productId)
                .orElseGet(() -> linkRepository.save(ResellerReferralLink.builder()
                        .partner(partner).product(product).code(newCode()).build()));
        return toLink(link);
    }

    @Transactional(readOnly = true)
    public Page<LinkResponse> myLinks(User user, Pageable pageable) {
        ResellerProfile partner = resellerService.requireProfile(user);
        return linkRepository.findByPartner(partner.getId(), pageable).map(this::toLink);
    }

    /** Destination web d'un lien (redirection publique) ; renvoie la page d'accueil si le code est inconnu. */
    @Transactional(readOnly = true)
    public String redirectTarget(String code) {
        return linkRepository.findByCodeWithDetails(code)
                .map(l -> webUrl + "/?ref=" + l.getCode())
                .orElse(webUrl + "/");
    }

    // ----- Exclusions produit (admin) -----

    @Transactional
    public void setProductExcluded(UUID productId, boolean excluded, String reason, User actor, String ip) {
        if (reason == null || reason.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Un motif est obligatoire");
        }
        productRepository.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Produit introuvable"));
        boolean before = eligibilityRepository.findById(productId).map(ResellerProductEligibility::isExcluded).orElse(false);
        eligibilityRepository.save(ResellerProductEligibility.builder()
                .productId(productId).excluded(excluded).updatedBy(actor.getId()).updatedAt(Instant.now()).build());
        auditRepository.save(ResellerAuditLog.builder()
                .actorId(actor.getId()).action("PRODUCT_EXCLUSION").entityType("PRODUCT").entityId(productId)
                .oldValue(String.valueOf(before)).newValue(String.valueOf(excluded)).reason(reason.trim()).build());
        auditService.log(actor.getId(), AuditAction.RESELLER_PRODUCT_EXCLUSION_CHANGED, "PRODUCT", productId,
                "exclu=" + excluded + " | " + reason.trim(), ip);
    }

    // ----- Clics et jeton -----

    /**
     * Enregistre un clic. Il est « valide » (peut servir à l'attribution) si le programme est actif, le
     * partenaire approuvé, le produit éligible et que le visiteur n'est pas le partenaire lui-même. Un
     * visiteur qui reclique en moins de 10 minutes n'est compté qu'une fois dans les statistiques, mais
     * son clic reste valide : le dernier clic valide l'emporte.
     */
    @Transactional
    public ClickResponse recordClick(String code, String visitorId, String ip, String userAgent, User viewer) {
        ResellerReferralLink link = linkRepository.findByCodeWithDetails(code)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Lien introuvable"));
        ResellerSettings settings = settingsService.current();
        Product product = link.getProduct();
        boolean selfClick = viewer != null && viewer.getId().equals(link.getPartner().getUser().getId());
        boolean valid = settings.isProgramActive() && link.getPartner().isActivePartner()
                && isProductEligible(product, settings) && !selfClick;

        String visitorHash = sha256((visitorId != null && !visitorId.isBlank()) ? visitorId
                : String.valueOf(ip) + "|" + String.valueOf(userAgent));
        Instant now = Instant.now();
        boolean counted = !clickRepository.existsByLinkIdAndVisitorHashAndCountedTrueAndClickedAtAfter(
                link.getId(), visitorHash, now.minus(CLICK_DEDUP));
        Instant expiresAt = now.plus(Duration.ofDays(settings.getAttributionDays()));
        // saveAndFlush : l'upsert JDBC ci-dessous référence ce clic par clé étrangère.
        ResellerClick click = clickRepository.saveAndFlush(ResellerClick.builder()
                .linkId(link.getId()).partnerId(link.getPartner().getId()).productId(product.getId())
                .visitorHash(visitorHash).valid(valid).counted(counted).clickedAt(now).expiresAt(expiresAt).build());

        if (!valid) {
            return new ClickResponse(false, product.getId(), product.getSlug(), null, null);
        }
        if (viewer != null) {
            // Un nouveau clic valide REMPLACE l'attribution précédente de ce compte pour ce produit.
            jdbc.update("INSERT INTO reseller_buyer_attributions (user_id, product_id, click_id, partner_id, clicked_at, expires_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (user_id, product_id) DO UPDATE SET "
                            + "click_id = EXCLUDED.click_id, partner_id = EXCLUDED.partner_id, "
                            + "clicked_at = EXCLUDED.clicked_at, expires_at = EXCLUDED.expires_at",
                    viewer.getId(), product.getId(), click.getId(), link.getPartner().getId(),
                    Timestamp.from(now), Timestamp.from(expiresAt));
        }
        return new ClickResponse(true, product.getId(), product.getSlug(), tokenFor(click.getId()), expiresAt);
    }

    /** Clic encore utilisable d'après un jeton signé, ou vide si le jeton est faux. */
    Optional<UUID> clickIdFromToken(String token) {
        if (token == null) return Optional.empty();
        int dot = token.indexOf('.');
        if (dot < 0) return Optional.empty();
        try {
            UUID clickId = UUID.fromString(token.substring(0, dot));
            byte[] expected = hmac(clickId.toString());
            byte[] given = Base64.getUrlDecoder().decode(token.substring(dot + 1));
            return MessageDigest.isEqual(expected, given) ? Optional.of(clickId) : Optional.empty();
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    // ----- Éligibilité -----

    boolean isProductEligible(Product product, ResellerSettings settings) {
        if (product.isDeleted() || product.getStatus() != ProductStatus.ACTIVE
                || product.getShop().getStatus() != ShopStatus.APPROVED) {
            return false;
        }
        if (eligibilityRepository.findById(product.getId()).map(ResellerProductEligibility::isExcluded).orElse(false)) {
            return false;
        }
        return ResellerCommissionCalculator.isEligible(unitPriceOf(product), settings);
    }

    static long unitPriceOf(Product p) {
        return CommissionService.displayPriceFor(p, p.getBasePrice()).longValue();
    }

    // ----- Privé -----

    private String tokenFor(UUID clickId) {
        return clickId + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(hmac(clickId.toString()));
    }

    private byte[] hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(("reseller:" + signingSecret).getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Signature indisponible", e);
        }
    }

    private String newCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder(8);
            for (int i = 0; i < 8; i++) sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            if (!linkRepository.existsByCode(sb.toString())) return sb.toString();
        }
        throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Impossible de générer un code de lien");
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String urlFor(String code) {
        return linkBaseUrl + "/r/" + code;
    }

    private LinkResponse toLink(ResellerReferralLink l) {
        return new LinkResponse(l.getId(), l.getProduct().getId(), l.getProduct().getName(), l.getCode(),
                urlFor(l.getCode()), l.getCreatedAt());
    }
}
