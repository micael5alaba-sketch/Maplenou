package com.maplenou.backend.user;

import com.maplenou.backend.user.dto.AddressResponse;
import com.maplenou.backend.user.dto.CreateAddressRequest;
import com.maplenou.backend.user.dto.UpdateAddressRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/me/addresses")
@RequiredArgsConstructor
@Tag(name = "Adresses")
@SecurityRequirement(name = "bearerAuth")
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public List<AddressResponse> list(@AuthenticationPrincipal User currentUser) {
        return addressService.listMyAddresses(currentUser.getId())
                .stream().map(AddressResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<AddressResponse> create(@AuthenticationPrincipal User currentUser,
                                                   @Valid @RequestBody CreateAddressRequest request) {
        Address created = addressService.create(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(AddressResponse.from(created));
    }

    @PutMapping("/{id}")
    public AddressResponse update(@AuthenticationPrincipal User currentUser,
                                   @PathVariable UUID id,
                                   @Valid @RequestBody UpdateAddressRequest request) {
        return AddressResponse.from(addressService.update(currentUser, id, request));
    }

    @PatchMapping("/{id}/default")
    public ResponseEntity<Void> setDefault(@AuthenticationPrincipal User currentUser,
                                            @PathVariable UUID id) {
        addressService.setDefault(currentUser, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User currentUser,
                                        @PathVariable UUID id) {
        addressService.delete(currentUser, id);
        return ResponseEntity.noContent().build();
    }
}
