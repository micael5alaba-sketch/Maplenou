package com.maplenou.support.shared;

/**
 * Error raised by the domain and application layers. Framework-free: the web adapter turns the {@link Kind}
 * into an HTTP status and returns an RFC 9457 problem with the stable {@code code} (and the business rule).
 */
public class SupportException extends RuntimeException {

    /** What went wrong, independent of the transport. */
    public enum Kind {
        INVALID_INPUT,       // 400
        UNAUTHENTICATED,     // 401
        FORBIDDEN,           // 403
        NOT_FOUND,           // 404
        CONFLICT,            // 409
        BUSINESS_RULE,       // 422
        TOO_MANY_REQUESTS,   // 429
        UPSTREAM_FAILED,     // 502
        UNAVAILABLE          // 503
    }

    private final Kind kind;
    private final String code;
    private final String rule;

    public SupportException(Kind kind, String code, String message, String rule) {
        super(message);
        this.kind = kind;
        this.code = code;
        this.rule = rule;
    }

    public Kind kind() {
        return kind;
    }

    public String code() {
        return code;
    }

    public String rule() {
        return rule;
    }

    /** 404 — also used when the caller may not know the resource exists (AC-SB-017). */
    public static SupportException notFound(String what) {
        return new SupportException(Kind.NOT_FOUND, "NOT_FOUND", what + " introuvable", null);
    }

    /** 403 — authenticated but not authorized for this operation (BR-071, BR-076). */
    public static SupportException forbidden(String message, String rule) {
        return new SupportException(Kind.FORBIDDEN, "FORBIDDEN", message, rule);
    }

    /** 422 — request is well-formed but violates a business rule. */
    public static SupportException businessRule(String code, String message, String rule) {
        return new SupportException(Kind.BUSINESS_RULE, code, message, rule);
    }

    /** 409 — conflicting state (concurrent modification, duplicate pending request...). */
    public static SupportException conflict(String code, String message, String rule) {
        return new SupportException(Kind.CONFLICT, code, message, rule);
    }

    /** 400 — invalid input. */
    public static SupportException badRequest(String code, String message, String rule) {
        return new SupportException(Kind.INVALID_INPUT, code, message, rule);
    }

    /** 503 — a system this service depends on cannot be reached; the caller may retry later. */
    public static SupportException unavailable(String code, String message) {
        return new SupportException(Kind.UNAVAILABLE, code, message, null);
    }

    /** 502 — a system this service depends on answered with an error. */
    public static SupportException upstream(String code, String message) {
        return new SupportException(Kind.UPSTREAM_FAILED, code, message, null);
    }

    public static SupportException unauthenticated(String code, String message, String rule) {
        return new SupportException(Kind.UNAUTHENTICATED, code, message, rule);
    }

    public static SupportException tooManyRequests(String code, String message) {
        return new SupportException(Kind.TOO_MANY_REQUESTS, code, message, null);
    }
}
