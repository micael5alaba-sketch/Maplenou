-- =====================================================================
-- Maplenou Customer Service — CS-03 database foundation
-- Source of truth: docs/architecture/database-model.md
-- Rules referenced as BR-xxx come from docs/requirements/business-rules.md
-- =====================================================================

-- ---------------------------------------------------------------------
-- Users known to the support module (projection of the Maplenou identity).
-- Rows are created/refreshed from the authenticated token (BR-004).
-- ---------------------------------------------------------------------
CREATE TABLE support_users (
    id            UUID PRIMARY KEY,
    display_name  VARCHAR(120) NOT NULL,
    email         VARCHAR(254),
    phone         VARCHAR(40),
    role          VARCHAR(30)  NOT NULL
        CHECK (role IN ('CUSTOMER', 'SELLER', 'DELIVERY_DRIVER',
                        'SUPPORT_AGENT', 'SUPPORT_MANAGER', 'ADMINISTRATOR')),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_support_users_role ON support_users (role) WHERE active;

-- ---------------------------------------------------------------------
-- Categories (BR-005, BR-006): closed list in the MVP.
-- Administrators may rename/deactivate; new codes need a change request.
-- ---------------------------------------------------------------------
CREATE TABLE ticket_categories (
    id           UUID PRIMARY KEY,
    code         VARCHAR(30)  NOT NULL UNIQUE,
    name         VARCHAR(80)  NOT NULL,
    description  VARCHAR(255),
    sort_order   SMALLINT     NOT NULL DEFAULT 0,
    active       BOOLEAN      NOT NULL DEFAULT TRUE
);

INSERT INTO ticket_categories (id, code, name, description, sort_order) VALUES
 ('0b1f0001-0000-4000-8000-000000000001', 'ORDER',         'Commande',               'Problèmes liés à une commande',             1),
 ('0b1f0001-0000-4000-8000-000000000002', 'PAYMENT',       'Paiement',               'Paiements, commissions et reversements',    2),
 ('0b1f0001-0000-4000-8000-000000000003', 'DELIVERY',      'Livraison',              'Suivi et incidents de livraison',           3),
 ('0b1f0001-0000-4000-8000-000000000004', 'SELLER',        'Vendeur',                'Problèmes avec un vendeur',                 4),
 ('0b1f0001-0000-4000-8000-000000000005', 'ACCOUNT',       'Compte',                 'Accès et gestion du compte',                5),
 ('0b1f0001-0000-4000-8000-000000000006', 'RETURN_REFUND', 'Retour / remboursement', 'Retours, échanges et remboursements',       6),
 ('0b1f0001-0000-4000-8000-000000000007', 'OTHER',         'Autre',                  'Autres demandes',                           7);

-- ---------------------------------------------------------------------
-- Ticket numbers are generated from a sequence and never reused (BR-002).
-- Format applied by the application: TKT-<year>-<sequence padded to 5>.
-- ---------------------------------------------------------------------
CREATE SEQUENCE ticket_number_seq START WITH 1 INCREMENT BY 1 NO CYCLE;

-- ---------------------------------------------------------------------
-- Tickets
-- ---------------------------------------------------------------------
CREATE TABLE support_tickets (
    id                     UUID PRIMARY KEY,
    ticket_number          VARCHAR(24)  NOT NULL UNIQUE,
    subject                VARCHAR(200) NOT NULL,
    description            TEXT         NOT NULL,
    category_id            UUID         NOT NULL REFERENCES ticket_categories (id),
    priority               VARCHAR(10)  NOT NULL
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    priority_rank          SMALLINT GENERATED ALWAYS AS (
        CASE priority WHEN 'CRITICAL' THEN 4 WHEN 'HIGH' THEN 3 WHEN 'MEDIUM' THEN 2 ELSE 1 END
    ) STORED,
    status                 VARCHAR(25)  NOT NULL
        CHECK (status IN ('NEW', 'ASSIGNED', 'IN_PROGRESS', 'WAITING_FOR_CUSTOMER', 'RESOLVED', 'CLOSED')),
    requester_id           UUID         NOT NULL REFERENCES support_users (id),
    requester_role         VARCHAR(30)  NOT NULL
        CHECK (requester_role IN ('CUSTOMER', 'SELLER', 'DELIVERY_DRIVER', 'ADMINISTRATOR')),
    requester_reported_urgent BOOLEAN   NOT NULL DEFAULT FALSE,   -- BR-037
    assigned_agent_id      UUID REFERENCES support_users (id),
    escalated              BOOLEAN      NOT NULL DEFAULT FALSE,
    escalated_at           TIMESTAMPTZ,
    -- Contextual references to other Maplenou modules (BR-019)
    order_ref              VARCHAR(40),
    payment_ref            VARCHAR(40),
    delivery_ref           VARCHAR(40),
    seller_ref             VARCHAR(40),
    refund_ref             VARCHAR(40),
    -- SLA (BR-029..BR-035)
    first_response_due_at  TIMESTAMPTZ  NOT NULL,
    first_responded_at     TIMESTAMPTZ,
    resolution_due_at      TIMESTAMPTZ  NOT NULL,
    resolution_period_started_at TIMESTAMPTZ NOT NULL,
    resolved_at            TIMESTAMPTZ,
    closed_at              TIMESTAMPTZ,
    created_at             TIMESTAMPTZ  NOT NULL,
    updated_at             TIMESTAMPTZ  NOT NULL,
    version                BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT chk_closed_has_timestamp CHECK (status <> 'CLOSED' OR closed_at IS NOT NULL),
    CONSTRAINT chk_resolved_has_timestamp CHECK (status NOT IN ('RESOLVED', 'CLOSED') OR resolved_at IS NOT NULL)
);
CREATE INDEX idx_tickets_requester   ON support_tickets (requester_id, created_at DESC);
CREATE INDEX idx_tickets_assignee    ON support_tickets (assigned_agent_id, status);
CREATE INDEX idx_tickets_status      ON support_tickets (status, priority_rank DESC, created_at);
CREATE INDEX idx_tickets_category    ON support_tickets (category_id);
CREATE INDEX idx_tickets_created_at  ON support_tickets (created_at);
CREATE INDEX idx_tickets_order_ref   ON support_tickets (order_ref) WHERE order_ref IS NOT NULL;
CREATE INDEX idx_tickets_delivery_ref ON support_tickets (delivery_ref) WHERE delivery_ref IS NOT NULL;

-- ---------------------------------------------------------------------
-- Messages and internal notes (BR-014..BR-016). Visibility is structural.
-- client_message_id makes sending idempotent (AC-SB-006, no duplicates).
-- ---------------------------------------------------------------------
CREATE TABLE ticket_messages (
    id                 UUID PRIMARY KEY,
    ticket_id          UUID         NOT NULL REFERENCES support_tickets (id),
    author_id          UUID         NOT NULL REFERENCES support_users (id),
    author_role        VARCHAR(30)  NOT NULL,
    visibility         VARCHAR(10)  NOT NULL CHECK (visibility IN ('PUBLIC', 'INTERNAL')),
    body               TEXT         NOT NULL,
    client_message_id  VARCHAR(64),
    created_at         TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_message_idempotency UNIQUE (ticket_id, author_id, client_message_id)
);
CREATE INDEX idx_messages_ticket ON ticket_messages (ticket_id, created_at);

-- ---------------------------------------------------------------------
-- Attachments (DEC-003, BR-040..BR-047). Files live in object storage;
-- only metadata is stored here. Limits are enforced by the application.
-- ---------------------------------------------------------------------
CREATE TABLE ticket_attachments (
    id                 UUID PRIMARY KEY,
    ticket_id          UUID         NOT NULL REFERENCES support_tickets (id),
    message_id         UUID         NOT NULL REFERENCES ticket_messages (id),
    uploaded_by        UUID         NOT NULL REFERENCES support_users (id),
    original_filename  VARCHAR(255) NOT NULL,
    content_type       VARCHAR(50)  NOT NULL
        CHECK (content_type IN ('image/jpeg', 'image/png', 'image/webp', 'application/pdf')),
    size_bytes         BIGINT       NOT NULL CHECK (size_bytes > 0 AND size_bytes <= 10485760),
    storage_key        VARCHAR(255) NOT NULL UNIQUE,
    sha256             VARCHAR(64)  NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_attachments_ticket ON ticket_attachments (ticket_id);
CREATE INDEX idx_attachments_message ON ticket_attachments (message_id);

-- ---------------------------------------------------------------------
-- Ticket history / audit (BR-017, BR-018): append-only.
-- ---------------------------------------------------------------------
CREATE TABLE ticket_history (
    id           UUID PRIMARY KEY,
    ticket_id    UUID         NOT NULL REFERENCES support_tickets (id),
    actor_id     UUID         REFERENCES support_users (id),
    actor_role   VARCHAR(30),
    action       VARCHAR(40)  NOT NULL,
    old_value    VARCHAR(255),
    new_value    VARCHAR(255),
    details      VARCHAR(500),
    requester_visible BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_history_ticket ON ticket_history (ticket_id, created_at);

CREATE FUNCTION forbid_history_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'ticket_history is append-only (BR-018)';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_ticket_history_append_only
    BEFORE UPDATE OR DELETE ON ticket_history
    FOR EACH ROW EXECUTE FUNCTION forbid_history_mutation();

-- ---------------------------------------------------------------------
-- Reopening requests (DEC-004, DEC-007, BR-048..BR-054)
-- ---------------------------------------------------------------------
CREATE TABLE ticket_reopen_requests (
    id             UUID PRIMARY KEY,
    ticket_id      UUID         NOT NULL REFERENCES support_tickets (id),
    requested_by   UUID         NOT NULL REFERENCES support_users (id),
    reason         VARCHAR(1000) NOT NULL,
    status         VARCHAR(10)  NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    decided_by     UUID REFERENCES support_users (id),
    decision_note  VARCHAR(1000),
    created_at     TIMESTAMPTZ  NOT NULL,
    decided_at     TIMESTAMPTZ
);
CREATE UNIQUE INDEX uq_one_pending_reopen_per_ticket
    ON ticket_reopen_requests (ticket_id) WHERE status = 'PENDING';

-- ---------------------------------------------------------------------
-- Resolution SLA periods: reopening keeps history and opens a new period (BR-055)
-- ---------------------------------------------------------------------
CREATE TABLE ticket_sla_periods (
    id                 UUID PRIMARY KEY,
    ticket_id          UUID         NOT NULL REFERENCES support_tickets (id),
    period_number      SMALLINT     NOT NULL,
    priority           VARCHAR(10)  NOT NULL,
    started_at         TIMESTAMPTZ  NOT NULL,
    resolution_due_at  TIMESTAMPTZ  NOT NULL,
    ended_at           TIMESTAMPTZ,
    end_reason         VARCHAR(20),
    CONSTRAINT uq_sla_period UNIQUE (ticket_id, period_number)
);

-- ---------------------------------------------------------------------
-- Business calendar: officially recognized public holidays (DEC-001, BR-032).
-- Maintained by administrators; not seeded (the official list must be loaded).
-- ---------------------------------------------------------------------
CREATE TABLE business_holidays (
    holiday_date  DATE PRIMARY KEY,
    label         VARCHAR(120) NOT NULL
);

-- ---------------------------------------------------------------------
-- Notifications: in-app inbox + push delivery state (FR-013, BR-020).
-- Rows are written in the same transaction as the ticket change; push is
-- dispatched asynchronously so a delivery failure never touches ticket state.
-- ---------------------------------------------------------------------
CREATE TABLE notifications (
    id                    UUID PRIMARY KEY,
    recipient_id          UUID         NOT NULL REFERENCES support_users (id),
    ticket_id             UUID REFERENCES support_tickets (id),
    event_type            VARCHAR(40)  NOT NULL,
    title                 VARCHAR(160) NOT NULL,
    body                  VARCHAR(500) NOT NULL,
    read_at               TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL,
    push_status           VARCHAR(10)  NOT NULL CHECK (push_status IN ('PENDING', 'SENT', 'FAILED', 'SKIPPED')),
    push_attempts         SMALLINT     NOT NULL DEFAULT 0,
    push_next_attempt_at  TIMESTAMPTZ,
    push_last_error       VARCHAR(500),
    push_sent_at          TIMESTAMPTZ
);
CREATE INDEX idx_notifications_recipient ON notifications (recipient_id, created_at DESC);
CREATE INDEX idx_notifications_push_queue ON notifications (push_next_attempt_at) WHERE push_status = 'PENDING';

CREATE TABLE device_tokens (
    id            UUID PRIMARY KEY,
    user_id       UUID         NOT NULL REFERENCES support_users (id),
    token         VARCHAR(512) NOT NULL UNIQUE,
    platform      VARCHAR(10)  NOT NULL CHECK (platform IN ('ANDROID', 'IOS', 'WEB')),
    created_at    TIMESTAMPTZ  NOT NULL,
    last_seen_at  TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_device_tokens_user ON device_tokens (user_id);
