# CS-03 Validation — PostgreSQL Database and Backend/API Foundation

**Result:** PASSED (technical validation)
**Decision:** AWAITING PROJECT-OWNER APPROVAL
**Dependency:** CS-01 (approved)
**Validation date:** 2026-10-08

## Deliverables

| Roadmap deliverable (Sept. week 3) | Delivered |
| --- | --- |
| PostgreSQL model | `backend/src/main/resources/db/migration/V1__support_schema.sql` — 11 tables, constraints, append-only history trigger; [database model](../architecture/database-model.md) |
| API specification | [API specification](../architecture/api-specification.md) + generated [OpenAPI](../architecture/openapi.json) (34 paths) |
| Proposed tables (support_tickets, ticket_messages, ticket_categories, ticket_history, ticket_attachments, support_agents) | All present; `support_agents` is generalized as `support_users` (agents, managers, admins and requesters), plus reopening, SLA periods, holidays, notifications and device tokens |
| Proposed endpoints (POST/GET tickets, GET ticket, POST messages, PUT status, PUT assign) | All implemented under `/api/v1/support`, plus priority, category, escalation, internal notes, attachments, history, reopening, context |
| Stack decision | [Architecture decisions](../architecture/decisions.md) DEC-CS03-01…18 |
| FR-014 detailed behaviour (deferred by CS-01/CS-02) | Search and filter specification + AC-026…AC-029 |
| Backend skeleton, build, CI | Maven project with wrapper, Dockerfile, `.github/workflows/backend.yml` |

## Checks

| Check | Result |
| --- | --- |
| Schema applies on PostgreSQL 16 and is validated by Hibernate at start-up | PASS |
| Every CS-01 entity, status, priority and category represented | PASS |
| Database-level guarantees (unique numbers, closed lists, history immutability, one pending reopening) | PASS — `HistoryImmutabilityIT` |
| API covers every FR (FR-001…FR-017) | PASS — see traceability |
| Errors standardized (RFC 9457) | PASS |
| Stack choices documented and justified | PASS |

## Items needing owner confirmation

DEC-CS03-05 (1 business day = 10 business hours — *amended 2026-10-29 by DEC-CS10-16: superseded by the 24/7 SLA clock, one day = 24 hours; still applies with `SLA_MODE=business-hours`*), DEC-CS03-08 (agent triage on unassigned tickets),
DEC-CS03-16 (basic report scope). All other decisions follow directly from approved documents.
