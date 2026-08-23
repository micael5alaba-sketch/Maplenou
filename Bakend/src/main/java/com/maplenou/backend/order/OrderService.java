package com.maplenou.backend.order;

import com.maplenou.backend.cart.Cart;
import com.maplenou.backend.cart.CartItem;
import com.maplenou.backend.cart.CartItemRepository;
import com.maplenou.backend.cart.CartRepository;
import com.maplenou.backend.catalog.ProductStatus;
import com.maplenou.backend.catalog.Shop;
import com.maplenou.backend.catalog.ProductVariant;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.order.dto.OrderResponse;
import com.maplenou.backend.order.dto.OrderSummaryResponse;
import com.maplenou.backend.order.dto.PlaceOrderRequest;
import com.maplenou.backend.promo.PromoCodeService;
import com.maplenou.backend.user.Address;
import com.maplenou.backend.user.AddressRepository;
import com.maplenou.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final CommissionService commissionService;
    private final PromoCodeService promoCodeService;

    // ----- Lecture acheteur -----

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> listMyOrders(User buyer, Pageable pageable) {
        // subOrders chargés via batch_fetch_size pour le count — pas de JOIN qui casse la pagination
        return orderRepository
                .findByBuyerIdOrderByCreatedAtDesc(buyer.getId(), pageable)
                .map(OrderSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public OrderResponse getMyOrder(User buyer, UUID orderId) {
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Commande introuvable"));

        if (!order.getBuyer().getId().equals(buyer.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        return OrderResponse.from(order);
    }

    // ----- Passation de commande -----

    @Transactional
    public OrderResponse placeOrder(User buyer, PlaceOrderRequest request) {

        // 1. Charger le panier
        Cart cart = cartRepository.findByUserId(buyer.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Votre panier est vide"));

        List<CartItem> cartItems = cartItemRepository.findByCartIdWithDetails(cart.getId());
        if (cartItems.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Votre panier est vide");
        }

        // 2. Valider disponibilité de chaque article et statut de la boutique
        for (CartItem ci : cartItems) {
            ProductVariant v = ci.getVariant();
            if (v.getProduct().getShop().getStatus() != com.maplenou.backend.catalog.ShopStatus.APPROVED) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "La boutique \"" + v.getProduct().getShop().getName() + "\" n'est plus disponible");
            }
            if (v.getProduct().getStatus() != ProductStatus.ACTIVE || v.getProduct().isDeleted()) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Le produit \"" + v.getProduct().getName() + "\" n'est plus disponible");
            }
            if (v.getStockQuantity() < ci.getQuantity()) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "Stock insuffisant pour \"" + v.getProduct().getName()
                        + " – " + v.getLabel() + "\" (disponible : " + v.getStockQuantity() + ")");
            }
        }

        // 3. Snapshot de l'adresse de livraison
        Address address = addressRepository.findById(request.addressId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Adresse introuvable"));

        if (!address.getUser().getId().equals(buyer.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Cette adresse ne vous appartient pas");
        }

        AddressSnapshot snapshot = AddressSnapshot.builder()
                .label(address.getLabel())
                .city(address.getCity())
                .district(address.getDistrict())
                .details(address.getDetails())
                .latitude(address.getLatitude() != null ? BigDecimal.valueOf(address.getLatitude()) : null)
                .longitude(address.getLongitude() != null ? BigDecimal.valueOf(address.getLongitude()) : null)
                .build();

        // 4. Regrouper les articles par boutique
        Map<UUID, List<CartItem>> byShop = cartItems.stream()
                .collect(Collectors.groupingBy(ci -> ci.getVariant().getProduct().getShop().getId()));

        // 5. Construire les sous-ordres
        List<SubOrder> subOrders = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (List<CartItem> shopItems : byShop.values()) {
            Shop shop = shopItems.get(0).getVariant().getProduct().getShop();

            // Calcul du sous-total de la boutique
            BigDecimal subtotal = shopItems.stream()
                    .map(ci -> effectivePrice(ci.getVariant()).multiply(BigDecimal.valueOf(ci.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal rate   = commissionService.rateFor(subtotal);
            BigDecimal comm   = commissionService.amountFor(subtotal);
            BigDecimal net    = commissionService.netFor(subtotal);
            totalAmount       = totalAmount.add(subtotal);

            // Construire les lignes du sous-ordre
            List<SubOrderItem> items = shopItems.stream()
                    .map(ci -> buildItem(ci))
                    .toList();

            SubOrder so = SubOrder.builder()
                    .shop(shop)
                    .subtotal(subtotal)
                    .commissionRate(rate)
                    .commissionAmount(comm)
                    .netAmount(net)
                    .status(SubOrderStatus.PENDING)
                    .items(new ArrayList<>(items))
                    .build();

            // Lier chaque item à son sous-ordre
            items.forEach(i -> i.setSubOrder(so));
            subOrders.add(so);
        }

        // 6. Créer la commande parent
        Order order = Order.builder()
                .buyer(buyer)
                .deliveryAddress(snapshot)
                .totalAmount(totalAmount)
                .status(OrderStatus.CREATED)
                .subOrders(new ArrayList<>(subOrders))
                .build();

        subOrders.forEach(so -> so.setOrder(order));

        Order saved = orderRepository.save(order);

        // 7. Appliquer le code promo si fourni (dans la même transaction)
        if (request.promoCode() != null && !request.promoCode().isBlank()) {
            BigDecimal discount = promoCodeService.applyToOrder(
                    buyer,
                    request.promoCode(),
                    request.promoShopId(),
                    totalAmount,
                    saved);
            saved.setDiscountAmount(discount);
            saved.setTotalAmount(totalAmount.subtract(discount).max(BigDecimal.ZERO));
            orderRepository.save(saved);
        }

        // 8. Vider le panier
        cart.getItems().clear();
        cartRepository.save(cart);

        return OrderResponse.from(saved);
    }

    // ----- Utilitaires privés -----

    private BigDecimal effectivePrice(ProductVariant variant) {
        return variant.getPriceOverride() != null
                ? variant.getPriceOverride()
                : variant.getProduct().getBasePrice();
    }

    private SubOrderItem buildItem(CartItem ci) {
        ProductVariant v = ci.getVariant();
        BigDecimal price = effectivePrice(v);
        BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(ci.getQuantity()));

        return SubOrderItem.builder()
                .productVariant(v)
                .snapshotProductName(v.getProduct().getName())
                .snapshotVariantLabel(v.getLabel())
                .snapshotSku(v.getSku())
                .unitPrice(price)
                .quantity(ci.getQuantity())
                .lineTotal(lineTotal)
                .build();
    }
}
