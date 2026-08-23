package com.maplenou.backend.order;

import com.maplenou.backend.order.dto.OrderResponse;
import com.maplenou.backend.order.dto.OrderSummaryResponse;
import com.maplenou.backend.order.dto.PlaceOrderRequest;
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
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Commandes")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    /** Passer une commande à partir du panier courant. */
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody PlaceOrderRequest request) {
        OrderResponse response = orderService.placeOrder(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Lister mes commandes (acheteur). */
    @GetMapping
    public Page<OrderSummaryResponse> listMyOrders(
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        return orderService.listMyOrders(currentUser, pageable);
    }

    /** Détail d'une commande (acheteur — vérifié en service). */
    @GetMapping("/{orderId}")
    public OrderResponse getMyOrder(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID orderId) {
        return orderService.getMyOrder(currentUser, orderId);
    }
}
