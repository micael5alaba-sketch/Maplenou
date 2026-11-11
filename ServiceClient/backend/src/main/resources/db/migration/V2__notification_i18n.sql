-- Notifications keep the language-neutral facts next to the French title/body so that clients can
-- render them in the user's language (web UI FR/EN). See docs/architecture/notifications.md §Languages.
ALTER TABLE notifications ADD COLUMN ticket_number VARCHAR(24);
ALTER TABLE notifications ADD COLUMN detail        VARCHAR(40);

UPDATE notifications n SET ticket_number = t.ticket_number
FROM support_tickets t WHERE n.ticket_id = t.id AND n.ticket_number IS NULL;
