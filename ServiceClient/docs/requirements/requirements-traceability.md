# Requirements Traceability Matrix

**Status:** Approved (CS-01) · **Version:** 1.1.0 — implementation and test columns added in CS-03…CS-09

| Requirement | Related rules | Acceptance criteria | Implementation | Automated tests |
| --- | --- | --- | --- | --- |
| FR-001 | BR-001–BR-004 | AC-001, AC-002 | `TicketCommandService.create`, `POST /tickets` | `TicketCreationIT`, e2e scenario |
| FR-002 | BR-001–BR-002, BR-029–BR-033 | AC-001, AC-013 | ticket number sequence, SLA targets | `TicketCreationIT.ac001_*`, `ac013_*`, `TicketLifecycleTest` |
| FR-003 | BR-005–BR-006 | AC-001 | `ticket_categories`, `PUT /category` | `TicketCreationIT.categoriesAreListed`, `LifecycleIT.categoryChangeAndEscalation` |
| FR-004 | BR-007–BR-008 | AC-013, AC-016 | `PUT /priority` | `LifecycleIT.ac016_*`, `ac015_*` |
| FR-005 | BR-009–BR-011, BR-051–BR-053 | AC-004, AC-005, AC-020, AC-021 | `Workflow`, `PUT /status` | `TicketLifecycleTest`, `LifecycleIT.ac004_*`, `ac005_*`, `ReopenIT` |
| FR-006 | BR-012–BR-013, BR-057–BR-059 | AC-006, AC-022 | `PUT /assign` | `LifecycleIT.ac006_*`, `ac022_*`, `TicketAccessPolicyTest` |
| FR-007 | BR-014–BR-015 | AC-008 | `POST /messages` | `LifecycleIT.duplicateMessageRetriesAreIgnored`, `ScenarioIT`, e2e |
| FR-008 | BR-016 | AC-007 | `POST /internal-notes`, visibility | `AccessControlIT.ac007_*`, e2e scenario |
| FR-009 | BR-017–BR-018, BR-038, BR-054, BR-066 | AC-010, AC-016, AC-020 | `ticket_history` + trigger | `LifecycleIT.ac004_*`, `HistoryImmutabilityIT`, `AccessControlIT.requesterSeesOnlyRequesterVisibleHistory` |
| FR-010 | BR-021, BR-040–BR-047 | AC-017, AC-018 | `AttachmentPolicy`, storage, download endpoint | `AttachmentPolicyTest`, `AttachmentIT` |
| FR-011 | BR-019 | AC-009 | `EcosystemGateway`, `GET /context` | `ContextIT`, `ScenarioIT` |
| FR-012 | BR-003–BR-004, BR-015–BR-016, BR-019, BR-044, BR-046, BR-071–BR-076 | AC-003, AC-007, AC-018, AC-023, AC-024 | `SecurityConfig`, `TicketAccessPolicy` | `AccessControlIT`, `TicketAccessPolicyTest`, e2e |
| FR-013 | BR-020 | AC-011 | `NotificationService`, `PushDispatcher` | `NotificationIT`, `NotificationServiceTest` |
| FR-014 | — | AC-026 – AC-029 (added in CS-03) | `TicketSearchCriteria`, `GET /tickets` | `SearchIT` |
| FR-015 | BR-009–BR-011, BR-017, BR-051–BR-055 | AC-004, AC-005, AC-012, AC-020, AC-021 | resolve/close transitions | `LifecycleIT.ac012_*`, `ReopenIT` |
| FR-016 | BR-029–BR-035, BR-055 | AC-001, AC-013, AC-025 | `BusinessCalendar`, `SlaService`, SLA periods | `BusinessCalendarTest`, `SlaTargetsAndStateTest`, `SearchIT` (`slaBreached`) |
| FR-017 | BR-011, BR-048–BR-055 | AC-019, AC-020, AC-021 | reopen requests | `ReopenIT`, e2e scenario |

## Coverage note

FR-014 now has detailed acceptance criteria AC-026 … AC-029, defined in the
[API specification](../architecture/api-specification.md#search-and-filters-fr-014) during CS-03, as CS-01 and
CS-02 planned. AC-025 (SLA breach identifiable) is covered by the SLA state on every support ticket and the
`slaBreached` filter.
