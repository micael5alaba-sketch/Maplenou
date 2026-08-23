-- V30 : Pages de contenu (CMS) — CGU, CGV, FAQ, à propos…

CREATE TABLE content_pages (
    id         UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    slug       VARCHAR(150) NOT NULL,
    title      VARCHAR(200) NOT NULL,
    body       TEXT        NOT NULL,
    published  BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_content_pages_slug UNIQUE (slug)
);

CREATE INDEX idx_content_pages_published ON content_pages(published);
