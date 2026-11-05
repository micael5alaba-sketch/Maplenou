-- Derniere position connue de chaque livreur, hors livraison en cours : le tracking GPS en
-- direct (Firebase RTDB) n'existe que pendant une course, or l'admin doit voir ou sont les
-- livreurs LIBRES pour leur affecter une livraison (rapport de modification du 28/10/2026).
-- Une seule ligne par livreur, ecrasee a chaque ping de l'app.

CREATE TABLE courier_locations (
    agent_id    UUID             PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    latitude    DOUBLE PRECISION NOT NULL,
    longitude   DOUBLE PRECISION NOT NULL,
    updated_at  TIMESTAMPTZ      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_courier_locations_updated_at ON courier_locations(updated_at DESC);
