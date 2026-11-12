-- Iteration 6 (DEC-CS11-02, DEC-CS11-03): the business vocabulary becomes data so the ticketing core can serve other
-- domains (reference architecture §4.3): statuses and transitions, priorities, SLA targets and the permission matrix
-- live in tables; the five Maplenou reference columns become generic (type, reference) pairs; the resolution clock
-- can be paused. Existing tickets keep their values: the seeds reproduce exactly the V1 rules.

-- ---------------------------------------------------------------- statuses and manual transitions
CREATE TABLE ticket_statuses (
    code        VARCHAR(25) PRIMARY KEY CHECK (code ~ '^[A-Z][A-Z0-9_]{1,24}$'),
    kind        VARCHAR(20) NOT NULL
        CHECK (kind IN ('NEW', 'ASSIGNED', 'IN_PROGRESS', 'WAITING_REQUESTER', 'ON_HOLD', 'RESOLVED', 'CLOSED')),
    label_fr    VARCHAR(60) NOT NULL,
    label_en    VARCHAR(60),
    sort_order  SMALLINT    NOT NULL DEFAULT 0,
    sla_paused  BOOLEAN     NOT NULL DEFAULT FALSE,
    system      BOOLEAN     NOT NULL DEFAULT FALSE,
    active      BOOLEAN     NOT NULL DEFAULT TRUE,
    -- statuses added by administrators may only be "in progress" or "waiting for a third party"
    CONSTRAINT chk_custom_status_kind CHECK (system OR kind IN ('IN_PROGRESS', 'ON_HOLD'))
);
-- one system status per kind: the automatic changes (assignment, replies, reopening) use it
CREATE UNIQUE INDEX uq_system_status_kind ON ticket_statuses (kind) WHERE system;

INSERT INTO ticket_statuses (code, kind, label_fr, label_en, sort_order, sla_paused, system) VALUES
 ('NEW',                  'NEW',               'Nouveau',              'New',                  1, FALSE, TRUE),
 ('ASSIGNED',             'ASSIGNED',          'Attribué',             'Assigned',             2, FALSE, TRUE),
 ('IN_PROGRESS',          'IN_PROGRESS',       'En cours',             'In progress',          3, FALSE, TRUE),
 ('WAITING_FOR_CUSTOMER', 'WAITING_REQUESTER', 'En attente du client', 'Waiting for customer', 4, TRUE,  TRUE),
 ('RESOLVED',             'RESOLVED',          'Résolu',               'Resolved',             5, FALSE, TRUE),
 ('CLOSED',               'CLOSED',            'Fermé',                'Closed',               6, FALSE, TRUE);

CREATE TABLE ticket_transitions (
    from_code  VARCHAR(25) NOT NULL REFERENCES ticket_statuses (code) ON DELETE CASCADE,
    to_code    VARCHAR(25) NOT NULL REFERENCES ticket_statuses (code) ON DELETE CASCADE,
    PRIMARY KEY (from_code, to_code)
);
INSERT INTO ticket_transitions (from_code, to_code) VALUES
 ('ASSIGNED', 'IN_PROGRESS'), ('ASSIGNED', 'WAITING_FOR_CUSTOMER'),
 ('IN_PROGRESS', 'WAITING_FOR_CUSTOMER'), ('IN_PROGRESS', 'RESOLVED'),
 ('WAITING_FOR_CUSTOMER', 'IN_PROGRESS'), ('WAITING_FOR_CUSTOMER', 'RESOLVED'),
 ('RESOLVED', 'CLOSED');

-- ---------------------------------------------------------------- priorities and SLA targets
CREATE TABLE ticket_priorities (
    code                  VARCHAR(20) PRIMARY KEY CHECK (code ~ '^[A-Z][A-Z0-9_]{1,19}$'),
    rank                  SMALLINT    NOT NULL UNIQUE,
    label_fr              VARCHAR(40) NOT NULL,
    label_en              VARCHAR(40),
    requester_selectable  BOOLEAN     NOT NULL DEFAULT TRUE,
    is_default            BOOLEAN     NOT NULL DEFAULT FALSE,
    system                BOOLEAN     NOT NULL DEFAULT FALSE,
    active                BOOLEAN     NOT NULL DEFAULT TRUE
);
CREATE UNIQUE INDEX uq_default_priority ON ticket_priorities (is_default) WHERE is_default;
INSERT INTO ticket_priorities (code, rank, label_fr, label_en, requester_selectable, is_default, system) VALUES
 ('LOW',      1, 'Basse',    'Low',      TRUE,  FALSE, TRUE),
 ('MEDIUM',   2, 'Moyenne',  'Medium',   TRUE,  TRUE,  TRUE),
 ('HIGH',     3, 'Élevée',   'High',     TRUE,  FALSE, TRUE),
 ('CRITICAL', 4, 'Critique', 'Critical', FALSE, FALSE, TRUE);   -- BR-036: given by support only

-- SLA module table: no foreign key to the ticket module (module boundary); the application keeps them in step
CREATE TABLE sla_policies (
    priority_code           VARCHAR(20) PRIMARY KEY,
    first_response_minutes  INTEGER     NOT NULL CHECK (first_response_minutes > 0),
    resolution_minutes      INTEGER     CHECK (resolution_minutes > 0),
    resolution_days         INTEGER     CHECK (resolution_days > 0),
    CONSTRAINT chk_one_resolution_target CHECK ((resolution_minutes IS NULL) <> (resolution_days IS NULL))
);
-- DEC-001
INSERT INTO sla_policies (priority_code, first_response_minutes, resolution_minutes, resolution_days) VALUES
 ('LOW', 1440, NULL, 5), ('MEDIUM', 480, NULL, 3), ('HIGH', 240, NULL, 1), ('CRITICAL', 60, 240, NULL);

-- ---------------------------------------------------------------- permission matrix (docs/requirements/authorization-matrix.md)
CREATE TABLE permission_rules (
    role    VARCHAR(30) NOT NULL CHECK (role IN ('CUSTOMER', 'SELLER', 'DELIVERY_DRIVER',
                                                 'SUPPORT_AGENT', 'SUPPORT_MANAGER', 'ADMINISTRATOR')),
    action  VARCHAR(40) NOT NULL,
    scope   VARCHAR(12) NOT NULL CHECK (scope IN ('OWN', 'ASSIGNED', 'UNASSIGNED', 'ANY')),
    PRIMARY KEY (role, action, scope)
);
INSERT INTO permission_rules (role, action, scope) VALUES
 ('CUSTOMER', 'SEND_MESSAGE', 'OWN'),
 ('CUSTOMER', 'REQUEST_REOPEN', 'OWN'),
 ('CUSTOMER', 'RATE', 'OWN'),
 ('SELLER', 'SEND_MESSAGE', 'OWN'),
 ('SELLER', 'REQUEST_REOPEN', 'OWN'),
 ('SELLER', 'RATE', 'OWN'),
 ('DELIVERY_DRIVER', 'SEND_MESSAGE', 'OWN'),
 ('DELIVERY_DRIVER', 'REQUEST_REOPEN', 'OWN'),
 ('DELIVERY_DRIVER', 'RATE', 'OWN'),
 ('ADMINISTRATOR', 'SEND_MESSAGE', 'OWN'),
 ('ADMINISTRATOR', 'REQUEST_REOPEN', 'OWN'),
 ('ADMINISTRATOR', 'RATE', 'OWN'),
 ('SUPPORT_AGENT', 'VIEW_INTERNAL', 'ANY'),
 ('SUPPORT_AGENT', 'VIEW_CONTEXT', 'ANY'),
 ('SUPPORT_AGENT', 'VIEW_SLA', 'ANY'),
 ('SUPPORT_MANAGER', 'VIEW_INTERNAL', 'ANY'),
 ('SUPPORT_MANAGER', 'VIEW_CONTEXT', 'ANY'),
 ('SUPPORT_MANAGER', 'VIEW_SLA', 'ANY'),
 ('ADMINISTRATOR', 'VIEW_INTERNAL', 'ANY'),
 ('ADMINISTRATOR', 'VIEW_CONTEXT', 'ANY'),
 ('ADMINISTRATOR', 'VIEW_SLA', 'ANY'),
 ('SUPPORT_AGENT', 'CHANGE_PRIORITY', 'ASSIGNED'),
 ('SUPPORT_AGENT', 'CHANGE_PRIORITY', 'UNASSIGNED'),
 ('SUPPORT_MANAGER', 'CHANGE_PRIORITY', 'ANY'),
 ('ADMINISTRATOR', 'CHANGE_PRIORITY', 'ANY'),
 ('SUPPORT_AGENT', 'SET_CRITICAL', 'ASSIGNED'),
 ('SUPPORT_AGENT', 'SET_CRITICAL', 'UNASSIGNED'),
 ('SUPPORT_MANAGER', 'SET_CRITICAL', 'ANY'),
 ('ADMINISTRATOR', 'SET_CRITICAL', 'ANY'),
 ('SUPPORT_AGENT', 'CHANGE_CATEGORY', 'ASSIGNED'),
 ('SUPPORT_AGENT', 'CHANGE_CATEGORY', 'UNASSIGNED'),
 ('SUPPORT_MANAGER', 'CHANGE_CATEGORY', 'ANY'),
 ('ADMINISTRATOR', 'CHANGE_CATEGORY', 'ANY'),
 ('SUPPORT_AGENT', 'ADD_INTERNAL_NOTE', 'ASSIGNED'),
 ('SUPPORT_AGENT', 'ADD_INTERNAL_NOTE', 'UNASSIGNED'),
 ('SUPPORT_MANAGER', 'ADD_INTERNAL_NOTE', 'ANY'),
 ('ADMINISTRATOR', 'ADD_INTERNAL_NOTE', 'ANY'),
 ('SUPPORT_AGENT', 'ESCALATE', 'ASSIGNED'),
 ('SUPPORT_AGENT', 'ESCALATE', 'UNASSIGNED'),
 ('SUPPORT_MANAGER', 'ESCALATE', 'ANY'),
 ('ADMINISTRATOR', 'ESCALATE', 'ANY'),
 ('SUPPORT_AGENT', 'USE_AI_ASSIST', 'ASSIGNED'),
 ('SUPPORT_AGENT', 'USE_AI_ASSIST', 'UNASSIGNED'),
 ('SUPPORT_MANAGER', 'USE_AI_ASSIST', 'ANY'),
 ('ADMINISTRATOR', 'USE_AI_ASSIST', 'ANY'),
 ('SUPPORT_AGENT', 'SEND_MESSAGE', 'ASSIGNED'),
 ('SUPPORT_MANAGER', 'SEND_MESSAGE', 'ANY'),
 ('ADMINISTRATOR', 'SEND_MESSAGE', 'ANY'),
 ('SUPPORT_AGENT', 'CHANGE_STATUS', 'ASSIGNED'),
 ('SUPPORT_MANAGER', 'CHANGE_STATUS', 'ANY'),
 ('ADMINISTRATOR', 'CHANGE_STATUS', 'ANY'),
 ('SUPPORT_AGENT', 'RESOLVE', 'ASSIGNED'),
 ('SUPPORT_MANAGER', 'RESOLVE', 'ANY'),
 ('ADMINISTRATOR', 'RESOLVE', 'ANY'),
 ('SUPPORT_AGENT', 'CLOSE', 'ASSIGNED'),
 ('SUPPORT_MANAGER', 'CLOSE', 'ANY'),
 ('ADMINISTRATOR', 'CLOSE', 'ANY'),
 ('SUPPORT_AGENT', 'DECIDE_REOPEN', 'ASSIGNED'),
 ('SUPPORT_MANAGER', 'DECIDE_REOPEN', 'ANY'),
 ('ADMINISTRATOR', 'DECIDE_REOPEN', 'ANY'),
 ('SUPPORT_AGENT', 'REQUEST_REFUND', 'ASSIGNED'),
 ('SUPPORT_MANAGER', 'REQUEST_REFUND', 'ANY'),
 ('ADMINISTRATOR', 'REQUEST_REFUND', 'ANY'),
 ('SUPPORT_AGENT', 'RESCHEDULE_DELIVERY', 'ASSIGNED'),
 ('SUPPORT_MANAGER', 'RESCHEDULE_DELIVERY', 'ANY'),
 ('ADMINISTRATOR', 'RESCHEDULE_DELIVERY', 'ANY'),
 ('SUPPORT_AGENT', 'CONTACT_DRIVER', 'ASSIGNED'),
 ('SUPPORT_MANAGER', 'CONTACT_DRIVER', 'ANY'),
 ('ADMINISTRATOR', 'CONTACT_DRIVER', 'ANY'),
 ('SUPPORT_AGENT', 'ASSIGN_SELF', 'UNASSIGNED'),
 ('SUPPORT_MANAGER', 'ASSIGN_SELF', 'ANY'),
 ('SUPPORT_MANAGER', 'REASSIGN', 'ANY'),
 ('ADMINISTRATOR', 'REASSIGN', 'ANY');

-- ---------------------------------------------------------------- tickets use the vocabulary tables
ALTER TABLE support_tickets DROP CONSTRAINT support_tickets_priority_check;
ALTER TABLE support_tickets DROP CONSTRAINT support_tickets_status_check;
-- the rank now comes from ticket_priorities (kept in step by the application) instead of a fixed CASE
ALTER TABLE support_tickets ALTER COLUMN priority_rank DROP EXPRESSION;
ALTER TABLE support_tickets ALTER COLUMN priority_rank TYPE INTEGER;
ALTER TABLE support_tickets ALTER COLUMN priority_rank SET NOT NULL;
ALTER TABLE support_tickets ALTER COLUMN priority TYPE VARCHAR(20);
ALTER TABLE support_tickets ADD CONSTRAINT fk_ticket_status FOREIGN KEY (status) REFERENCES ticket_statuses (code);
ALTER TABLE support_tickets ADD CONSTRAINT fk_ticket_priority FOREIGN KEY (priority) REFERENCES ticket_priorities (code);
ALTER TABLE ticket_sla_periods ALTER COLUMN priority TYPE VARCHAR(20);

-- categories are referenced by code; administrators may add some (DEC-CS11-06)
ALTER TABLE ticket_categories ADD COLUMN name_en VARCHAR(80);
UPDATE ticket_categories SET name_en = CASE code
    WHEN 'ORDER' THEN 'Order' WHEN 'PAYMENT' THEN 'Payment' WHEN 'DELIVERY' THEN 'Delivery'
    WHEN 'SELLER' THEN 'Seller' WHEN 'ACCOUNT' THEN 'Account' WHEN 'RETURN_REFUND' THEN 'Return / refund'
    ELSE 'Other' END;
ALTER TABLE support_tickets ADD COLUMN category_code VARCHAR(30);
UPDATE support_tickets t SET category_code = c.code FROM ticket_categories c WHERE c.id = t.category_id;
ALTER TABLE support_tickets ALTER COLUMN category_code SET NOT NULL;
ALTER TABLE support_tickets ADD CONSTRAINT fk_ticket_category FOREIGN KEY (category_code) REFERENCES ticket_categories (code);
DROP INDEX idx_tickets_category;
ALTER TABLE support_tickets DROP COLUMN category_id;
CREATE INDEX idx_tickets_category ON support_tickets (category_code);

-- ---------------------------------------------------------------- generic context references
CREATE TABLE ticket_context_refs (
    ticket_id  UUID        NOT NULL REFERENCES support_tickets (id),
    ref_type   VARCHAR(20) NOT NULL,
    ref        VARCHAR(60) NOT NULL,
    PRIMARY KEY (ticket_id, ref_type)
);
CREATE INDEX idx_context_refs_lookup ON ticket_context_refs (ref_type, ref);
INSERT INTO ticket_context_refs (ticket_id, ref_type, ref)
          SELECT id, 'ORDER',    order_ref    FROM support_tickets WHERE order_ref    IS NOT NULL
UNION ALL SELECT id, 'PAYMENT',  payment_ref  FROM support_tickets WHERE payment_ref  IS NOT NULL
UNION ALL SELECT id, 'DELIVERY', delivery_ref FROM support_tickets WHERE delivery_ref IS NOT NULL
UNION ALL SELECT id, 'SELLER',   seller_ref   FROM support_tickets WHERE seller_ref   IS NOT NULL
UNION ALL SELECT id, 'REFUND',   refund_ref   FROM support_tickets WHERE refund_ref   IS NOT NULL;
DROP INDEX idx_tickets_order_ref;
DROP INDEX idx_tickets_delivery_ref;
ALTER TABLE support_tickets DROP COLUMN order_ref, DROP COLUMN payment_ref, DROP COLUMN delivery_ref,
    DROP COLUMN seller_ref, DROP COLUMN refund_ref;

-- ---------------------------------------------------------------- SLA pause (DEC-CS11-03) and automatic closure (DEC-CS11-04)
ALTER TABLE support_tickets ADD COLUMN sla_paused_since TIMESTAMPTZ;
ALTER TABLE ticket_sla_periods ADD COLUMN paused_minutes BIGINT NOT NULL DEFAULT 0;
CREATE INDEX idx_tickets_resolved_open ON support_tickets (resolved_at) WHERE closed_at IS NULL;
