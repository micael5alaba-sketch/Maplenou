-- V7 : Variantes d'un produit (taille, couleur, etc.)
-- price_override nullable : si null, on utilise le base_price du produit parent.
-- La décrémentation du stock doit être ATOMIQUE (voir CLAUDE.md §5).

CREATE TABLE product_variants (
    id             UUID           NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    product_id     UUID           NOT NULL,
    label          VARCHAR(100)   NOT NULL,
    price_override NUMERIC(12, 2),
    stock_quantity INT            NOT NULL DEFAULT 0,
    sku            VARCHAR(100)   NOT NULL,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT uk_variants_sku       UNIQUE (sku),
    CONSTRAINT fk_variants_product   FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_variants_stock    CHECK (stock_quantity >= 0)
);

CREATE INDEX idx_variants_product ON product_variants (product_id);
