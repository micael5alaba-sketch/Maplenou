-- Commission integree au prix affiche (5 % Standard / 3 % Prix eleves) : on conserve, pour
-- chaque commande, la categorie du vendeur au moment de l'achat et le prix de base vendeur de
-- chaque ligne (tracabilite exigee : prix de base, prix affiche, categorie, taux, commission).
-- Colonnes NULL pour les commandes anterieures (ancien modele 25 % / 22,5 %, jamais reecrites).

ALTER TABLE sub_orders      ADD COLUMN fee_category    VARCHAR(20);
ALTER TABLE sub_order_items ADD COLUMN base_unit_price NUMERIC(12,2);
