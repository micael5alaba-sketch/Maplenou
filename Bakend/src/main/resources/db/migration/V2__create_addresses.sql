-- V2 : Adresses de livraison rattachées à un utilisateur

CREATE TABLE addresses (
    id          UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id     UUID        NOT NULL,
    label       VARCHAR(100),
    city        VARCHAR(100) NOT NULL,
    district    VARCHAR(100),
    details     TEXT,
    latitude    DOUBLE PRECISION,
    longitude   DOUBLE PRECISION,
    is_default  BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- Jamais de ON DELETE CASCADE : suppression de compte = anonymisation, pas suppression physique (RGPD).
CREATE INDEX idx_addresses_user ON addresses (user_id);
