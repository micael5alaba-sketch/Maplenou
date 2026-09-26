-- Documents légaux du livreur (permis, pièce d'identité, assurance véhicule, carte grise),
-- avec vérification admin (PENDING -> VALID/REJECTED). Un seul document par (courier, type) :
-- un nouvel envoi remplace le précédent (pas d'historique des fichiers remplacés).
CREATE TABLE courier_documents (
    id UUID PRIMARY KEY,
    courier_id UUID NOT NULL REFERENCES users(id),
    type VARCHAR(30) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expires_at DATE,
    rejection_reason VARCHAR(500),
    reviewed_by UUID REFERENCES users(id),
    reviewed_at TIMESTAMPTZ,
    submitted_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    CONSTRAINT uk_courier_documents_courier_type UNIQUE (courier_id, type)
);

-- Filtres réels : "mes documents" (par livreur) et "file d'attente admin" (par statut).
CREATE INDEX idx_courier_documents_courier_id ON courier_documents(courier_id);
CREATE INDEX idx_courier_documents_status ON courier_documents(status);
