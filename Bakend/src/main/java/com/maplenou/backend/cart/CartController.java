package com.maplenou.backend.cart;

import com.maplenou.backend.cart.dto.AddToCartRequest;
import com.maplenou.backend.cart.dto.CartResponse;
import com.maplenou.backend.cart.dto.UpdateCartItemRequest;
import com.maplenou.backend.user.User;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Panier")
@SecurityRequirement(name = "bearerAuth")
public class CartController {

    private final CartService cartService;

    @GetMapping
    public CartResponse getCart(@AuthenticationPrincipal User currentUser) {
        return cartService.getCart(currentUser);
    }

    @PostMapping("/items")
    public CartResponse addItem(@AuthenticationPrincipal User currentUser,
                                 @Valid @RequestBody AddToCartRequest request) {
        return cartService.addItem(currentUser, request);
    }

    @PatchMapping("/items/{itemId}")
    public CartResponse updateItem(@AuthenticationPrincipal User currentUser,
                                    @PathVariable UUID itemId,
                                    @Valid @RequestBody UpdateCartItemRequest request) {
        return cartService.updateItem(currentUser, itemId, request);
    }

    @DeleteMapping("/items/{itemId}")
    public CartResponse removeItem(@AuthenticationPrincipal User currentUser,
                                    @PathVariable UUID itemId) {
        return cartService.removeItem(currentUser, itemId);
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal User currentUser) {
        cartService.clearCart(currentUser);
        return ResponseEntity.noContent().build();
    }
}
