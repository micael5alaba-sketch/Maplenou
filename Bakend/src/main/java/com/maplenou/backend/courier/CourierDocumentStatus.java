package com.maplenou.backend.courier;

public enum CourierDocumentStatus {
    /** Envoyé par le livreur, pas encore examiné par l'admin. */
    PENDING,
    /** Validé par l'admin. */
    VALID,
    /** Refusé par l'admin (voir rejectionReason). */
    REJECTED,
    /** Date d'expiration dépassée (calculé, jamais écrit directement par l'admin). */
    EXPIRED
}
