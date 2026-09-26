-- Référence commande courte et lisible (ex: "CMD-000482"), affichée au client et au vendeur
-- à la place de l'UUID brut. Générée via une séquence dédiée pour rester strictement croissante
-- et lisible, indépendamment de l'UUID technique de la ligne.
CREATE SEQUENCE order_number_seq START WITH 1 INCREMENT BY 1;

ALTER TABLE orders ADD COLUMN order_number VARCHAR(20);

-- Backfill des commandes existantes, dans l'ordre chronologique de création.
WITH numbered AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY created_at ASC) AS rn
    FROM orders
)
UPDATE orders o
SET order_number = 'CMD-' || LPAD(numbered.rn::text, 6, '0')
FROM numbered
WHERE o.id = numbered.id;

-- Aligne la séquence sur le dernier numéro attribué, pour que les prochaines commandes continuent
-- la numérotation sans collision avec le backfill. GREATEST(1, ...) car setval refuse 0 (borne
-- basse de la séquence) ; is_called=false sur une base vide pour que le tout premier nextval()
-- renvoie bien 1 plutôt que de le sauter.
SELECT setval('order_number_seq',
    GREATEST(1, (SELECT COUNT(*) FROM orders)),
    (SELECT COUNT(*) FROM orders) > 0
);

ALTER TABLE orders ALTER COLUMN order_number SET NOT NULL;
ALTER TABLE orders ADD CONSTRAINT uk_orders_order_number UNIQUE (order_number);
