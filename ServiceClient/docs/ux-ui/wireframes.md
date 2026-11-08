# Wireframes

## Requester

**A1 Help & Support:** title, explanation, My Tickets and Create Ticket entry points.

**A2 My Tickets:** ticket number, subject, priority, status, last update.

**A3 Create Ticket:** subject, category, description, urgency indication, optional authorized context, attachments. No direct Critical control.

**A4 Ticket Detail:** header, status/priority/category, conversation, attachments, visible history. `WAITING_FOR_CUSTOMER` highlights required action; `RESOLVED` exposes reopening request; `CLOSED` offers new ticket only.

## Support

**B1 Dashboard:** New, Unassigned, In Progress, SLA warning/breached, attention items.

**B2 Queue:** ticket ID, subject, priority, status, SLA, requester/actor, assignee.

**B3 My Tickets:** personal workload.

**B4 Support Ticket Detail:** overview, conversation, internal notes, attachments, authorized context, SLA, history/audit.

## Shared states

Loading, empty, error, unauthorized, and confirmation patterns.

## Design principles

Status is easy to find; one obvious primary action; final actions are deliberate; internal notes are structurally separated; SLA appears where decisions occur; mobile preserves hierarchy.
