package com.maplenou.support.context.adapter.out.maplenou;

import com.maplenou.support.context.application.port.EcosystemGateway;
import com.maplenou.support.context.domain.Ecosystem.DeliverySummary;
import com.maplenou.support.context.domain.Ecosystem.OrderSummary;
import com.maplenou.support.context.domain.Ecosystem.PaymentSummary;
import com.maplenou.support.context.domain.Ecosystem.RefundSummary;
import com.maplenou.support.context.domain.Ecosystem.SellerSummary;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Reads Maplenou data from Bakend's internal API (docs/architecture/ecosystem-integration.md). */
public class HttpEcosystemGateway implements EcosystemGateway {

    private final RestClient client;

    public HttpEcosystemGateway(RestClient client) {
        this.client = client;
    }

    @Override
    public Optional<OrderSummary> findOrder(String ref) {
        return get("/internal/v1/orders/{ref}", OrderSummary.class, ref);
    }

    @Override
    public Optional<PaymentSummary> findPayment(String ref) {
        return get("/internal/v1/payments/{ref}", PaymentSummary.class, ref);
    }

    @Override
    public Optional<DeliverySummary> findDelivery(String ref) {
        return get("/internal/v1/deliveries/{ref}", DeliverySummary.class, ref);
    }

    @Override
    public Optional<SellerSummary> findSeller(String ref) {
        return get("/internal/v1/sellers/{ref}", SellerSummary.class, ref);
    }

    @Override
    public Optional<RefundSummary> findRefund(String ref) {
        return get("/internal/v1/refunds/{ref}", RefundSummary.class, ref);
    }

    @Override
    public List<OrderSummary> ordersOf(UUID userId) {
        return list("/internal/v1/users/{id}/orders", new ParameterizedTypeReference<>() { }, userId);
    }

    @Override
    public List<DeliverySummary> deliveriesOf(UUID driverId) {
        return list("/internal/v1/drivers/{id}/deliveries", new ParameterizedTypeReference<>() { }, driverId);
    }

    private <T> Optional<T> get(String path, Class<T> type, Object ref) {
        if (ref == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(client.get().uri(path, ref).retrieve().body(type));
        } catch (HttpClientErrorException e) {
            if (BakendClient.isNotFound(e)) {
                return Optional.empty();
            }
            throw new EcosystemUnavailableException("Ecosystem call failed: " + e.getStatusCode(), e);
        } catch (RestClientException e) {
            throw new EcosystemUnavailableException("Ecosystem unreachable", e);
        }
    }

    private <T> List<T> list(String path, ParameterizedTypeReference<List<T>> type, Object id) {
        try {
            List<T> body = client.get().uri(path, id).retrieve().body(type);
            return body == null ? List.of() : body;
        } catch (RestClientException e) {
            throw new EcosystemUnavailableException("Ecosystem unreachable", e);
        }
    }
}
