-- Iteration 7 (CS-12).
--
-- 1. Keep ServiceClient in step with the former Maplenou support module while it is still used by the
-- Flutter app (DEC-CS12-02). One row per imported legacy ticket: the legacy status last seen, so a later change in
-- the old module (e.g. resolved there) is noticed once and shown to the agents in the ticket history.
CREATE TABLE legacy_ticket_sync (
    legacy_id      UUID         PRIMARY KEY,
    ticket_id      UUID         NOT NULL REFERENCES support_tickets (id) ON DELETE CASCADE,
    legacy_status  VARCHAR(20),
    synced_at      TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_legacy_sync_ticket ON legacy_ticket_sync (ticket_id);

-- Tickets imported before this version: their legacy status at import time is unknown (NULL); the next sync only
-- records the current one.
INSERT INTO legacy_ticket_sync (legacy_id, ticket_id, legacy_status, synced_at)
SELECT legacy_id, id, NULL, now() FROM support_tickets WHERE legacy_id IS NOT NULL;

-- 2. English description of the categories (help page and create form in English showed the French text).
ALTER TABLE ticket_categories ADD COLUMN description_en VARCHAR(255);
UPDATE ticket_categories SET description_en = CASE code
    WHEN 'ORDER'         THEN 'Problems with an order'
    WHEN 'PAYMENT'       THEN 'Payments, fees and payouts'
    WHEN 'DELIVERY'      THEN 'Delivery tracking and incidents'
    WHEN 'SELLER'        THEN 'Problems with a seller'
    WHEN 'ACCOUNT'       THEN 'Account access and management'
    WHEN 'RETURN_REFUND' THEN 'Returns, exchanges and refunds'
    WHEN 'OTHER'         THEN 'Other requests'
END
WHERE description_en IS NULL AND description IN (
    'Problèmes liés à une commande', 'Paiements, commissions et reversements', 'Suivi et incidents de livraison',
    'Problèmes avec un vendeur', 'Accès et gestion du compte', 'Retours, échanges et remboursements', 'Autres demandes');
