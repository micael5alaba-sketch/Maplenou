-- Historique persistant des notifications, signale manquant par l'audit externe du 16/10/2026 :
-- seul le jeton FCM etait enregistre jusqu'ici, pas la notification elle-meme (rien apres
-- reinstallation, rien sur un second appareil, rien si les push sont refuses).

CREATE TABLE notifications (
    id            UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id  UUID            NOT NULL REFERENCES users(id),
    title         VARCHAR(150)    NOT NULL,
    body          TEXT            NOT NULL,
    type          VARCHAR(40)     NOT NULL,
    reference_id  VARCHAR(100),
    read_at       TIMESTAMPTZ,
    created_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- Liste "mes notifications" (newest first) et compteur nombre-non-lues : les deux requetes
-- reelles de l'ecran notifications et du badge de l'app.
CREATE INDEX idx_notifications_recipient_created ON notifications(recipient_id, created_at DESC);
CREATE INDEX idx_notifications_recipient_unread ON notifications(recipient_id) WHERE read_at IS NULL;
