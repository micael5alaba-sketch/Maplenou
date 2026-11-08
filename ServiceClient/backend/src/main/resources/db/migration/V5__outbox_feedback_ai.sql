-- Iteration 6: transactional outbox (DEC-CS11-01), notification templates per language and the recipient's
-- language (DEC-CS11-09), satisfaction ratings (DEC-CS11-05), canned replies, AI suggestions (DEC-CS11-08) and the
-- audit of actions sent to Maplenou (DEC-CS11-07).

-- ---------------------------------------------------------------- transactional outbox
CREATE TABLE outbox_events (
    id               UUID PRIMARY KEY,
    event_type       VARCHAR(200) NOT NULL,
    payload          TEXT         NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL,
    published_at     TIMESTAMPTZ,
    attempts         INTEGER      NOT NULL DEFAULT 0,
    next_attempt_at  TIMESTAMPTZ,
    last_error       VARCHAR(500),
    failed           BOOLEAN      NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_outbox_due ON outbox_events (next_attempt_at) WHERE published_at IS NULL AND NOT failed;
CREATE INDEX idx_outbox_published ON outbox_events (published_at) WHERE published_at IS NOT NULL;

-- ---------------------------------------------------------------- notifications in the recipient's language
ALTER TABLE support_users ADD COLUMN language VARCHAR(2) NOT NULL DEFAULT 'fr' CHECK (language IN ('fr', 'en'));

CREATE TABLE notification_templates (
    event_type  VARCHAR(40)  NOT NULL,
    language    VARCHAR(2)   NOT NULL CHECK (language IN ('fr', 'en')),
    title       VARCHAR(160) NOT NULL,
    body        VARCHAR(500) NOT NULL,
    PRIMARY KEY (event_type, language)
);
-- Placeholders: {ticket} number, {subject}, {status} label, {decision} (acceptée/refusée, accepted/rejected)
INSERT INTO notification_templates (event_type, language, title, body) VALUES
 ('TICKET_CREATED',        'fr', 'Ticket créé', 'Votre ticket {ticket} a été créé. Nous vous informerons dès qu''il y aura une mise à jour.'),
 ('TICKET_CREATED',        'en', 'Ticket created', 'Your ticket {ticket} has been created. We will let you know as soon as there is an update.'),
 ('NEW_TICKET_IN_QUEUE',   'fr', 'Nouveau ticket', '{ticket} — {subject}'),
 ('NEW_TICKET_IN_QUEUE',   'en', 'New ticket', '{ticket} — {subject}'),
 ('TICKET_ASSIGNED',       'fr', 'Ticket attribué', '{ticket} vous a été attribué — {subject}'),
 ('TICKET_ASSIGNED',       'en', 'Ticket assigned', '{ticket} has been assigned to you — {subject}'),
 ('AGENT_REPLIED',         'fr', 'Nouvelle réponse', 'Votre ticket {ticket} a reçu une nouvelle réponse.'),
 ('AGENT_REPLIED',         'en', 'New reply', 'Your ticket {ticket} has a new reply.'),
 ('REQUESTER_REPLIED',     'fr', 'Réponse du demandeur', '{ticket} : le demandeur a répondu.'),
 ('REQUESTER_REPLIED',     'en', 'Requester replied', '{ticket}: the requester replied.'),
 ('STATUS_CHANGED',        'fr', 'Statut mis à jour', 'Le statut de votre ticket {ticket} a changé : {status}.'),
 ('STATUS_CHANGED',        'en', 'Status updated', 'The status of your ticket {ticket} changed: {status}.'),
 ('INFORMATION_REQUESTED', 'fr', 'Informations demandées', 'Le support a besoin d''informations supplémentaires pour le ticket {ticket}.'),
 ('INFORMATION_REQUESTED', 'en', 'Information requested', 'Support needs more information about ticket {ticket}.'),
 ('TICKET_RESOLVED',       'fr', 'Ticket résolu', 'Votre ticket {ticket} a été résolu. Donnez votre avis sur la réponse reçue.'),
 ('TICKET_RESOLVED',       'en', 'Ticket resolved', 'Your ticket {ticket} has been resolved. Tell us what you think of the answer.'),
 ('TICKET_CLOSED',         'fr', 'Ticket fermé', 'Votre ticket {ticket} est fermé.'),
 ('TICKET_CLOSED',         'en', 'Ticket closed', 'Your ticket {ticket} is closed.'),
 ('REOPEN_REQUESTED',      'fr', 'Demande de réouverture', '{ticket} : le demandeur souhaite rouvrir le ticket.'),
 ('REOPEN_REQUESTED',      'en', 'Reopening requested', '{ticket}: the requester wants to reopen the ticket.'),
 ('REOPEN_DECIDED',        'fr', 'Demande de réouverture traitée', 'Votre demande de réouverture du ticket {ticket} a été {decision}.'),
 ('REOPEN_DECIDED',        'en', 'Reopening request handled', 'Your request to reopen ticket {ticket} has been {decision}.'),
 ('TICKET_ESCALATED',      'fr', 'Ticket escaladé', '{ticket} — {subject}'),
 ('TICKET_ESCALATED',      'en', 'Ticket escalated', '{ticket} — {subject}');

-- ---------------------------------------------------------------- customer satisfaction (one rating per ticket)
CREATE TABLE ticket_satisfaction (
    ticket_id  UUID          PRIMARY KEY REFERENCES support_tickets (id),
    rating     SMALLINT      NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment    VARCHAR(1000),
    rated_by   UUID          NOT NULL REFERENCES support_users (id),
    rated_at   TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_satisfaction_rated_at ON ticket_satisfaction (rated_at);

-- ---------------------------------------------------------------- canned replies (« Réponses types »)
CREATE TABLE canned_replies (
    id             UUID PRIMARY KEY,
    title          VARCHAR(120)  NOT NULL,
    body           VARCHAR(5000) NOT NULL,
    language       VARCHAR(2)    NOT NULL CHECK (language IN ('fr', 'en')),
    category_code  VARCHAR(30)   REFERENCES ticket_categories (code),
    active         BOOLEAN       NOT NULL DEFAULT TRUE,
    updated_by     UUID          REFERENCES support_users (id),
    updated_at     TIMESTAMPTZ   NOT NULL
);
INSERT INTO canned_replies (id, title, body, language, category_code, active, updated_at) VALUES
 ('c0a1e5d0-0000-4000-8000-000000000001', 'Accusé de réception',
  'Bonjour {client},' || chr(10) || chr(10) || 'Merci pour votre message. Nous étudions votre demande {ticket} et revenons vers vous rapidement.' || chr(10) || chr(10) || '{agent}, service client Maplenou',
  'fr', NULL, TRUE, now()),
 ('c0a1e5d0-0000-4000-8000-000000000002', 'Demande de numéro de commande',
  'Bonjour {client},' || chr(10) || chr(10) || 'Pour vérifier votre demande, pouvez-vous nous indiquer le numéro de votre commande (il commence par CMD) ?' || chr(10) || chr(10) || '{agent}, service client Maplenou',
  'fr', 'ORDER', TRUE, now()),
 ('c0a1e5d0-0000-4000-8000-000000000003', 'Acknowledgement',
  'Hello {client},' || chr(10) || chr(10) || 'Thank you for your message. We are looking into your request {ticket} and will get back to you shortly.' || chr(10) || chr(10) || '{agent}, Maplenou customer service',
  'en', NULL, TRUE, now());

-- ---------------------------------------------------------------- AI suggestions (advisory, one per ticket)
CREATE TABLE ticket_ai_suggestions (
    ticket_id      UUID          PRIMARY KEY REFERENCES support_tickets (id),
    category_code  VARCHAR(30)   NOT NULL,
    priority_code  VARCHAR(20)   NOT NULL,
    confidence     NUMERIC(4, 3) NOT NULL CHECK (confidence BETWEEN 0 AND 1),
    reason         VARCHAR(500),
    model          VARCHAR(80),
    created_at     TIMESTAMPTZ   NOT NULL,
    decision       VARCHAR(10)   CHECK (decision IN ('APPLIED', 'DISMISSED')),
    decided_by     UUID          REFERENCES support_users (id),
    decided_at     TIMESTAMPTZ
);

-- ---------------------------------------------------------------- actions sent to Maplenou from a ticket
CREATE TABLE ticket_external_actions (
    id            UUID PRIMARY KEY,
    ticket_id     UUID         NOT NULL REFERENCES support_tickets (id),
    action_type   VARCHAR(30)  NOT NULL CHECK (action_type IN ('REFUND_REQUEST', 'DELIVERY_RESCHEDULE', 'DRIVER_MESSAGE')),
    target_ref    VARCHAR(60)  NOT NULL,
    succeeded     BOOLEAN      NOT NULL,
    result_ref    VARCHAR(60),
    error         VARCHAR(500),
    requested_by  UUID         NOT NULL REFERENCES support_users (id),
    created_at    TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_external_actions_ticket ON ticket_external_actions (ticket_id, created_at);
