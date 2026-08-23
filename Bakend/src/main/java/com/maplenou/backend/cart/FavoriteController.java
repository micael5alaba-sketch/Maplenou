package com.maplenou.backend.cart;

import com.maplenou.backend.cart.dto.AddFavoriteRequest;
import com.maplenou.backend.cart.dto.FavoriteResponse;
import com.maplenou.backend.user.User;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
@Tag(name = "Favoris")
@SecurityRequirement(name = "bearerAuth")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping
    public Page<FavoriteResponse> list(@AuthenticationPrincipal User currentUser,
                                        @PageableDefault(size = 20) Pageable pageable) {
        return favoriteService.list(currentUser, pageable);
    }

    @PostMapping
    public ResponseEntity<FavoriteResponse> add(@AuthenticationPrincipal User currentUser,
                                                  @Valid @RequestBody AddFavoriteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(favoriteService.add(currentUser, request.productId()));
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal User currentUser,
                        @PathVariable UUID productId) {
        favoriteService.remove(currentUser, productId);
    }
}
