package com.maplenou.backend.common.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final Integer retryAfterSeconds;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
        this.retryAfterSeconds = null;
    }

    /** Ajoute l'en-tête Retry-After à la réponse — utilisé pour les 429 (verrouillage,
     * rate limiting) afin que le client sache exactement quand réessayer. */
    public ApiException(HttpStatus status, String message, int retryAfterSeconds) {
        super(message);
        this.status = status;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Integer getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
