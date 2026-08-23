-- V11 : Produits mis en favoris par un utilisateur
-- Un utilisateur ne peut pas mettre le même produit en favori deux fois.

CREATE TABLE favorites (
    id         UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id    UUID        NOT NULL,
    product_id UUID        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uk_favorites_user_product UNIQUE (user_id, product_id),
    CONSTRAINT fk_favorites_user         FOREIGN KEY (user_id)    REFERENCES users    (id),
    CONSTRAINT fk_favorites_product      FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE INDEX idx_favorites_user ON favorites (user_id, created_at DESC);
