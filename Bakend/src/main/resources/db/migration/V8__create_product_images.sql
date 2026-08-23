-- V8 : Images d'un produit, ordonnées par position
-- Les URLs sont gérées par Cloudinary (upload côté client, on stocke l'URL résultante).

CREATE TABLE product_images (
    id         UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    product_id UUID        NOT NULL,
    url        VARCHAR(500) NOT NULL,
    position   SMALLINT    NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_images_product FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE INDEX idx_images_product_pos ON product_images (product_id, position);
