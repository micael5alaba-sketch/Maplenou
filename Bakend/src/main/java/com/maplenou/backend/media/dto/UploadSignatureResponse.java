package com.maplenou.backend.media.dto;

public record UploadSignatureResponse(
        String cloudName,
        String apiKey,
        long timestamp,
        String signature,
        String folder
) {
}
