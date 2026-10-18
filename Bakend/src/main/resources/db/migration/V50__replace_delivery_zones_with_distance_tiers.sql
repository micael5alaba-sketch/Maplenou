-- Le modele "zones" (noms de ville/quartier devant correspondre exactement a l'adresse,
-- geres manuellement par l'admin) est abandonne sur decision explicite du porteur de projet :
-- l'admin ne doit plus avoir a creer/maintenir des zones geographiques. Remplace par un
-- bareme de paliers par distance reelle (km) -- l'admin fixe juste "jusqu'a X km -> prix Y",
-- le calcul de distance entre boutique et acheteur (coordonnees GPS des deux cotes)
-- determine automatiquement quel palier s'applique, sans aucune notion de zone nommee.

CREATE TABLE delivery_pricing_tiers (
    id               UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    -- NULL = dernier palier, "au-dela de X km" (pas de limite haute).
    max_distance_km  NUMERIC(6, 2)   CHECK (max_distance_km IS NULL OR max_distance_km > 0),
    price            NUMERIC(12, 2)  NOT NULL CHECK (price >= 0),
    created_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- Au plus un palier "illimite" (max_distance_km IS NULL) : la derniere tranche du bareme.
CREATE UNIQUE INDEX idx_delivery_pricing_tiers_unlimited
    ON delivery_pricing_tiers ((max_distance_km IS NULL)) WHERE max_distance_km IS NULL;

-- Palier de depart pour que le systeme ne soit jamais vide avant que l'admin ne configure
-- son propre bareme (sinon GET /api/delivery-pricing-tiers/quote n'aurait rien a appliquer).
INSERT INTO delivery_pricing_tiers (max_distance_km, price) VALUES (NULL, 1000.00);

-- L'ancienne table delivery_zones n'est plus utilisee par aucun code (le frais et la
-- distance sont figes directement sur sub_orders.delivery_fee / delivery_distance_km
-- depuis la V49) mais n'est pas supprimee ici -- suppression destructrice volontairement
-- laissee a une action manuelle/explicite plutot qu'automatisee dans une migration.
