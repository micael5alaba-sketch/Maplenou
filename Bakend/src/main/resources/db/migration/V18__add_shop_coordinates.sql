-- Coordonnées GPS de la boutique pour la navigation du livreur (pickup point).
-- Remplies par le vendeur via son profil boutique.
ALTER TABLE shops
    ADD COLUMN latitude  NUMERIC(10, 7),
    ADD COLUMN longitude NUMERIC(10, 7);
