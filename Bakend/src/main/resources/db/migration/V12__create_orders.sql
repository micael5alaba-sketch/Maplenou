CREATE TABLE orders (
    id                          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    buyer_id                    UUID            NOT NULL REFERENCES users(id),
    -- Snapshot de l'adresse de livraison au moment de la commande (RGPD : pas de FK volatile)
    delivery_label              VARCHAR(100),
    delivery_city               VARCHAR(100)    NOT NULL,
    delivery_district           VARCHAR(100),
    delivery_details            TEXT,
    delivery_latitude           NUMERIC(10, 7),
    delivery_longitude          NUMERIC(10, 7),
    total_amount                NUMERIC(12, 2)  NOT NULL CHECK (total_amount >= 0),
    status                      VARCHAR(30)     NOT NULL DEFAULT 'CREATED',
    payment_reference           VARCHAR(255),
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_orders_status CHECK (
        status IN (
            'CREATED',
            'PAID',
            'CLOSED',
            'CANCELLED',
            'PAYMENT_FAILED',
            'PARTIALLY_REFUNDED',
            'REFUNDED'
        )
    )
);

CREATE INDEX idx_orders_buyer_id ON orders(buyer_id);
CREATE INDEX idx_orders_status   ON orders(status);
CREATE INDEX idx_orders_created  ON orders(created_at DESC);
