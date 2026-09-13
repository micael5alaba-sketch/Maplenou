-- Compte de destination des versements d'une boutique (T-Money, Flooz, virement bancaire).
-- Une boutique peut en enregistrer plusieurs ; un seul est marque par defaut a la fois.
CREATE TABLE payout_methods (
    id UUID PRIMARY KEY,
    shop_id UUID NOT NULL REFERENCES shops(id),
    type VARCHAR(20) NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_payout_methods_shop_id ON payout_methods(shop_id);
