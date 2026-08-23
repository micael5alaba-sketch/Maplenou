CREATE TABLE sub_order_items (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    sub_order_id            UUID            NOT NULL REFERENCES sub_orders(id),
    -- FK nullable : la variante peut être supprimée plus tard (soft-delete produit)
    -- Snapshot des données produit au moment de la commande
    product_variant_id      UUID            REFERENCES product_variants(id),
    snapshot_product_name   VARCHAR(255)    NOT NULL,
    snapshot_variant_label  VARCHAR(100)    NOT NULL,
    snapshot_sku            VARCHAR(100)    NOT NULL,
    unit_price              NUMERIC(12, 2)  NOT NULL CHECK (unit_price >= 0),
    quantity                SMALLINT        NOT NULL CHECK (quantity >= 1),
    line_total              NUMERIC(12, 2)  NOT NULL CHECK (line_total >= 0),
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sub_order_items_sub_order_id ON sub_order_items(sub_order_id);
CREATE INDEX idx_sub_order_items_variant_id   ON sub_order_items(product_variant_id);
