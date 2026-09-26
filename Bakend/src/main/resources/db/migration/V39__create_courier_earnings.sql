-- V39 : Rémunération livreur — un gain par livraison terminée (DELIVERED).
-- Module absent jusqu'ici (seul le vendeur avait un mécanisme de reversement,
-- voir V27__create_payouts.sql) ; créé pour que l'espace Livreur soit
-- réellement fonctionnel, pas juste mocké côté app.

CREATE TABLE courier_earnings (
    id          UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    delivery_id UUID          NOT NULL UNIQUE,
    agent_id    UUID          NOT NULL,
    amount      NUMERIC(12,2) NOT NULL,   -- provisoire : frais de la DeliveryZone correspondante
    status      VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    paid_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_courier_earnings_delivery FOREIGN KEY (delivery_id) REFERENCES deliveries(id),
    CONSTRAINT fk_courier_earnings_agent FOREIGN KEY (agent_id) REFERENCES users(id),
    CONSTRAINT chk_courier_earnings_status CHECK (status IN ('PENDING', 'PAID'))
);

CREATE INDEX idx_courier_earnings_agent_id ON courier_earnings(agent_id);
CREATE INDEX idx_courier_earnings_status ON courier_earnings(status);
