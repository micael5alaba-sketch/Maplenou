package com.maplenou.backend.shipping;

import com.maplenou.backend.shipping.dto.CarrierTrackingResponse;
import com.maplenou.backend.shipping.dto.ShipmentLabelResponse;
import com.maplenou.backend.shipping.dto.ShipmentRequest;

/**
 * Abstraction du transport principal assuré par un transporteur tiers (§2.2/§4.3 du cahier des
 * charges), distinct du dernier kilomètre géré en interne par le module `delivery`.
 * Une implémentation par transporteur (Colissimo, etc.) — voir {@link ColissimoShippingClient}.
 */
public interface CarrierShippingClient {

    String getCarrierCode();

    ShipmentLabelResponse createShipment(ShipmentRequest request);

    CarrierTrackingResponse track(String trackingNumber);
}
