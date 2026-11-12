# CS-01 Approved Business Decisions

**Status:** Approved | **Version:** 1.0.0

| Decision | Approved rule |
| --- | --- |
| DEC-001 | Priority-based SLA; SLA starts at ticket creation; business hours are Monday–Friday 08:00–18:00 excluding officially recognized public holidays. *Amended 2026-10-29 by DEC-CS10-16: SLA time is counted 24 hours a day, 7 days a week, weekends and public holidays included (`SLA_MODE=always`, default); the business-hours rule remains available as `SLA_MODE=business-hours`.* |
| DEC-002 | Only Support Agents, Support Managers, and Administrators may assign `CRITICAL`. Requester urgency does not automatically assign it. |
| DEC-003 | Attachments: JPG/JPEG/PNG/WEBP/PDF; 10 MB per file; 5 files per message; 50 MB total per ticket. |
| DEC-004 | Requesters may request reopening of `RESOLVED`; support personnel approve; `CLOSED` cannot be reopened by requesters. |
| DEC-005 | Support Agents have operational ticket-management authority but cannot normally reassign another agent's ticket or administer users/roles/system configuration. |
| DEC-006 | Support Agents can view the support queue, including unassigned and other-agent tickets, but action permissions remain independently authorized. |
| DEC-007 | Requesters can request reopening only for their own `RESOLVED` tickets; reopening is support-approved; closed cases require a new ticket. |

## SLA targets

| Priority | First response | Target resolution |
| --- | ---: | ---: |
| Low | 24 business hours | 5 business days |
| Medium | 8 business hours | 3 business days |
| High | 4 business hours | 1 business day |
| Critical | 1 business hour | 4 business hours |

*Amended 2026-10-29 by DEC-CS10-16:* the targets are unchanged but counted 24/7 by default — the hours are elapsed
hours and one "day" = 24 hours (with `SLA_MODE=business-hours`: business hours, one day = 10 business hours, DEC-CS03-05).

## Closure of decisions

All seven previously pending CS-01 business-rule decisions have been explicitly approved by the project owner on 2026-09-05.
