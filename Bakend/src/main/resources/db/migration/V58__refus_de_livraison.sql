-- Affectation automatique des livraisons : un livreur peut refuser une course qui vient de lui etre
-- affectee ; on garde ses refus pour ne pas la lui reproposer, et passer au livreur suivant.

CREATE TABLE delivery_declines (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    delivery_id  UUID         NOT NULL REFERENCES deliveries(id) ON DELETE CASCADE,
    agent_id     UUID         NOT NULL REFERENCES users(id),
    declined_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (delivery_id, agent_id)
);
CREATE INDEX idx_delivery_declines_delivery ON delivery_declines(delivery_id);
