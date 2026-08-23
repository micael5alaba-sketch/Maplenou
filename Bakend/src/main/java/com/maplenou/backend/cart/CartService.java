package com.maplenou.backend.cart;

import com.maplenou.backend.cart.dto.AddToCartRequest;
import com.maplenou.backend.cart.dto.CartItemResponse;
import com.maplenou.backend.cart.dto.CartResponse;
import com.maplenou.backend.cart.dto.UpdateCartItemRequest;
import com.maplenou.backend.catalog.ProductStatus;
import com.maplenou.backend.catalog.ProductVariant;
import com.maplenou.backend.catalog.ProductVariantRepository;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository variantRepository;

    // ----- Lecture -----

    @Transactional(readOnly = true)
    public CartResponse getCart(User user) {
        Optional<Cart> cartOpt = cartRepository.findByUserId(user.getId());
        if (cartOpt.isEmpty()) {
            return CartResponse.empty();
        }
        Cart cart = cartOpt.get();
        // JOIN FETCH complet dans le repository → tout est chargé dans la transaction
        List<CartItemResponse> items = cartItemRepository
                .findByCartIdWithDetails(cart.getId())
                .stream()
                .map(CartItemResponse::from)
                .toList();
        return CartResponse.from(cart, items);
    }

    // ----- Modification -----

    @Transactional
    public CartResponse addItem(User user, AddToCartRequest request) {
        ProductVariant variant = variantRepository.findById(request.productVariantId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Variante introuvable"));

        // Le produit doit être actif pour être ajouté au panier
        if (variant.getProduct().getStatus() != ProductStatus.ACTIVE
                || variant.getProduct().isDeleted()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Ce produit n'est plus disponible");
        }

        Cart cart = getOrCreateCart(user);

        // Si la variante est déjà dans le panier → incrémenter la quantité
        Optional<CartItem> existing = cartItemRepository
                .findByCartIdAndVariantId(cart.getId(), variant.getId());

        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity((short) (item.getQuantity() + request.quantity()));
            cartItemRepository.save(item);
        } else {
            CartItem item = CartItem.builder()
                    .cart(cart)
                    .variant(variant)
                    .quantity(request.quantity().shortValue())
                    .build();
            cartItemRepository.save(item);
        }

        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponse updateItem(User user, UUID itemId, UpdateCartItemRequest request) {
        Cart cart = getCartOrThrow(user);
        CartItem item = cartItemRepository.findByIdAndCartId(itemId, cart.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Article introuvable dans le panier"));

        item.setQuantity(request.quantity().shortValue());
        cartItemRepository.save(item);

        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(User user, UUID itemId) {
        Cart cart = getCartOrThrow(user);
        CartItem item = cartItemRepository.findByIdAndCartId(itemId, cart.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Article introuvable dans le panier"));

        cartItemRepository.delete(item);
        return buildCartResponse(cart);
    }

    @Transactional
    public void clearCart(User user) {
        cartRepository.findByUserId(user.getId()).ifPresent(cart -> {
            cart.getItems().clear();
            cartRepository.save(cart);
        });
    }

    // ----- Utilitaires privés -----

    // Crée le panier à la première utilisation (pattern lazy creation)
    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUserId(user.getId()).orElseGet(() -> {
            Cart newCart = Cart.builder().user(user).build();
            return cartRepository.save(newCart);
        });
    }

    private Cart getCartOrThrow(User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Panier introuvable"));
    }

    private CartResponse buildCartResponse(Cart cart) {
        List<CartItemResponse> items = cartItemRepository
                .findByCartIdWithDetails(cart.getId())
                .stream()
                .map(CartItemResponse::from)
                .toList();
        return CartResponse.from(cart, items);
    }
}
