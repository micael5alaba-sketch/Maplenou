-- Prix barre / reduction sur un produit. NULL = pas de promo affichee.
-- Regle d'affichage geree cote client : si old_price est absent, null, ou <= base_price,
-- aucune promo n'est affichee.
ALTER TABLE products ADD COLUMN old_price NUMERIC(12, 2);
