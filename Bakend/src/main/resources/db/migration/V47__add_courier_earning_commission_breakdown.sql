-- V47 : barème de commission plateforme/livreur formalisé (25% plateforme,
-- 75% livreur, voir CourierEarningService) — jusqu'ici le livreur touchait
-- 100% du tarif de zone (montant "provisoire", voir V39). On ajoute le détail
-- brut/commission/net pour la traçabilité, sur le même modèle que la
-- commission vendeur (sub_orders.commission_rate/commission_amount/net_amount,
-- voir V13).
--
-- Les lignes déjà existantes datent d'avant ce barème : le livreur a
-- réellement touché 100% à l'époque, donc on rétro-remplit gross_amount =
-- amount et commission_amount = 0 plutôt que de réappliquer le nouveau taux
-- rétroactivement.

ALTER TABLE courier_earnings
    ADD COLUMN gross_amount     NUMERIC(12,2),
    ADD COLUMN commission_rate  NUMERIC(5,4),
    ADD COLUMN commission_amount NUMERIC(12,2);

UPDATE courier_earnings
SET gross_amount      = amount,
    commission_rate   = 0,
    commission_amount = 0
WHERE gross_amount IS NULL;

ALTER TABLE courier_earnings
    ALTER COLUMN gross_amount SET NOT NULL,
    ALTER COLUMN commission_rate SET NOT NULL,
    ALTER COLUMN commission_amount SET NOT NULL;
