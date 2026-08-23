package com.maplenou.backend.user;

import com.maplenou.backend.admin.GdprService;
import com.maplenou.backend.user.dto.UpdateProfileRequest;
import com.maplenou.backend.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Utilisateurs")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;
    private final GdprService gdprService;

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal User currentUser) {
        return UserResponse.from(currentUser);
    }

    @PutMapping("/me")
    public UserResponse updateMe(@AuthenticationPrincipal User currentUser,
                                  @Valid @RequestBody UpdateProfileRequest request) {
        return UserResponse.from(userService.updateProfile(currentUser.getId(), request));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(@AuthenticationPrincipal User currentUser) {
        gdprService.selfDelete(currentUser);
        return ResponseEntity.noContent().build();
    }
}
