package com.maplenou.backend.cart;

import com.maplenou.backend.cart.dto.FavoriteResponse;
import com.maplenou.backend.catalog.Product;
import com.maplenou.backend.catalog.ProductRepository;
import com.maplenou.backend.common.exception.ApiException;
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
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public Page<FavoriteResponse> list(User user, Pageable pageable) {
        // EntityGraph charge product + shop ; images se chargent en batch lazy dans la transaction
        return favoriteRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(FavoriteResponse::from);
    }

    @Transactional
    public FavoriteResponse add(User user, UUID productId) {
        if (favoriteRepository.existsByUserIdAndProductId(user.getId(), productId)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ce produit est déjà dans vos favoris");
        }

        // JOIN FETCH complet → shop, category, variants, images tous chargés dans la transaction
        Product product = productRepository.findByIdWithDetails(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Produit introuvable"));

        Favorite favorite = Favorite.builder()
                .user(user)
                .product(product)
                .build();

        Favorite saved = favoriteRepository.save(favorite);
        return buildResponse(saved, product);
    }

    @Transactional
    public void remove(User user, UUID productId) {
        Favorite favorite = favoriteRepository
                .findByUserIdAndProductId(user.getId(), productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ce produit n'est pas dans vos favoris"));
        favoriteRepository.delete(favorite);
    }

    private FavoriteResponse buildResponse(Favorite favorite, Product product) {
        String thumbnail = product.getImages().isEmpty()
                ? null
                : product.getImages().get(0).getUrl();

        return new FavoriteResponse(
                favorite.getId(),
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getBasePrice(),
                thumbnail,
                product.getShop().getId(),
                product.getShop().getName(),
                favorite.getCreatedAt()
        );
    }
}
