package com.maplenou.backend.order;

import com.maplenou.backend.cart.Cart;
import com.maplenou.backend.cart.CartItem;
import com.maplenou.backend.cart.CartItemRepository;
import com.maplenou.backend.cart.CartRepository;
import com.maplenou.backend.catalog.ProductStatus;
import com.maplenou.backend.catalog.Shop;
import com.maplenou.backend.catalog.ProductVariant;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.delivery.DeliveryPricingService;
import com.maplenou.backend.delivery.dto.DeliveryFeeQuote;
import com.maplenou.backend.order.dto.OrderResponse;
import com.maplenou.backend.order.dto.OrderSummaryResponse;
import com.maplenou.backend.order.dto.OrdersSummaryResponse;
import com.maplenou.backend.order.dto.PlaceOrderRequest;
import com.maplenou.backend.payment.FedaPayCheckoutService;
import com.maplenou.backend.promo.PromoCodeService;
import com.maplenou.backend.user.Address;
import com.maplenou.backend.user.AddressRepository;
import com.maplenou.backend.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
@Slf4j
public class OrderService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final CommissionService commissionService;
    private final PromoCodeService promoCodeService;
    private final FedaPayCheckoutService fedaPayCheckoutService;
    private final DeliveryPricingService deliveryPricingService;

    @Value("${app.fedapay.enabled}")
    private boolean fedapayEnabled;

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

    /**
     * Annulation par l'acheteur — absente jusqu'ici (signalé par l'audit externe du
     * 16/10/2026) : un acheteur n'avait aucun moyen de se défaire d'une commande impayée. Refusé
     * dès qu'un seul vendeur a commencé à préparer (SubOrder au-delà de PENDING) : à ce stade
     * l'annulation doit passer par le support, pas en self-service. Aucun stock à libérer : rien
     * n'est décrémenté avant PAID (voir ProductVariantRepository.decrementStock).
     */
    @Transactional
    public OrderResponse cancelMyOrder(User buyer, UUID orderId) {
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Commande introuvable"));

        if (!order.getBuyer().getId().equals(buyer.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Seule une commande non payée peut être annulée directement (statut actuel : " + order.getStatus() + ")");
        }
        boolean anySubOrderStarted = order.getSubOrders().stream()
                .anyMatch(so -> so.getStatus() != SubOrderStatus.PENDING);
        if (anySubOrderStarted) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Au moins un vendeur a déjà commencé à préparer cette commande — contactez le support pour l'annuler");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.getSubOrders().forEach(so -> so.setStatus(SubOrderStatus.CANCELLED));

        return OrderResponse.from(order);
    }

    private static final Set<OrderStatus> ONGOING_STATUSES = Set.of(OrderStatus.CREATED, OrderStatus.PAID);

    /** Compteur de mes commandes par statut (ex: badge "commandes en cours" du profil). */
    @Transactional(readOnly = true)
    public OrdersSummaryResponse getMyOrdersSummary(User buyer) {
        Map<OrderStatus, Long> byStatus = new EnumMap<>(OrderStatus.class);
        long total = 0;
        for (Object[] row : orderRepository.countByBuyerIdGroupByStatus(buyer.getId())) {
            OrderStatus status = (OrderStatus) row[0];
            long count = (Long) row[1];
            byStatus.put(status, count);
            total += count;
        }
        long ongoing = ONGOING_STATUSES.stream().mapToLong(s -> byStatus.getOrDefault(s, 0L)).sum();
        return new OrdersSummaryResponse(total, ongoing, byStatus);
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

            // Frais de livraison dynamique (zone + distance réelle boutique↔adresse, voir
            // DeliveryPricingService) — une boutique à la fois, chacune pouvant être à une
            // distance différente dans un panier multi-vendeurs. Figé ici, jamais recalculé
            // après coup, et réellement ajouté au montant facturé (voir §7 plus bas).
            DeliveryFeeQuote deliveryQuote = deliveryPricingService.quote(
                    shop, snapshot.getLatitude(), snapshot.getLongitude());
            totalAmount = totalAmount.add(deliveryQuote.fee());

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
                    .deliveryFee(deliveryQuote.fee())
                    .deliveryDistanceKm(deliveryQuote.distanceKm())
                    .status(SubOrderStatus.PENDING)
                    .items(new ArrayList<>(items))
                    .build();

            // Lier chaque item à son sous-ordre
            items.forEach(i -> i.setSubOrder(so));
            subOrders.add(so);
        }

        // 6. Créer la commande parent
        String orderNumber = "CMD-" + String.format("%06d", orderRepository.nextOrderNumberSequenceValue());
        Order order = Order.builder()
                .buyer(buyer)
                .orderNumber(orderNumber)
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

        // 8. Démarrer le paiement FedaPay (désactivé par défaut en dev/CI, voir app.fedapay.enabled).
        String paymentUrl = null;
        if (fedapayEnabled) {
            var checkout = fedaPayCheckoutService.startCheckout(saved, request.paymentCountry());
            saved.setFedapayTransactionId(checkout.transactionId());
            orderRepository.save(saved);
            paymentUrl = checkout.paymentUrl();
        } else {
            log.warn("FedaPay désactivé (app.fedapay.enabled=false) — commande {} créée sans paiement réel", saved.getOrderNumber());
        }

        // 9. Vider le panier (après un éventuel échec FedaPay ci-dessus, qui aurait fait
        // rollback toute la transaction — le panier ne doit être vidé que si tout a réussi).
        cart.getItems().clear();
        cartRepository.save(cart);

        return OrderResponse.from(saved, paymentUrl);
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
