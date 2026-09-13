-- Numero d'identification fiscale de la boutique (NINEA au Senegal, TIN/TVA ailleurs).
-- Optionnel : toutes les boutiques n'en ont pas au moment de l'inscription.
ALTER TABLE shops ADD COLUMN tax_id VARCHAR(50);
