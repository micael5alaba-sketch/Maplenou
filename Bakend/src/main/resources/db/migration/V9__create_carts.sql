-- V9 : Panier actif par utilisateur (un seul panier actif à la fois)
-- Création paresseuse : le panier est créé au premier ajout d'article.
-- Pas de réservation de stock ici — le stock ne bouge qu'à la confirmation du paiement.

CREATE TABLE carts (
    id         UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id    UUID        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uk_carts_user  UNIQUE (user_id),
    CONSTRAINT fk_carts_user  FOREIGN KEY (user_id) REFERENCES users (id)
);
