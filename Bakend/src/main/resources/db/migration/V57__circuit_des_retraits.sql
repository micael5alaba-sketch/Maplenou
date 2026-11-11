-- Circuit obligatoire des retraits (document "Roles et acces au tableau de bord", section 8) :
-- reception -> verification -> soumission (Agent support et retraits) -> decision (Super Admin,
-- MFA redemandee) -> execution et trace. payouts.status est deja un VARCHAR(20) sans contrainte :
-- les nouveaux statuts (PREPARED, SUBMITTED, APPROVED, REJECTED, NEEDS_CORRECTION) n'exigent
-- aucune modification de schema.

-- Compte approbateur et horodatage de l'approbation (un retrait n'est payable qu'une fois approuve).
ALTER TABLE payouts ADD COLUMN approved_by UUID REFERENCES users(id);
ALTER TABLE payouts ADD COLUMN approved_at TIMESTAMPTZ;

-- Retraits des livreurs : meme circuit que les retraits vendeurs. Un retrait regroupe les gains
-- (courier_earnings) non encore verses du livreur au moment de la demande.
CREATE TABLE courier_withdrawals (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_id         UUID          NOT NULL REFERENCES users(id),
    amount           NUMERIC(14,2) NOT NULL,
    status           VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    payment_details  VARCHAR(100)  NOT NULL,
    approved_by      UUID          REFERENCES users(id),
    approved_at      TIMESTAMPTZ,
    paid_at          TIMESTAMPTZ,
    notes            TEXT,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ
);
CREATE INDEX idx_courier_withdrawals_agent  ON courier_withdrawals(agent_id, created_at DESC);
CREATE INDEX idx_courier_withdrawals_status ON courier_withdrawals(status);

ALTER TABLE courier_earnings ADD COLUMN withdrawal_id UUID REFERENCES courier_withdrawals(id);
CREATE INDEX idx_courier_earnings_withdrawal ON courier_earnings(withdrawal_id);

-- Historique de chaque transition de statut d'un retrait (qui, quand, pourquoi).
CREATE TABLE withdrawal_status_history (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    subject_type  VARCHAR(30)  NOT NULL,
    subject_id    UUID         NOT NULL,
    from_status   VARCHAR(20),
    to_status     VARCHAR(20)  NOT NULL,
    actor_id      UUID         REFERENCES users(id),
    note          TEXT,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_withdrawal_history_subject ON withdrawal_status_history(subject_type, subject_id, created_at);
