-- V40 : Notation du livreur par l'acheteur, une fois la livraison DELIVERED.
-- Miroir de product_reviews/shop_reviews, mais la cible est le livreur
-- (agent_id) et la preuve d'achat est la Delivery elle-même (un avis par
-- livraison, pas par sous-commande, car SubOrder <-> Delivery est 1-1).

CREATE TABLE courier_reviews (
    id          UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    delivery_id UUID          NOT NULL UNIQUE,
    agent_id    UUID          NOT NULL,
    user_id     UUID          NOT NULL,
    rating      SMALLINT      NOT NULL,
    comment     TEXT,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_courier_reviews_delivery FOREIGN KEY (delivery_id) REFERENCES deliveries(id),
    CONSTRAINT fk_courier_reviews_agent FOREIGN KEY (agent_id) REFERENCES users(id),
    CONSTRAINT fk_courier_reviews_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_courier_reviews_rating CHECK (rating BETWEEN 1 AND 5)
);

CREATE INDEX idx_courier_reviews_agent_id ON courier_reviews(agent_id);
