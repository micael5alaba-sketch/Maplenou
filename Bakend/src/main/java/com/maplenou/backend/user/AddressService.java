package com.maplenou.backend.user;

import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.user.dto.CreateAddressRequest;
import com.maplenou.backend.user.dto.UpdateAddressRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;

    @Transactional(readOnly = true)
    public List<Address> listMyAddresses(UUID userId) {
        return addressRepository.findByUserId(userId);
    }

    @Transactional
    public Address create(User user, CreateAddressRequest req) {
        if (req.isDefault()) {
            clearDefault(user.getId());
        }
        Address address = Address.builder()
                .user(user)
                .label(req.label())
                .city(req.city())
                .district(req.district())
                .details(req.details())
                .latitude(req.latitude())
                .longitude(req.longitude())
                .isDefault(req.isDefault())
                .build();
        return addressRepository.save(address);
    }

    @Transactional
    public Address update(User user, UUID addressId, UpdateAddressRequest req) {
        Address address = getOwned(user.getId(), addressId);
        if (req.label() != null)    address.setLabel(req.label());
        if (req.city()  != null && !req.city().isBlank()) address.setCity(req.city());
        if (req.district() != null) address.setDistrict(req.district());
        if (req.details()  != null) address.setDetails(req.details());
        if (req.latitude()  != null) address.setLatitude(req.latitude());
        if (req.longitude() != null) address.setLongitude(req.longitude());
        if (Boolean.TRUE.equals(req.isDefault())) {
            clearDefault(user.getId());
            address.setDefault(true);
        }
        return addressRepository.save(address);
    }

    @Transactional
    public void setDefault(User user, UUID addressId) {
        Address address = getOwned(user.getId(), addressId);
        clearDefault(user.getId());
        address.setDefault(true);
        addressRepository.save(address);
    }

    @Transactional
    public void delete(User user, UUID addressId) {
        Address address = getOwned(user.getId(), addressId);
        addressRepository.delete(address);
    }

    // ── Privé ────────────────────────────────────────────────────────────────

    private Address getOwned(UUID userId, UUID addressId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Adresse introuvable"));
    }

    private void clearDefault(UUID userId) {
        addressRepository.findByUserId(userId).stream()
                .filter(Address::isDefault)
                .forEach(a -> {
                    a.setDefault(false);
                    addressRepository.save(a);
                });
    }
}
