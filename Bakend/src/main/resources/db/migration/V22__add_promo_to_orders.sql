-- Lien vers le code promo utilisé (nullable — commande sans code promo)
ALTER TABLE orders
    ADD COLUMN promo_code_id   UUID        REFERENCES promo_codes(id),
    ADD COLUMN discount_amount NUMERIC(12, 2) NOT NULL DEFAULT 0;
