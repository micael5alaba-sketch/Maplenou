-- Programme Partenaire Revendeur (document "Programme Partenaire Revendeur", version 2.0, nov. 2026).
-- Montants en FCFA ENTIERS (BIGINT), jamais de decimales : tous les calculs sont faits cote serveur.

-- Parametres financiers VERSIONNES : une modification = une nouvelle ligne (l'ancienne est conservee).
-- Les commandes gardent la version appliquee a leur creation (reseller_commissions.settings_version).
CREATE TABLE reseller_settings (
    id                  UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    version             INT           NOT NULL UNIQUE,
    share_rate          NUMERIC(5,4)  NOT NULL,   -- part du partenaire dans la commission Maplenou (0.25)
    min_unit_price      BIGINT        NOT NULL,   -- prix unitaire final minimum (2500)
    tier_threshold      BIGINT        NOT NULL,   -- seuil des paliers de commission (10000, inclus dans le palier bas)
    rate_low            NUMERIC(5,4)  NOT NULL,   -- commission Maplenou de 2500 au seuil inclus (0.05)
    rate_high           NUMERIC(5,4)  NOT NULL,   -- commission Maplenou au-dessus du seuil (0.03)
    attribution_days    INT           NOT NULL,   -- duree d'attribution apres le dernier clic (30)
    min_withdrawal      BIGINT        NOT NULL,   -- retrait minimum (3000)
    withdrawal_fee_rate NUMERIC(5,4)  NOT NULL,   -- frais de retrait (0.03)
    program_active      BOOLEAN       NOT NULL DEFAULT TRUE,
    change_reason       TEXT,
    created_by          UUID          REFERENCES users(id),
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
INSERT INTO reseller_settings (version, share_rate, min_unit_price, tier_threshold, rate_low, rate_high,
                               attribution_days, min_withdrawal, withdrawal_fee_rate, program_active, change_reason)
VALUES (1, 0.2500, 2500, 10000, 0.0500, 0.0300, 30, 3000, 0.0300, TRUE, 'Valeurs initiales du programme (v2.0)');

-- Profil partenaire (un seul par utilisateur). Statuts : PENDING, APPROVED, REJECTED, SUSPENDED, DEACTIVATED.
CREATE TABLE reseller_profiles (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID         NOT NULL UNIQUE REFERENCES users(id),
    full_name           VARCHAR(150) NOT NULL,
    phone_number        VARCHAR(20)  NOT NULL,
    email               VARCHAR(255) NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    terms_version       VARCHAR(20)  NOT NULL,
    terms_accepted_at   TIMESTAMPTZ  NOT NULL,
    decided_by          UUID         REFERENCES users(id),
    decided_at          TIMESTAMPTZ,
    status_reason       TEXT,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_reseller_profiles_status ON reseller_profiles(status, created_at);

-- Acceptation des conditions : date ET version conservees a chaque acceptation.
CREATE TABLE reseller_terms_acceptances (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL REFERENCES users(id),
    terms_version VARCHAR(20)  NOT NULL,
    accepted_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    ip_address    VARCHAR(64)
);

-- Eligibilite manuelle d'un produit (exclusion par l'administration). Absent = non exclu.
CREATE TABLE reseller_product_eligibility (
    product_id  UUID        PRIMARY KEY REFERENCES products(id),
    excluded    BOOLEAN     NOT NULL DEFAULT FALSE,
    updated_by  UUID        REFERENCES users(id),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Lien unique par (partenaire, produit).
CREATE TABLE reseller_referral_links (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    partner_id  UUID        NOT NULL REFERENCES reseller_profiles(id),
    product_id  UUID        NOT NULL REFERENCES products(id),
    code        VARCHAR(16) NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (partner_id, product_id)
);

-- Historique des clics. valid = peut servir a l'attribution ; counted = compte dans les statistiques
-- (un meme visiteur qui reclique en moins de 10 minutes n'est compte qu'une fois).
CREATE TABLE reseller_clicks (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    link_id       UUID        NOT NULL REFERENCES reseller_referral_links(id),
    partner_id    UUID        NOT NULL REFERENCES reseller_profiles(id),
    product_id    UUID        NOT NULL REFERENCES products(id),
    visitor_hash  VARCHAR(64) NOT NULL,
    valid         BOOLEAN     NOT NULL,
    counted       BOOLEAN     NOT NULL,
    clicked_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at    TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_reseller_clicks_partner ON reseller_clicks(partner_id, clicked_at);
CREATE INDEX idx_reseller_clicks_visitor ON reseller_clicks(link_id, visitor_hash, clicked_at);

-- Attribution liee au compte acheteur connecte (dernier clic valide par produit) : permet de
-- retrouver l'attribution d'un autre appareil. Un nouveau clic REMPLACE la precedente.
CREATE TABLE reseller_buyer_attributions (
    user_id     UUID        NOT NULL REFERENCES users(id),
    product_id  UUID        NOT NULL REFERENCES products(id),
    click_id    UUID        NOT NULL REFERENCES reseller_clicks(id),
    partner_id  UUID        NOT NULL REFERENCES reseller_profiles(id),
    clicked_at  TIMESTAMPTZ NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, product_id)
);

-- Attribution GELEE a la creation de la commande, une par ligne de commande : aucun clic ulterieur
-- ne peut la modifier.
CREATE TABLE reseller_attributions (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    order_item_id  UUID        NOT NULL UNIQUE REFERENCES sub_order_items(id),
    partner_id     UUID        NOT NULL REFERENCES reseller_profiles(id),
    product_id     UUID        NOT NULL REFERENCES products(id),
    click_id       UUID        NOT NULL REFERENCES reseller_clicks(id),
    buyer_id       UUID        NOT NULL REFERENCES users(id),
    clicked_at     TIMESTAMPTZ NOT NULL,
    expires_at     TIMESTAMPTZ NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_reseller_attributions_partner ON reseller_attributions(partner_id, created_at);

-- Commission calculee PAR UNITE a la creation de la commande ; creditee seulement a la livraison.
CREATE TABLE reseller_commissions (
    id                  UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    order_item_id       UUID          NOT NULL REFERENCES sub_order_items(id),
    unit_index          SMALLINT      NOT NULL,
    attribution_id      UUID          NOT NULL REFERENCES reseller_attributions(id),
    partner_id          UUID          NOT NULL REFERENCES reseller_profiles(id),
    product_name        VARCHAR(255)  NOT NULL,
    unit_price          BIGINT        NOT NULL,       -- prix unitaire final payé (apres reduction)
    commission_rate     NUMERIC(5,4)  NOT NULL,
    maplenou_commission BIGINT        NOT NULL,
    partner_share       BIGINT        NOT NULL,
    maplenou_share      BIGINT        NOT NULL,
    settings_version    INT           NOT NULL,
    status              VARCHAR(20)   NOT NULL DEFAULT 'PENDING',  -- PENDING, CREDITED, CANCELLED
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    credited_at         TIMESTAMPTZ,
    -- une commission par unite de ligne : empeche toute creation en double
    UNIQUE (order_item_id, unit_index)
);
CREATE INDEX idx_reseller_commissions_partner ON reseller_commissions(partner_id, status, created_at);

-- Registre financier : IMMUABLE (trigger plus bas). Le solde se calcule uniquement a partir de ces
-- lignes : available_delta / reserved_delta (jamais de solde modifiable directement).
CREATE TABLE reseller_wallet_transactions (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    partner_id       UUID         NOT NULL REFERENCES reseller_profiles(id),
    type             VARCHAR(30)  NOT NULL,   -- COMMISSION_CREDIT, WITHDRAWAL_RESERVE, WITHDRAWAL_RELEASE, WITHDRAWAL_PAID, ADJUSTMENT
    amount           BIGINT       NOT NULL,   -- montant de l'operation (positif, sauf ADJUSTMENT signe)
    available_delta  BIGINT       NOT NULL,
    reserved_delta   BIGINT       NOT NULL,
    fee_amount       BIGINT       NOT NULL DEFAULT 0,   -- frais de retrait (WITHDRAWAL_PAID)
    reference_id     UUID,                              -- commission ou retrait concerne
    reference        VARCHAR(100),                      -- reference de transaction de paiement
    reason           TEXT,
    actor_id         UUID         REFERENCES users(id),
    idempotency_key  VARCHAR(120) NOT NULL UNIQUE,      -- un meme evenement ne s'enregistre qu'une fois
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_reseller_wallet_tx_partner ON reseller_wallet_transactions(partner_id, created_at);

CREATE FUNCTION reseller_wallet_transactions_immutable() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'Le registre financier des partenaires est immuable : corriger par une transaction de regularisation';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_reseller_wallet_transactions_immutable
    BEFORE UPDATE OR DELETE ON reseller_wallet_transactions
    FOR EACH ROW EXECUTE FUNCTION reseller_wallet_transactions_immutable();

-- Retraits (meme circuit de verification/approbation que les autres retraits).
CREATE TABLE reseller_withdrawals (
    id                 UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    partner_id         UUID         NOT NULL REFERENCES reseller_profiles(id),
    amount             BIGINT       NOT NULL,
    fee_amount         BIGINT       NOT NULL DEFAULT 0,
    net_amount         BIGINT       NOT NULL DEFAULT 0,
    method             VARCHAR(20)  NOT NULL,   -- MIX_BY_YAS, MOOV_AFRICA
    payment_phone      VARCHAR(20)  NOT NULL,
    status             VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    payment_reference  VARCHAR(100),
    approved_by        UUID         REFERENCES users(id),
    approved_at        TIMESTAMPTZ,
    paid_at            TIMESTAMPTZ,
    notes              TEXT,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ
);
CREATE INDEX idx_reseller_withdrawals_partner ON reseller_withdrawals(partner_id, created_at DESC);
CREATE INDEX idx_reseller_withdrawals_status  ON reseller_withdrawals(status);

-- Journal des actions administratives du programme (ancienne et nouvelle valeur conservees).
CREATE TABLE reseller_audit_logs (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id     UUID         REFERENCES users(id),
    action       VARCHAR(50)  NOT NULL,
    entity_type  VARCHAR(40)  NOT NULL,
    entity_id    UUID,
    old_value    TEXT,
    new_value    TEXT,
    reason       TEXT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_reseller_audit_logs_created ON reseller_audit_logs(created_at DESC);
