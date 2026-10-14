-- V48 : frais mensuel d'activité vendeur (catégorie tarifaire + 3 ventes gratuites + déduction
-- avant reversement) — voir SellerFeeService. Nouveau système, additionnel à la commission par
-- vente déjà en place (sub_orders.commission_rate/commission_amount, voir V13).

ALTER TABLE seller_profiles
    ADD COLUMN fee_category VARCHAR(20) NOT NULL DEFAULT 'STANDARD';

CREATE TABLE seller_monthly_fees (
    id           UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    shop_id      UUID          NOT NULL,
    billing_month DATE         NOT NULL,
    category     VARCHAR(20)   NOT NULL,
    amount       NUMERIC(12,2) NOT NULL,
    amount_paid  NUMERIC(12,2) NOT NULL DEFAULT 0,
    status       VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_seller_monthly_fees_shop FOREIGN KEY (shop_id) REFERENCES shops(id),
    CONSTRAINT uq_seller_monthly_fees_shop_month UNIQUE (shop_id, billing_month),
    CONSTRAINT chk_seller_monthly_fees_status CHECK (status IN ('PENDING', 'PARTIALLY_PAID', 'PAID', 'CANCELLED')),
    CONSTRAINT chk_seller_monthly_fees_category CHECK (category IN ('STANDARD', 'HIGH_PRICE'))
);

CREATE INDEX idx_seller_monthly_fees_shop_id ON seller_monthly_fees(shop_id);
CREATE INDEX idx_seller_monthly_fees_status ON seller_monthly_fees(status);

CREATE TABLE seller_fee_deductions (
    id             UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    monthly_fee_id UUID          NOT NULL,
    payout_id      UUID,
    amount         NUMERIC(12,2) NOT NULL,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_seller_fee_deductions_fee FOREIGN KEY (monthly_fee_id) REFERENCES seller_monthly_fees(id),
    CONSTRAINT fk_seller_fee_deductions_payout FOREIGN KEY (payout_id) REFERENCES payouts(id)
);

CREATE INDEX idx_seller_fee_deductions_fee_id ON seller_fee_deductions(monthly_fee_id);

CREATE TABLE seller_fee_category_change_requests (
    id                  UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    seller_profile_id   UUID        NOT NULL,
    requested_category  VARCHAR(20) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    effective_from      DATE,
    applied             BOOLEAN     NOT NULL DEFAULT FALSE,
    decided_by          UUID,
    decided_at          TIMESTAMPTZ,
    admin_notes         TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_seller_fee_cat_change_profile FOREIGN KEY (seller_profile_id) REFERENCES seller_profiles(id),
    CONSTRAINT fk_seller_fee_cat_change_admin FOREIGN KEY (decided_by) REFERENCES users(id),
    CONSTRAINT chk_seller_fee_cat_change_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT chk_seller_fee_cat_change_category CHECK (requested_category IN ('STANDARD', 'HIGH_PRICE'))
);

CREATE INDEX idx_seller_fee_cat_change_profile_id ON seller_fee_category_change_requests(seller_profile_id);

ALTER TABLE payouts
    ADD COLUMN fees_deducted NUMERIC(14,2) NOT NULL DEFAULT 0;
