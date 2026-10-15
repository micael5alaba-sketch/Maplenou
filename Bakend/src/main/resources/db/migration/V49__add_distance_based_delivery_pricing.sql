-- Tarification livraison dynamique (zone + distance, voir decision porteur de projet) :
-- le tarif fixe par zone devient un tarif de base, auquel s'ajoute un prix au km calcule
-- a partir des coordonnees reelles boutique <-> adresse de livraison.

ALTER TABLE delivery_zones
    ADD COLUMN price_per_km NUMERIC(10, 2) NOT NULL DEFAULT 0 CHECK (price_per_km >= 0);

-- Jusqu'ici l'Order ne memorisait aucun frais de livraison du tout (jamais facture a
-- l'acheteur) et le gain livreur etait deduit apres coup par correspondance approximative
-- de ville/quartier. Desormais calcule et fige au moment de la commande, par sous-commande
-- (un panier peut regrouper plusieurs boutiques, donc plusieurs distances differentes).
ALTER TABLE sub_orders
    ADD COLUMN delivery_fee NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (delivery_fee >= 0),
    ADD COLUMN delivery_distance_km NUMERIC(8, 2);
