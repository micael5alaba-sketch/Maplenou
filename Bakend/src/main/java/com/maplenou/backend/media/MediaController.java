package com.maplenou.backend.media;

import com.maplenou.backend.media.dto.UploadSignatureRequest;
import com.maplenou.backend.media.dto.UploadSignatureResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Médias")
@RequestMapping("/api/media")
@SecurityRequirement(name = "bearerAuth")
public class MediaController {

    private final MediaService mediaService;

    /** Tout utilisateur authentifié peut demander une signature (upload avatar, logo boutique, produit, preuve de livraison…). */
    @PostMapping("/upload-signature")
    public UploadSignatureResponse getUploadSignature(@Valid @RequestBody(required = false) UploadSignatureRequest request) {
        String folder = request != null ? request.folder() : null;
        return mediaService.generateUploadSignature(folder);
    }

    /**
     * Modération : retrait de contenu non conforme (§3.4).
     * publicId passé en query param (peut contenir des "/" pour les sous-dossiers Cloudinary).
     */
    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestParam String publicId) {
        mediaService.deleteAsset(publicId);
    }
}
