CREATE TABLE delivery_zones (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    -- ex: "Grand Lomé", "Baguida", "Avépozo", "Kpogan"
    name                    VARCHAR(150)    NOT NULL UNIQUE,
    delivery_fee            NUMERIC(12, 2)  NOT NULL CHECK (delivery_fee >= 0),
    -- Délai estimé en minutes (ex: 30, 60, 120)
    estimated_time_minutes  INT             NOT NULL DEFAULT 60 CHECK (estimated_time_minutes >= 1),
    is_active               BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_delivery_zones_active ON delivery_zones(is_active);
