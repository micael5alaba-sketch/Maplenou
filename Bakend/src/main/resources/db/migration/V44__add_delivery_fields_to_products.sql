-- Champs livraison par produit (DEMANDES_MODIFICATIONS_BACKEND.md §14) : poids de colis,
-- délai estimé et disponibilité de livraison, distincts des zones de livraison géographiques
-- (DeliveryZone) qui restent la source de vérité pour les frais/délais par défaut.
ALTER TABLE products ADD COLUMN package_weight_kg NUMERIC(8, 3);
ALTER TABLE products ADD COLUMN estimated_delivery_delay VARCHAR(100);
ALTER TABLE products ADD COLUMN delivery_available BOOLEAN NOT NULL DEFAULT TRUE;
