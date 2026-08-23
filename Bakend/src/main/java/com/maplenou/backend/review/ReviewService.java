package com.maplenou.backend.review;

import com.maplenou.backend.catalog.Product;
import com.maplenou.backend.catalog.ProductRepository;
import com.maplenou.backend.catalog.Shop;
import com.maplenou.backend.catalog.ShopRepository;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.order.SubOrder;
import com.maplenou.backend.order.SubOrderRepository;
import com.maplenou.backend.order.SubOrderStatus;
import com.maplenou.backend.review.dto.*;
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
public class ReviewService {

    private final ProductReviewRepository productReviewRepository;
    private final ShopReviewRepository    shopReviewRepository;
    private final SubOrderRepository      subOrderRepository;
    private final ProductRepository       productRepository;
    private final ShopRepository          shopRepository;

    // ── Avis produit ──────────────────────────────────────────────────────────

    /**
     * L'acheteur soumet un avis sur un produit.
     * Règles :
     *  - La sous-commande doit être DELIVERED et appartenir à l'acheteur
     *  - Le produit doit appartenir à la sous-commande (via la boutique)
     *  - Un seul avis par acheteur par produit
     *  - Une sous-commande ne peut servir qu'une seule fois par produit
     */
    @Transactional
    public ReviewResponse createProductReview(User buyer, CreateProductReviewRequest req) {

        SubOrder subOrder = subOrderRepository.findByIdWithDetails(req.subOrderId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Sous-commande introuvable"));

        // L'acheteur est bien le propriétaire de la commande
        if (!subOrder.getOrder().getBuyer().getId().equals(buyer.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Cette sous-commande ne vous appartient pas");
        }

        // La sous-commande doit être livrée
        if (subOrder.getStatus() != SubOrderStatus.DELIVERED) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Un avis n'est possible que sur une sous-commande livrée (statut : " + subOrder.getStatus() + ")");
        }

        Product product = productRepository.findByIdWithDetails(req.productId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Produit introuvable"));

        // Le produit appartient bien à la boutique de cette sous-commande
        if (!product.getShop().getId().equals(subOrder.getShop().getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Ce produit n'appartient pas à la boutique de cette sous-commande");
        }

        // Un seul avis par acheteur par produit
        if (productReviewRepository.existsByProductIdAndAuthorId(product.getId(), buyer.getId())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Vous avez déjà laissé un avis sur ce produit");
        }

        // Une sous-commande ne peut être utilisée qu'une seule fois par produit
        if (productReviewRepository.existsByProductIdAndSubOrderId(product.getId(), subOrder.getId())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Cette sous-commande a déjà été utilisée pour un avis sur ce produit");
        }

        ProductReview review = ProductReview.builder()
                .product(product)
                .author(buyer)
                .subOrder(subOrder)
                .rating(req.rating())
                .comment(req.comment())
                .build();

        return ReviewResponse.from(productReviewRepository.save(review));
    }

    /** Avis publics d'un produit (paginés). */
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getProductReviews(UUID productId, Pageable pageable) {
        if (!productRepository.existsById(productId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Produit introuvable");
        }
        return productReviewRepository.findByProductIdWithAuthor(productId, pageable)
                .map(ReviewResponse::from);
    }

    /** Note moyenne + nombre d'avis d'un produit. */
    @Transactional(readOnly = true)
    public RatingSummaryResponse getProductRatingSummary(UUID productId) {
        double avg   = productReviewRepository.findAverageRatingByProductId(productId).orElse(0.0);
        long   count = productReviewRepository.countByProductId(productId);
        return new RatingSummaryResponse(avg, count);
    }

    // ── Avis boutique ─────────────────────────────────────────────────────────

    /**
     * L'acheteur soumet un avis sur une boutique.
     * Règles :
     *  - La sous-commande doit être DELIVERED et appartenir à l'acheteur
     *  - La boutique évaluée doit correspondre à la sous-commande
     *  - Un seul avis par acheteur par boutique
     *  - Une sous-commande ne peut servir qu'une seule fois par boutique
     */
    @Transactional
    public ReviewResponse createShopReview(User buyer, CreateShopReviewRequest req) {

        SubOrder subOrder = subOrderRepository.findByIdWithDetails(req.subOrderId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Sous-commande introuvable"));

        // L'acheteur est bien le propriétaire de la commande
        if (!subOrder.getOrder().getBuyer().getId().equals(buyer.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Cette sous-commande ne vous appartient pas");
        }

        // La sous-commande doit être livrée
        if (subOrder.getStatus() != SubOrderStatus.DELIVERED) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Un avis n'est possible que sur une sous-commande livrée (statut : " + subOrder.getStatus() + ")");
        }

        Shop shop = shopRepository.findById(req.shopId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Boutique introuvable"));

        // La boutique doit correspondre à la sous-commande
        if (!shop.getId().equals(subOrder.getShop().getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Cette boutique ne correspond pas à la sous-commande fournie");
        }

        // Un seul avis par acheteur par boutique
        if (shopReviewRepository.existsByShopIdAndAuthorId(shop.getId(), buyer.getId())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Vous avez déjà laissé un avis sur cette boutique");
        }

        // Une sous-commande ne peut être utilisée qu'une seule fois par boutique
        if (shopReviewRepository.existsByShopIdAndSubOrderId(shop.getId(), subOrder.getId())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Cette sous-commande a déjà été utilisée pour un avis sur cette boutique");
        }

        ShopReview review = ShopReview.builder()
                .shop(shop)
                .author(buyer)
                .subOrder(subOrder)
                .rating(req.rating())
                .comment(req.comment())
                .build();

        return ReviewResponse.from(shopReviewRepository.save(review));
    }

    /** Avis publics d'une boutique (paginés). */
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getShopReviews(UUID shopId, Pageable pageable) {
        if (!shopRepository.existsById(shopId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Boutique introuvable");
        }
        return shopReviewRepository.findByShopIdWithAuthor(shopId, pageable)
                .map(ReviewResponse::from);
    }

    /** Note moyenne + nombre d'avis d'une boutique. */
    @Transactional(readOnly = true)
    public RatingSummaryResponse getShopRatingSummary(UUID shopId) {
        double avg   = shopReviewRepository.findAverageRatingByShopId(shopId).orElse(0.0);
        long   count = shopReviewRepository.countByShopId(shopId);
        return new RatingSummaryResponse(avg, count);
    }
}
