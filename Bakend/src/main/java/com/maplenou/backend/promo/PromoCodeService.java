package com.maplenou.backend.promo;

import com.maplenou.backend.catalog.ShopRepository;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.order.Order;
import com.maplenou.backend.promo.dto.*;
import com.maplenou.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PromoCodeService {

    private final PromoCodeRepository     promoCodeRepository;
    private final PromoCodeUsageRepository usageRepository;
    private final ShopRepository          shopRepository;

    // ── Admin : création ──────────────────────────────────────────────────────

    /**
     * Crée un code promo.
     * PLATFORM : réservé à l'admin.
     * SHOP     : admin ou vendeur propriétaire de la boutique.
     */
    @Transactional
    public PromoCodeResponse create(User creator, CreatePromoCodeRequest req) {

        // Validation : SHOP nécessite un scopeId
        if (req.scopeType() == PromoScopeType.SHOP && req.scopeId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Un code SHOP nécessite un identifiant de boutique (scopeId)");
        }

        // Pour un code SHOP, vérifier que la boutique existe
        if (req.scopeType() == PromoScopeType.SHOP) {
            shopRepository.findById(req.scopeId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Boutique introuvable"));

            // Un vendeur ne peut créer des codes que pour SA boutique
            boolean isAdmin = creator.getRole() == com.maplenou.backend.user.Role.ADMIN;
            if (!isAdmin) {
                var ownedShop = shopRepository.findByOwnerId(creator.getId())
                        .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN,
                                "Vous n'avez pas de boutique active"));
                if (!ownedShop.getId().equals(req.scopeId())) {
                    throw new ApiException(HttpStatus.FORBIDDEN,
                            "Vous ne pouvez créer des codes que pour votre propre boutique");
                }
            }
        } else {
            // PLATFORM : réservé à l'admin
            if (creator.getRole() != com.maplenou.backend.user.Role.ADMIN) {
                throw new ApiException(HttpStatus.FORBIDDEN,
                        "Seul un administrateur peut créer un code PLATFORM");
            }
        }

        // Expiration dans le passé
        if (req.expiresAt() != null && req.expiresAt().isBefore(java.time.Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "La date d'expiration ne peut pas être dans le passé");
        }

        PromoCode promo = PromoCode.builder()
                .code(req.code().toUpperCase())
                .scopeType(req.scopeType())
                .scopeId(req.scopeType() == PromoScopeType.SHOP ? req.scopeId() : null)
                .discountPercent(req.discountPercent())
                .minOrderAmount(req.minOrderAmount())
                .maxUses(req.maxUses())
                .expiresAt(req.expiresAt())
                .build();

        return PromoCodeResponse.from(promoCodeRepository.save(promo));
    }

    // ── Activation / désactivation ────────────────────────────────────────────

    @Transactional
    public PromoCodeResponse setActive(User actor, UUID promoId, boolean active) {
        PromoCode promo = getOrThrow(promoId);
        checkEditPermission(actor, promo);
        promo.setActive(active);
        return PromoCodeResponse.from(promoCodeRepository.save(promo));
    }

    // ── Validation (avant placement de commande) ──────────────────────────────

    /**
     * Valide un code promo et retourne le montant de réduction.
     * N'enregistre PAS l'utilisation — celle-ci se fait dans applyToOrder().
     */
    @Transactional(readOnly = true)
    public ValidatePromoResponse validate(User buyer, ValidatePromoRequest req) {
        PromoCode promo = resolve(req.code(), req.shopId());
        assertApplicable(promo, buyer.getId(), req.orderAmount());

        BigDecimal discount  = promo.computeDiscount(req.orderAmount());
        BigDecimal finalAmt  = req.orderAmount().subtract(discount).max(BigDecimal.ZERO);

        return new ValidatePromoResponse(
                promo.getId(), promo.getCode(),
                promo.getDiscountPercent(), discount, finalAmt);
    }

    // ── Application à une commande (appelé par OrderService) ─────────────────

    /**
     * Applique le code promo à une commande passée.
     * Enregistre l'utilisation, incrémente le compteur et lie le code à la commande.
     * Doit être appelé dans la même transaction que la création de la commande.
     */
    @Transactional
    public BigDecimal applyToOrder(User buyer, String code, UUID shopId,
                                   BigDecimal orderAmount, Order order) {
        PromoCode promo = resolve(code, shopId);
        assertApplicable(promo, buyer.getId(), orderAmount);

        BigDecimal discount = promo.computeDiscount(orderAmount);

        // Lier le code promo à la commande (FK promo_code_id)
        order.setPromoCode(promo);

        PromoCodeUsage usage = PromoCodeUsage.builder()
                .promoCode(promo)
                .order(order)
                .user(buyer)
                .discountAmount(discount)
                .build();
        usageRepository.save(usage);
        promoCodeRepository.incrementUsage(promo.getId());

        return discount;
    }

    // ── Lecture ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<PromoCodeResponse> listPlatformCodes(Pageable pageable) {
        return promoCodeRepository
                .findByScopeType(PromoScopeType.PLATFORM, pageable)
                .map(PromoCodeResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<PromoCodeResponse> listShopCodes(User actor, UUID shopId, Pageable pageable) {
        // Un vendeur ne peut voir que les codes de SA boutique
        boolean isAdmin = actor.getRole() == com.maplenou.backend.user.Role.ADMIN;
        if (!isAdmin) {
            var ownedShop = shopRepository.findByOwnerId(actor.getId())
                    .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN,
                            "Vous n'avez pas de boutique active"));
            if (!ownedShop.getId().equals(shopId)) {
                throw new ApiException(HttpStatus.FORBIDDEN,
                        "Vous ne pouvez pas consulter les codes d'une autre boutique");
            }
        }
        return promoCodeRepository
                .findByScopeTypeAndScopeId(PromoScopeType.SHOP, shopId, pageable)
                .map(PromoCodeResponse::from);
    }

    @Transactional(readOnly = true)
    public PromoCodeResponse getById(UUID id) {
        return PromoCodeResponse.from(getOrThrow(id));
    }

    // ── Privé ─────────────────────────────────────────────────────────────────

    /**
     * Résout un code : cherche d'abord un code SHOP correspondant au shopId,
     * puis repli sur un code PLATFORM du même libellé.
     */
    private PromoCode resolve(String code, UUID shopId) {
        PromoCode promo = promoCodeRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Code promo introuvable ou invalide"));

        // Vérification de périmètre
        if (promo.getScopeType() == PromoScopeType.SHOP) {
            if (shopId == null || !promo.getScopeId().equals(shopId)) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Ce code promo n'est pas valide pour cette boutique");
            }
        }

        return promo;
    }

    private void assertApplicable(PromoCode promo, UUID buyerId, BigDecimal amount) {
        if (!promo.isActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ce code promo est désactivé");
        }
        if (promo.isExpired()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ce code promo a expiré");
        }
        if (promo.isExhausted()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Ce code promo a atteint son nombre maximum d'utilisations");
        }
        if (promo.getMinOrderAmount() != null
                && amount.compareTo(promo.getMinOrderAmount()) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Le montant minimum pour ce code est de " + promo.getMinOrderAmount() + " FCFA");
        }
        if (usageRepository.existsByPromoCodeIdAndUserId(promo.getId(), buyerId)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Vous avez déjà utilisé ce code promo");
        }
    }

    private PromoCode getOrThrow(UUID id) {
        return promoCodeRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Code promo introuvable"));
    }

    private void checkEditPermission(User actor, PromoCode promo) {
        boolean isAdmin = actor.getRole() == com.maplenou.backend.user.Role.ADMIN;
        if (isAdmin) return;

        if (promo.getScopeType() == PromoScopeType.PLATFORM) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Seul un administrateur peut modifier un code PLATFORM");
        }
        // SHOP : vérifier que le vendeur est propriétaire de la boutique
        var ownedShop = shopRepository.findByOwnerId(actor.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN,
                        "Vous n'avez pas de boutique active"));
        if (!ownedShop.getId().equals(promo.getScopeId())) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Ce code promo n'appartient pas à votre boutique");
        }
    }
}
