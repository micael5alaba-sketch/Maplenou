package com.maplenou.backend.media;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.media.dto.UploadSignatureResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Upload signé côté client : le backend ne reçoit jamais les octets de l'image
 * (économie de bande passante — important pour les réseaux mobiles à faible débit).
 * Le client (Flutter/Next.js) envoie ensuite le fichier directement à Cloudinary
 * avec cette signature, puis stocke l'URL retournée (ex: dans AddImageRequest).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaService {

    private final Cloudinary cloudinary;

    @Value("${app.cloudinary.url}")
    private String cloudinaryUrl;

    public UploadSignatureResponse generateUploadSignature(String folder) {
        requireConfigured();

        long timestamp = System.currentTimeMillis() / 1000;
        Map<String, Object> paramsToSign = new LinkedHashMap<>();
        paramsToSign.put("timestamp", timestamp);
        if (folder != null && !folder.isBlank()) {
            paramsToSign.put("folder", folder);
        }

        String signature = cloudinary.apiSignRequest(paramsToSign, cloudinary.config.apiSecret);

        return new UploadSignatureResponse(
                cloudinary.config.cloudName,
                cloudinary.config.apiKey,
                timestamp,
                signature,
                folder
        );
    }

    public void deleteAsset(String publicId) {
        requireConfigured();
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (Exception e) {
            log.error("Erreur suppression Cloudinary pour {} : {}", publicId, e.getMessage());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Échec de la suppression du média");
        }
    }

    /** Best-effort : appelé lors de la suppression d'une image produit, ne doit jamais bloquer la transaction. */
    public void deleteAssetQuietly(String publicId) {
        if (publicId == null || publicId.isBlank()) return;
        try {
            deleteAsset(publicId);
        } catch (Exception e) {
            log.warn("Suppression Cloudinary ignorée pour {} : {}", publicId, e.getMessage());
        }
    }

    private void requireConfigured() {
        if (cloudinaryUrl.contains("dev-key") || cloudinaryUrl.contains("dev-cloud")) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Cloudinary n'est pas configuré sur cet environnement");
        }
    }
}
