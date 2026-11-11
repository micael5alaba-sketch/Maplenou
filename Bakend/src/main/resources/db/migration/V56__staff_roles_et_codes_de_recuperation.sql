-- Roles du personnel du back-office (Super Admin / Admin livraisons / Agent support et retraits /
-- Auditeur) : la colonne users.role est deja un VARCHAR(30) sans contrainte, les nouvelles valeurs
-- (DELIVERY_ADMIN, SUPPORT_AGENT, AUDITOR) n'exigent donc aucune modification de schema.

-- Premiere connexion d'un compte du personnel : mot de passe a changer obligatoirement.
ALTER TABLE users ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

-- Tarification de livraison attribuee ou non a l'Administrateur des livraisons.
ALTER TABLE users ADD COLUMN delivery_pricing_granted BOOLEAN NOT NULL DEFAULT FALSE;

-- Codes de recuperation MFA a usage unique (remis a l'activation de la MFA, stockes hashes).
CREATE TABLE mfa_recovery_codes (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code_hash   VARCHAR(64)  NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, code_hash)
);
CREATE INDEX idx_mfa_recovery_codes_user ON mfa_recovery_codes(user_id);
