-- ── Avis produits ────────────────────────────────────────────────────────────
CREATE TABLE product_reviews (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id      UUID        NOT NULL REFERENCES products(id),
    user_id         UUID        NOT NULL REFERENCES users(id),
    -- Sous-commande livrée qui justifie l'avis (achat vérifié)
    sub_order_id    UUID        NOT NULL REFERENCES sub_orders(id),
    rating          SMALLINT    NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment         TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    -- Un seul avis par acheteur par produit
    CONSTRAINT uq_product_review UNIQUE (product_id, user_id)
);

CREATE INDEX idx_product_reviews_product_id ON product_reviews(product_id);
CREATE INDEX idx_product_reviews_user_id    ON product_reviews(user_id);

-- ── Avis boutiques ────────────────────────────────────────────────────────────
CREATE TABLE shop_reviews (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    shop_id         UUID        NOT NULL REFERENCES shops(id),
    user_id         UUID        NOT NULL REFERENCES users(id),
    -- Sous-commande livrée qui justifie l'avis (achat vérifié)
    sub_order_id    UUID        NOT NULL REFERENCES sub_orders(id),
    rating          SMALLINT    NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment         TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    -- Un seul avis par acheteur par boutique
    CONSTRAINT uq_shop_review UNIQUE (shop_id, user_id)
);

CREATE INDEX idx_shop_reviews_shop_id  ON shop_reviews(shop_id);
CREATE INDEX idx_shop_reviews_user_id  ON shop_reviews(user_id);
