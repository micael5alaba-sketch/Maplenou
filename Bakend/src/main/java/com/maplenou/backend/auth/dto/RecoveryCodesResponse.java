package com.maplenou.backend.auth.dto;

import java.util.List;

/** Codes de récupération MFA, affichés une seule fois à l'activation (ou à la régénération). */
public record RecoveryCodesResponse(List<String> recoveryCodes) {
}
