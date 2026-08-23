package com.maplenou.backend.shipping;

import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.shipping.dto.CarrierTrackingResponse;
import com.maplenou.backend.shipping.dto.ShipmentLabelResponse;
import com.maplenou.backend.shipping.dto.ShipmentRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Implémentation Colissimo — scaffold prêt à brancher.
 *
 * Non fonctionnel tant qu'un contrat Colissimo réel (numéro de compte + clé API webservice)
 * n'est pas disponible : app.carrier.colissimo.enabled reste à false par défaut.
 *
 * Pour activer :
 *   1. Obtenir un contrat Colissimo Entreprise et les identifiants du webservice
 *      (https://www.colissimo.entreprise.laposte.fr/fr/etiquette-2d-doc)
 *   2. Renseigner COLISSIMO_ENABLED=true, COLISSIMO_CONTRACT_NUMBER, COLISSIMO_API_KEY
 *   3. Implémenter callGenerateLabelApi()/callTrackingApi() ci-dessous avec le client HTTP/SOAP
 *      du webservice Colissimo (WSDL "Web Service Colissimo").
 */
@Component
public class ColissimoShippingClient implements CarrierShippingClient {

    @Value("${app.carrier.colissimo.enabled}")
    private boolean enabled;

    @Value("${app.carrier.colissimo.contract-number}")
    private String contractNumber;

    @Value("${app.carrier.colissimo.api-key}")
    private String apiKey;

    @Override
    public String getCarrierCode() {
        return "COLISSIMO";
    }

    @Override
    public ShipmentLabelResponse createShipment(ShipmentRequest request) {
        requireEnabled();
        // TODO : appeler le webservice Colissimo "Génération d'étiquette" avec contractNumber/apiKey
        // et les informations du destinataire (request), puis retourner le numéro de colis + l'URL du PDF.
        throw new ApiException(HttpStatus.NOT_IMPLEMENTED,
                "Génération d'étiquette Colissimo non implémentée — contrat et webservice à intégrer");
    }

    @Override
    public CarrierTrackingResponse track(String trackingNumber) {
        requireEnabled();
        // TODO : appeler le webservice Colissimo "Suivi de colis" avec trackingNumber.
        throw new ApiException(HttpStatus.NOT_IMPLEMENTED,
                "Suivi Colissimo non implémenté — contrat et webservice à intégrer");
    }

    private void requireEnabled() {
        if (!enabled || contractNumber == null || contractNumber.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "L'intégration Colissimo n'est pas activée sur cet environnement");
        }
    }
}
