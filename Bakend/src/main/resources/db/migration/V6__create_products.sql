-- V6 : Produits liés à une boutique et une catégorie
-- base_price en FCFA, minimum 500 FCFA (vérifié au niveau applicatif).
-- is_deleted = true pour le soft delete : un produit déjà commandé ne disparaît jamais physiquement.

CREATE TABLE products (
    id          UUID           NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    shop_id     UUID           NOT NULL,
    category_id UUID           NOT NULL,
    name        VARCHAR(200)   NOT NULL,
    slug        VARCHAR(230)   NOT NULL,
    description TEXT,
    base_price  NUMERIC(12, 2) NOT NULL,
    status      VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    is_deleted  BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT uk_products_slug   UNIQUE (slug),
    CONSTRAINT fk_products_shop   FOREIGN KEY (shop_id)     REFERENCES shops      (id),
    CONSTRAINT fk_products_cat    FOREIGN KEY (category_id) REFERENCES categories (id)
);

-- Index composites pour les filtres fréquents du catalogue public
CREATE INDEX idx_products_shop_status   ON products (shop_id, status) WHERE is_deleted = false;
CREATE INDEX idx_products_cat_status    ON products (category_id, status) WHERE is_deleted = false;
CREATE INDEX idx_products_created_at    ON products (created_at DESC, id DESC) WHERE status = 'ACTIVE' AND is_deleted = false;

-- Recherche full-text PostgreSQL (tsvector) — pas de LIKE '%...%'
ALTER TABLE products ADD COLUMN search_vector TSVECTOR
    GENERATED ALWAYS AS (
        to_tsvector('french', coalesce(name, '') || ' ' || coalesce(description, ''))
    ) STORED;

CREATE INDEX idx_products_search ON products USING GIN (search_vector);
