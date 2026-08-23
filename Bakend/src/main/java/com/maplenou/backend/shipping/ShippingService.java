package com.maplenou.backend.shipping;

import com.maplenou.backend.catalog.Shop;
import com.maplenou.backend.catalog.ShopService;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.order.SubOrder;
import com.maplenou.backend.order.SubOrderRepository;
import com.maplenou.backend.shipping.dto.CarrierTrackingResponse;
import com.maplenou.backend.shipping.dto.ShipmentLabelResponse;
import com.maplenou.backend.shipping.dto.ShipmentRequest;
import com.maplenou.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShippingService {

    private final SubOrderRepository subOrderRepository;
    private final ShopService shopService;
    private final CarrierShippingClient carrierShippingClient;

    @Transactional
    public ShipmentLabelResponse generateLabel(User seller, UUID subOrderId) {
        SubOrder subOrder = getOwnedSubOrder(seller, subOrderId);

        ShipmentRequest request = new ShipmentRequest(
                subOrder.getOrder().getBuyer().getFullName(),
                subOrder.getOrder().getBuyer().getPhoneNumber(),
                subOrder.getOrder().getDeliveryAddress().getCity(),
                subOrder.getOrder().getDeliveryAddress().getDistrict(),
                subOrder.getOrder().getDeliveryAddress().getDetails()
        );

        ShipmentLabelResponse label = carrierShippingClient.createShipment(request);

        subOrder.setCarrierCode(label.carrierCode());
        subOrder.setTrackingNumber(label.trackingNumber());
        subOrder.setCarrierLabelUrl(label.labelUrl());
        subOrderRepository.save(subOrder);

        return label;
    }

    @Transactional(readOnly = true)
    public CarrierTrackingResponse trackShipment(User seller, UUID subOrderId) {
        SubOrder subOrder = getOwnedSubOrder(seller, subOrderId);
        if (subOrder.getTrackingNumber() == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Aucune étiquette générée pour cette sous-commande");
        }
        return carrierShippingClient.track(subOrder.getTrackingNumber());
    }

    private SubOrder getOwnedSubOrder(User seller, UUID subOrderId) {
        Shop shop = shopService.getMine(seller);
        return subOrderRepository.findByIdWithDetails(subOrderId)
                .filter(so -> so.getShop().getId().equals(shop.getId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Sous-commande introuvable"));
    }
}
