-- Integration with the Maplenou core platform (Bakend), owner decision 2026-10-29 (DEC-CS10-17).

-- 1. Tickets imported from the core's former support module (Bakend table support_tickets, V46).
--    legacy_id makes the import idempotent; legacy_number keeps the number users already know (e.g. SUP-…).
ALTER TABLE support_tickets ADD COLUMN legacy_id UUID;
ALTER TABLE support_tickets ADD COLUMN legacy_number VARCHAR(30);
CREATE UNIQUE INDEX uk_support_tickets_legacy_id ON support_tickets (legacy_id) WHERE legacy_id IS NOT NULL;
CREATE INDEX idx_support_tickets_legacy_number ON support_tickets (legacy_number) WHERE legacy_number IS NOT NULL;

-- 2. Support staff roles. Maplenou accounts have no support role (only ADMIN / DELIVERY_AGENT, plus the seller
--    profile): an administrator of the support service gives the role here, by phone number. It applies at the
--    person's next sign-in with their Maplenou account.
CREATE TABLE support_staff_assignments (
    phone        VARCHAR(40) PRIMARY KEY,
    role         VARCHAR(30) NOT NULL CHECK (role IN ('SUPPORT_AGENT', 'SUPPORT_MANAGER', 'ADMINISTRATOR')),
    assigned_by  UUID        REFERENCES support_users (id),
    assigned_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
