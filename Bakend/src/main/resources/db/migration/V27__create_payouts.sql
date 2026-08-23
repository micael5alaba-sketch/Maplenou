-- V27 : Reversements vendeurs (payouts hebdomadaires)

CREATE TABLE payouts (
    id               UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    shop_id          UUID          NOT NULL,
    period_start     TIMESTAMPTZ   NOT NULL,
    period_end       TIMESTAMPTZ   NOT NULL,
    amount           NUMERIC(14,2) NOT NULL,   -- net vendeur (= somme netAmount des SubOrders)
    commission_total NUMERIC(14,2) NOT NULL,   -- commission Maplenou (= somme commissionAmount)
    status           VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    paid_at          TIMESTAMPTZ,
    notes            TEXT,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_payouts_shop FOREIGN KEY (shop_id) REFERENCES shops(id)
);

CREATE INDEX idx_payouts_shop_id ON payouts(shop_id);
CREATE INDEX idx_payouts_status  ON payouts(status);
CREATE INDEX idx_payouts_period  ON payouts(period_start, period_end);

-- Lier chaque SubOrder à son Payout (nullable : les SubOrders non encore payés ont NULL)
ALTER TABLE sub_orders ADD COLUMN payout_id UUID;
ALTER TABLE sub_orders ADD CONSTRAINT fk_sub_orders_payout
    FOREIGN KEY (payout_id) REFERENCES payouts(id);
CREATE INDEX idx_sub_orders_payout_id ON sub_orders(payout_id);
