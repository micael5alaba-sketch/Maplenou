-- Messagerie livreur <-> client (BUYER_COURIER) et livreur <-> boutique (SELLER_COURIER) :
-- ces deux nouveaux types de conversation s'ancrent sur une livraison précise (une conversation
-- par livraison), pas sur un couple d'utilisateurs indépendant du contexte. Le livreur participant
-- se déduit de deliveries.agent_id via cette colonne (pas de courier_id dupliqué sur conversations).
ALTER TABLE conversations ADD COLUMN delivery_id UUID REFERENCES deliveries(id);
CREATE INDEX idx_conversations_delivery_id ON conversations(delivery_id);

-- buyer_id n'a plus de sens pour SELLER_COURIER (aucun acheteur impliqué) : seller_id + shop_id +
-- delivery_id suffisent à identifier les deux participants (le vendeur, et le livreur via
-- deliveries.agent_id).
ALTER TABLE conversations ALTER COLUMN buyer_id DROP NOT NULL;
