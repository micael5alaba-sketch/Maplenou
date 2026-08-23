CREATE TABLE sub_orders (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id            UUID            NOT NULL REFERENCES orders(id),
    shop_id             UUID            NOT NULL REFERENCES shops(id),
    subtotal            NUMERIC(12, 2)  NOT NULL CHECK (subtotal >= 0),
    commission_rate     NUMERIC(5, 4)   NOT NULL,
    commission_amount   NUMERIC(12, 2)  NOT NULL CHECK (commission_amount >= 0),
    net_amount          NUMERIC(12, 2)  NOT NULL CHECK (net_amount >= 0),
    status              VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_sub_orders_status CHECK (
        status IN (
            'PENDING',
            'PREPARING',
            'READY_FOR_PICKUP',
            'IN_DELIVERY',
            'DELIVERED',
            'CANCELLED',
            'RETURN_REQUESTED',
            'RETURNED'
        )
    )
);

CREATE INDEX idx_sub_orders_order_id ON sub_orders(order_id);
CREATE INDEX idx_sub_orders_shop_id  ON sub_orders(shop_id);
CREATE INDEX idx_sub_orders_status   ON sub_orders(status);
