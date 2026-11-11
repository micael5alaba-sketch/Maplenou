package com.maplenou.backend.staff.dto;

/**
 * Compte du personnel + mot de passe initial en clair, renvoyé UNE seule fois (création ou
 * réinitialisation). Le Super Admin le transmet par un canal sécurisé ; l'utilisateur doit le
 * changer à sa première connexion.
 */
public record StaffCredentialsResponse(
        StaffResponse staff,
        String temporaryPassword
) {
}
