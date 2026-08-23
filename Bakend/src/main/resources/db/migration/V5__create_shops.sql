-- V5 : Boutiques des vendeurs
-- Une boutique = un storefront lié à un compte User ayant un SellerProfile APPROVED.
-- Le statut de la boutique est indépendant du statut du SellerProfile.

CREATE TABLE shops (
    id          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    owner_id    UUID         NOT NULL,
    name        VARCHAR(150) NOT NULL,
    slug        VARCHAR(180) NOT NULL,
    description TEXT,
    logo_url    VARCHAR(500),
    cover_url   VARCHAR(500),
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    city        VARCHAR(100) NOT NULL,
    district    VARCHAR(100),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uk_shops_slug     UNIQUE (slug),
    CONSTRAINT uk_shops_owner    UNIQUE (owner_id),
    CONSTRAINT fk_shops_owner    FOREIGN KEY (owner_id) REFERENCES users (id)
);

CREATE INDEX idx_shops_status ON shops (status);
CREATE INDEX idx_shops_slug   ON shops (slug);
