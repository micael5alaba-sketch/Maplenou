-- V1 : Table des comptes utilisateurs
-- Un compte unique par personne (téléphone + mot de passe).
-- role est nullable : la grande majorité des comptes sont acheteurs par défaut.

CREATE TABLE users (
    id              UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    full_name       VARCHAR(150) NOT NULL,
    phone_number    VARCHAR(20)  NOT NULL,
    email           VARCHAR(255),
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(30),
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    is_phone_verified BOOLEAN    NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uk_users_phone UNIQUE (phone_number)
);

CREATE INDEX idx_users_phone ON users (phone_number);
