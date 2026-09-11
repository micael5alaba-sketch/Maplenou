-- Specifications libres d'un produit (Marque, Matiere, Origine, Pointure...), sous forme de
-- liste ordonnee de paires {label, value}. Pas de table a part : chaque vendeur met ce qui est
-- pertinent pour son produit, le frontend affiche n'importe quelle paire telle quelle.
ALTER TABLE products ADD COLUMN specifications JSONB;
