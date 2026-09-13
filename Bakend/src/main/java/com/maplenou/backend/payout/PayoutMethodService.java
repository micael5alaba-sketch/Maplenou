package com.maplenou.backend.payout;

import com.maplenou.backend.catalog.Shop;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.payout.dto.CreatePayoutMethodRequest;
import com.maplenou.backend.payout.dto.PayoutMethodResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PayoutMethodService {

    private final PayoutMethodRepository payoutMethodRepository;

    @Transactional(readOnly = true)
    public List<PayoutMethodResponse> listForShop(UUID shopId) {
        return payoutMethodRepository.findByShopIdOrderByIsDefaultDescCreatedAtDesc(shopId).stream()
                .map(PayoutMethodResponse::from)
                .toList();
    }

    /** Le tout premier moyen de paiement d'une boutique devient son défaut automatiquement. */
    @Transactional
    public PayoutMethodResponse create(Shop shop, CreatePayoutMethodRequest request) {
        boolean isFirst = !payoutMethodRepository.existsByShopId(shop.getId());

        PayoutMethod method = PayoutMethod.builder()
                .shop(shop)
                .type(request.type())
                .accountNumber(request.accountNumber())
                .isDefault(isFirst)
                .build();

        return PayoutMethodResponse.from(payoutMethodRepository.save(method));
    }

    @Transactional
    public PayoutMethodResponse setDefault(Shop shop, UUID methodId) {
        PayoutMethod method = payoutMethodRepository.findByIdAndShopId(methodId, shop.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Moyen de paiement introuvable"));

        payoutMethodRepository.clearDefaultForShop(shop.getId());
        method.setDefault(true);
        return PayoutMethodResponse.from(payoutMethodRepository.save(method));
    }

    @Transactional
    public void delete(Shop shop, UUID methodId) {
        PayoutMethod method = payoutMethodRepository.findByIdAndShopId(methodId, shop.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Moyen de paiement introuvable"));

        boolean wasDefault = method.isDefault();
        payoutMethodRepository.delete(method);

        if (wasDefault) {
            // Reporte le défaut sur le moyen de paiement restant le plus ancien, s'il y en a un.
            List<PayoutMethod> remaining =
                    payoutMethodRepository.findByShopIdOrderByIsDefaultDescCreatedAtDesc(shop.getId());
            if (!remaining.isEmpty()) {
                PayoutMethod next = remaining.get(remaining.size() - 1);
                next.setDefault(true);
                payoutMethodRepository.save(next);
            }
        }
    }
}
