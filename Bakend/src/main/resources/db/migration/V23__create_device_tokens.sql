-- ── Tokens FCM des appareils utilisateurs ────────────────────────────────────
-- Chaque utilisateur peut avoir plusieurs appareils (mobile Android, iOS, web).
CREATE TABLE device_tokens (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       TEXT        NOT NULL,
    platform    VARCHAR(10) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_device_platform CHECK (platform IN ('ANDROID', 'IOS', 'WEB')),
    -- Un token FCM ne peut appartenir qu'à un seul utilisateur à la fois
    CONSTRAINT uq_device_token UNIQUE (token)
);

CREATE INDEX idx_device_tokens_user_id ON device_tokens(user_id);
