-- Tickets de support structures (livreur + acheteur), avec un vrai numero de suivi genere par
-- une sequence Postgres - remplace l'ancien systeme qui ne faisait qu'envoyer un message de
-- messagerie avec un numero invente cote app (jamais retrouvable).
CREATE SEQUENCE support_ticket_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE support_tickets (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    ticket_number       VARCHAR(30)  NOT NULL,
    requester_id        UUID         NOT NULL,
    category            VARCHAR(100) NOT NULL,
    subject             VARCHAR(200) NOT NULL,
    description         TEXT         NOT NULL,
    related_delivery_id UUID,
    photo_url           VARCHAR(500),
    status              VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    conversation_id     UUID,
    resolution_notes    TEXT,
    resolved_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uk_support_tickets_number UNIQUE (ticket_number),
    CONSTRAINT fk_support_tickets_requester FOREIGN KEY (requester_id) REFERENCES users (id),
    CONSTRAINT fk_support_tickets_delivery FOREIGN KEY (related_delivery_id) REFERENCES deliveries (id),
    CONSTRAINT fk_support_tickets_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id)
);

CREATE INDEX idx_support_tickets_requester ON support_tickets (requester_id, created_at DESC);
CREATE INDEX idx_support_tickets_status ON support_tickets (status, created_at ASC);
