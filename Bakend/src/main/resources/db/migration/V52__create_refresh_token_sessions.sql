-- Rotation stricte des refresh tokens (correctif F-02, pentest du 21/10/2026) : chaque jeton
-- emis est trace par son hash, jamais la valeur brute. Avant ce mecanisme, un refresh token
-- restait utilisable indefiniment meme apres avoir produit son successeur (pas de revocation a
-- la rotation, seulement a la deconnexion explicite).

CREATE TABLE refresh_token_session (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id       UUID            NOT NULL,
    user_id         UUID            NOT NULL REFERENCES users(id),
    token_hash      VARCHAR(128)    NOT NULL,
    status          VARCHAR(20)     NOT NULL,
    parent_id       UUID,
    ip_hash         VARCHAR(128),
    issued_at       TIMESTAMPTZ     NOT NULL,
    expires_at      TIMESTAMPTZ     NOT NULL,
    revoked_at      TIMESTAMPTZ,
    revoked_reason  VARCHAR(100)
);

-- Lookup principal : retrouver un jeton presente par son hash (chaque rotation, chaque logout).
CREATE UNIQUE INDEX idx_refresh_token_session_hash ON refresh_token_session(token_hash);
-- Revocation de toute une famille (rejeu detecte, deconnexion).
CREATE INDEX idx_refresh_token_session_family ON refresh_token_session(family_id);
-- Revocation de toutes les sessions actives d'un utilisateur (changement de mot de passe).
CREATE INDEX idx_refresh_token_session_user_status ON refresh_token_session(user_id, status);
