package com.maplenou.backend.promo;

import com.maplenou.backend.catalog.Shop;
import com.maplenou.backend.catalog.ShopRepository;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.promo.dto.CreatePromoCodeRequest;
import com.maplenou.backend.promo.dto.ValidatePromoRequest;
import com.maplenou.backend.promo.dto.ValidatePromoResponse;
import com.maplenou.backend.user.Role;
import com.maplenou.backend.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromoCodeServiceTest {

    @Mock
    private PromoCodeRepository promoCodeRepository;
    @Mock
    private PromoCodeUsageRepository usageRepository;
    @Mock
    private ShopRepository shopRepository;

    @InjectMocks
    private PromoCodeService promoCodeService;

    private User buyer;

    @BeforeEach
    void setUp() {
        buyer = User.builder().fullName("Buyer").phoneNumber("+22890000003").passwordHash("h").build();
        buyer.setActive(true);
    }

    private PromoCode platformCode(BigDecimal discountPercent) {
        return PromoCode.builder()
                .id(UUID.randomUUID())
                .code("PROMO10")
                .scopeType(PromoScopeType.PLATFORM)
                .discountPercent(discountPercent)
                .active(true)
                .currentUses(0)
                .build();
    }

    @Test
    void validateComputesDiscountForAnApplicableCode() {
        PromoCode promo = platformCode(new BigDecimal("10.00"));
        when(promoCodeRepository.findByCodeIgnoreCase("PROMO10")).thenReturn(Optional.of(promo));
        when(usageRepository.existsByPromoCodeIdAndUserId(promo.getId(), buyer.getId())).thenReturn(false);

        ValidatePromoResponse response = promoCodeService.validate(
                buyer, new ValidatePromoRequest("PROMO10", new BigDecimal("10000"), null));

        assertEquals(0, new BigDecimal("1000.00").compareTo(response.discountAmount()));
        assertEquals(0, new BigDecimal("9000.00").compareTo(response.finalAmount()));
    }

    @Test
    void validateRejectsUnknownCode() {
        when(promoCodeRepository.findByCodeIgnoreCase("NOPE")).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> promoCodeService.validate(
                buyer, new ValidatePromoRequest("NOPE", BigDecimal.TEN, null)));
        assertEquals(404, ex.getStatus().value());
    }

    @Test
    void validateRejectsDeactivatedCode() {
        PromoCode promo = platformCode(new BigDecimal("10.00"));
        promo.setActive(false);
        when(promoCodeRepository.findByCodeIgnoreCase("PROMO10")).thenReturn(Optional.of(promo));

        assertThrows(ApiException.class, () -> promoCodeService.validate(
                buyer, new ValidatePromoRequest("PROMO10", BigDecimal.TEN, null)));
    }

    @Test
    void validateRejectsExpiredCode() {
        PromoCode promo = platformCode(new BigDecimal("10.00"));
        promo.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
        when(promoCodeRepository.findByCodeIgnoreCase("PROMO10")).thenReturn(Optional.of(promo));

        assertThrows(ApiException.class, () -> promoCodeService.validate(
                buyer, new ValidatePromoRequest("PROMO10", BigDecimal.TEN, null)));
    }

    @Test
    void validateRejectsExhaustedCode() {
        PromoCode promo = platformCode(new BigDecimal("10.00"));
        promo.setMaxUses(5);
        promo.setCurrentUses(5);
        when(promoCodeRepository.findByCodeIgnoreCase("PROMO10")).thenReturn(Optional.of(promo));

        assertThrows(ApiException.class, () -> promoCodeService.validate(
                buyer, new ValidatePromoRequest("PROMO10", BigDecimal.TEN, null)));
    }

    @Test
    void validateRejectsBelowMinimumOrderAmount() {
        PromoCode promo = platformCode(new BigDecimal("10.00"));
        promo.setMinOrderAmount(new BigDecimal("5000"));
        when(promoCodeRepository.findByCodeIgnoreCase("PROMO10")).thenReturn(Optional.of(promo));

        ApiException ex = assertThrows(ApiException.class, () -> promoCodeService.validate(
                buyer, new ValidatePromoRequest("PROMO10", new BigDecimal("1000"), null)));
        assertEquals(400, ex.getStatus().value());
    }

    @Test
    void validateRejectsAlreadyUsedCodeForThisBuyer() {
        PromoCode promo = platformCode(new BigDecimal("10.00"));
        when(promoCodeRepository.findByCodeIgnoreCase("PROMO10")).thenReturn(Optional.of(promo));
        when(usageRepository.existsByPromoCodeIdAndUserId(promo.getId(), buyer.getId())).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> promoCodeService.validate(
                buyer, new ValidatePromoRequest("PROMO10", new BigDecimal("10000"), null)));
        assertEquals(409, ex.getStatus().value());
    }

    @Test
    void validateRejectsShopCodeUsedWithoutMatchingShopId() {
        PromoCode promo = PromoCode.builder()
                .id(UUID.randomUUID()).code("SHOP5").scopeType(PromoScopeType.SHOP)
                .scopeId(UUID.randomUUID()).discountPercent(new BigDecimal("5.00")).active(true).build();
        when(promoCodeRepository.findByCodeIgnoreCase("SHOP5")).thenReturn(Optional.of(promo));

        ApiException ex = assertThrows(ApiException.class, () -> promoCodeService.validate(
                buyer, new ValidatePromoRequest("SHOP5", new BigDecimal("10000"), null)));
        assertEquals(400, ex.getStatus().value());
    }

    @Test
    void createRejectsShopScopeWithoutScopeId() {
        User seller = User.builder().fullName("Seller").phoneNumber("+22890000004").passwordHash("h").build();
        CreatePromoCodeRequest req = new CreatePromoCodeRequest(
                "CODE1", PromoScopeType.SHOP, null, new BigDecimal("10.00"), null, null, null);

        ApiException ex = assertThrows(ApiException.class, () -> promoCodeService.create(seller, req));
        assertEquals(400, ex.getStatus().value());
    }

    @Test
    void createRejectsPlatformScopeFromNonAdmin() {
        User seller = User.builder().fullName("Seller").phoneNumber("+22890000005").passwordHash("h").build();
        CreatePromoCodeRequest req = new CreatePromoCodeRequest(
                "CODE2", PromoScopeType.PLATFORM, null, new BigDecimal("10.00"), null, null, null);

        ApiException ex = assertThrows(ApiException.class, () -> promoCodeService.create(seller, req));
        assertEquals(403, ex.getStatus().value());
    }

    @Test
    void createRejectsSellerCreatingCodeForAnotherShop() {
        User seller = User.builder().fullName("Seller").phoneNumber("+22890000006").passwordHash("h").build();
        UUID otherShopId = UUID.randomUUID();
        Shop myShop = Shop.builder().id(UUID.randomUUID()).owner(seller).name("My shop")
                .slug("my-shop").city("Lomé").build();

        when(shopRepository.findById(otherShopId)).thenReturn(Optional.of(
                Shop.builder().id(otherShopId).owner(seller).name("Other").slug("other").city("Lomé").build()));
        when(shopRepository.findByOwnerId(seller.getId())).thenReturn(Optional.of(myShop));

        CreatePromoCodeRequest req = new CreatePromoCodeRequest(
                "CODE3", PromoScopeType.SHOP, otherShopId, new BigDecimal("10.00"), null, null, null);

        ApiException ex = assertThrows(ApiException.class, () -> promoCodeService.create(seller, req));
        assertEquals(403, ex.getStatus().value());
    }

    @Test
    void createRejectsExpirationInThePast() {
        User admin = User.builder().fullName("Admin").phoneNumber("+22890000000").passwordHash("h")
                .role(Role.ADMIN).build();
        CreatePromoCodeRequest req = new CreatePromoCodeRequest(
                "CODE4", PromoScopeType.PLATFORM, null, new BigDecimal("10.00"), null, null,
                Instant.now().minus(1, ChronoUnit.DAYS));

        ApiException ex = assertThrows(ApiException.class, () -> promoCodeService.create(admin, req));
        assertEquals(400, ex.getStatus().value());
    }
}
