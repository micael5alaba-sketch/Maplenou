package com.maplenou.backend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 2, max = 100) String fullName,
        @Email String email,
        // Optionnel : URL Cloudinary (flux d'upload signé existant). Chaîne vide = retirer l'avatar.
        @Size(max = 500) String avatarUrl
) {
}
