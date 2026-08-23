CREATE TABLE return_requests (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    -- Un seul retour par sous-commande
    sub_order_id        UUID        NOT NULL UNIQUE REFERENCES sub_orders(id),
    buyer_id            UUID        NOT NULL REFERENCES users(id),
    -- Snapshot de la date de livraison au moment de la demande
    delivery_date       TIMESTAMPTZ NOT NULL,
    -- Délai limite : delivery_date + 30 jours
    return_deadline     TIMESTAMPTZ NOT NULL,
    reason              TEXT        NOT NULL,
    -- État du produit retourné selon l'acheteur
    product_condition   VARCHAR(20) NOT NULL,
    -- Décision du vendeur ou de l'admin
    decision            VARCHAR(20),
    decided_at          TIMESTAMPTZ,
    -- Qui a décidé (vendeur ou admin)
    decided_by_id       UUID        REFERENCES users(id),
    admin_notes         TEXT,
    -- Statut du remboursement
    refund_status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_return_product_condition
        CHECK (product_condition IN ('NEW','GOOD','DAMAGED')),
    CONSTRAINT chk_return_decision
        CHECK (decision IS NULL OR decision IN ('APPROVED','REJECTED')),
    CONSTRAINT chk_return_refund_status
        CHECK (refund_status IN ('PENDING','PROCESSING','REFUNDED','DENIED'))
);

CREATE INDEX idx_return_requests_buyer_id    ON return_requests(buyer_id);
CREATE INDEX idx_return_requests_sub_order   ON return_requests(sub_order_id);
CREATE INDEX idx_return_requests_decision    ON return_requests(decision) WHERE decision IS NULL;
