-- V10 : Lignes du panier, une ligne par variante de produit
-- Un même produit avec deux variantes différentes = deux lignes distinctes.
-- Prix non stocké : toujours calculé à partir de la variante au moment de la lecture.

CREATE TABLE cart_items (
    id                 UUID     NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    cart_id            UUID     NOT NULL,
    product_variant_id UUID     NOT NULL,
    quantity           SMALLINT NOT NULL DEFAULT 1,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uk_cart_items_variant  UNIQUE (cart_id, product_variant_id),
    CONSTRAINT fk_cart_items_cart     FOREIGN KEY (cart_id)            REFERENCES carts            (id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_variant  FOREIGN KEY (product_variant_id) REFERENCES product_variants (id),
    CONSTRAINT chk_cart_items_qty     CHECK (quantity >= 1)
);

CREATE INDEX idx_cart_items_cart ON cart_items (cart_id);
