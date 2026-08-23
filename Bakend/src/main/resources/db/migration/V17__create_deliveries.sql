CREATE TABLE deliveries (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    sub_order_id    UUID        NOT NULL UNIQUE REFERENCES sub_orders(id),
    agent_id        UUID        REFERENCES users(id),
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    -- Type de preuve : PHOTO ou SIGNATURE
    proof_type      VARCHAR(20),
    -- URL Cloudinary de la photo ou signature
    proof_url       TEXT,
    notes           TEXT,
    assigned_at     TIMESTAMPTZ,
    delivered_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_deliveries_status CHECK (
        status IN ('PENDING','ASSIGNED','IN_TRANSIT','DELIVERED','FAILED')
    ),
    CONSTRAINT chk_deliveries_proof_type CHECK (
        proof_type IS NULL OR proof_type IN ('PHOTO','SIGNATURE')
    )
);

CREATE INDEX idx_deliveries_agent_id  ON deliveries(agent_id);
CREATE INDEX idx_deliveries_status    ON deliveries(status);
