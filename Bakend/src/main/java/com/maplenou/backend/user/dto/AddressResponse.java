package com.maplenou.backend.user.dto;

import com.maplenou.backend.user.Address;
import java.util.UUID;

public record AddressResponse(
        UUID id,
        String label,
        String city,
        String district,
        String details,
        Double latitude,
        Double longitude,
        boolean isDefault
) {
    public static AddressResponse from(Address a) {
        return new AddressResponse(
                a.getId(),
                a.getLabel(),
                a.getCity(),
                a.getDistrict(),
                a.getDetails(),
                a.getLatitude(),
                a.getLongitude(),
                a.isDefault()
        );
    }
}
