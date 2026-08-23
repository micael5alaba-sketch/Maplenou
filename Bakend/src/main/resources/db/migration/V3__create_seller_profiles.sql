-- V3 : Profils vendeurs (optionnels, 1-1 avec users)
-- Ce n'est pas un rôle séparé : le même compte devient vendeur en activant ce profil.
-- statut APPROVED donne les droits ROLE_SELLER via User.getAuthorities().

CREATE TABLE seller_profiles (
    id          UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id     UUID        NOT NULL,
    shop_name   VARCHAR(150) NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uk_seller_profiles_user UNIQUE (user_id),
    CONSTRAINT fk_seller_profiles_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- Index composite pour les requêtes admin (filtrage par statut)
CREATE INDEX idx_seller_profiles_status ON seller_profiles (status);
