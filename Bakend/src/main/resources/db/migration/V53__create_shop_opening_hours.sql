-- Horaires d'ouverture de boutique, signale manquant dans le rapport de modification du
-- 28/10/2026 : un acheteur visitant une boutique ne pouvait pas savoir si/quand elle est ouverte.

CREATE TABLE shop_opening_hours (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    shop_id       UUID         NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    day_of_week   SMALLINT     NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    opens_at      TIME,
    closes_at     TIME,
    closed        BOOLEAN      NOT NULL DEFAULT FALSE,
    UNIQUE (shop_id, day_of_week)
);

CREATE INDEX idx_shop_opening_hours_shop ON shop_opening_hours(shop_id);
