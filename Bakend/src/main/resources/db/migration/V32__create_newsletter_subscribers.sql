-- V32 : Abonnés newsletter (§3.5) — capture des emails, intégration avec l'outil d'emailing à faire côté marketing

CREATE TABLE newsletter_subscribers (
    id            UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    active        BOOLEAN     NOT NULL DEFAULT TRUE,
    subscribed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    unsubscribed_at TIMESTAMPTZ,

    CONSTRAINT uk_newsletter_subscribers_email UNIQUE (email)
);
