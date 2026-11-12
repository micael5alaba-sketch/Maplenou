# CS-05 Validation — Ticket Backend

**Result:** PASSED (technical validation)
**Decision:** AWAITING PROJECT-OWNER APPROVAL
**Dependency:** CS-03
**Validation date:** 2026-10-08

## Roadmap items (Oct. week 1)

| Item | Implementation | Tests |
| --- | --- | --- |
| Création des tickets | `POST /tickets` (JSON or multipart with files) | `TicketCreationIT` |
| Numéro automatique | `TKT-<year>-<sequence>` | `ac001_ticketNumbersAreUnique`, `TicketLifecycleTest` |
| Catégories / priorités / statuts | closed lists, Critical reserved to support, lifecycle rules | `TicketLifecycleTest`, `LifecycleIT` |
| Attribution à un agent | self-assignment, manager reassignment, active agent only | `LifecycleIT.ac006_*`, `ac022_*` |
| Modification des tickets | priority (SLA recalculated), category, escalation | `LifecycleIT.ac016_*`, `categoryChangeAndEscalation` |
| Historique des actions | append-only history with actor, values, timestamp | `LifecycleIT.ac004_*`, `HistoryImmutabilityIT` |
| Messages, notes internes, pièces jointes | separate visibilities; DEC-003 limits | `AccessControlIT.ac007_*`, `AttachmentIT` |
| SLA | business calendar, targets, states, periods | `BusinessCalendarTest`, `SlaTargetsAndStateTest`, `TicketCreationIT.ac013_*` |
| Réouverture | request, decision, new SLA period, closed = new ticket | `ReopenIT` |
| Recherche et filtrage (FR-014) | criteria API, pagination, sort | `SearchIT` |

## CS-01 acceptance criteria

All of AC-001 … AC-025 are covered by automated tests — see the [traceability matrix](../requirements/requirements-traceability.md).
