-- ── Codes promo ───────────────────────────────────────────────────────────────
CREATE TABLE promo_codes (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    code            VARCHAR(50)     NOT NULL,
    -- PLATFORM : valable sur toute la plateforme
    -- SHOP     : valable uniquement pour une boutique précise
    scope_type      VARCHAR(20)     NOT NULL,
    -- NULL si scope_type = PLATFORM, sinon l'ID de la boutique
    scope_id        UUID,
    -- Réduction en pourcentage (ex: 10.00 = 10 %)
    discount_percent NUMERIC(5, 2)  NOT NULL,
    -- Montant minimum de commande pour que le code soit applicable (facultatif)
    min_order_amount NUMERIC(12, 2),
    -- Nombre maximum d'utilisations totales (NULL = illimité)
    max_uses        INT,
    -- Nombre d'utilisations actuelles
    current_uses    INT             NOT NULL DEFAULT 0,
    -- Date d'expiration (NULL = pas d'expiration)
    expires_at      TIMESTAMPTZ,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_promo_scope_type    CHECK (scope_type IN ('PLATFORM', 'SHOP')),
    CONSTRAINT chk_promo_discount      CHECK (discount_percent > 0 AND discount_percent <= 100),
    CONSTRAINT chk_promo_max_uses      CHECK (max_uses IS NULL OR max_uses > 0),
    CONSTRAINT chk_promo_scope_id      CHECK (scope_type = 'PLATFORM' OR scope_id IS NOT NULL)
);

-- Index de recherche par code (insensible à la casse)
CREATE INDEX idx_promo_codes_code ON promo_codes(UPPER(code));

-- Unicité des codes PLATFORM : PostgreSQL traite NULL != NULL dans UNIQUE classique,
-- donc on utilise des index partiels pour garantir l'unicité correctement.
CREATE UNIQUE INDEX uq_promo_platform_code
    ON promo_codes(UPPER(code))
    WHERE scope_type = 'PLATFORM';

CREATE UNIQUE INDEX uq_promo_shop_code
    ON promo_codes(UPPER(code), scope_id)
    WHERE scope_type = 'SHOP';

-- ── Utilisations (historique) ─────────────────────────────────────────────────
CREATE TABLE promo_code_usages (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    promo_code_id   UUID        NOT NULL REFERENCES promo_codes(id),
    order_id        UUID        NOT NULL REFERENCES orders(id),
    user_id         UUID        NOT NULL REFERENCES users(id),
    discount_amount NUMERIC(12, 2) NOT NULL,
    used_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    -- Un code ne peut être utilisé qu'une seule fois par commande
    CONSTRAINT uq_promo_usage_order  UNIQUE (promo_code_id, order_id),
    -- Un utilisateur ne peut utiliser un code qu'une seule fois
    CONSTRAINT uq_promo_usage_user   UNIQUE (promo_code_id, user_id)
);

CREATE INDEX idx_promo_usages_promo_code_id ON promo_code_usages(promo_code_id);
CREATE INDEX idx_promo_usages_user_id       ON promo_code_usages(user_id);
