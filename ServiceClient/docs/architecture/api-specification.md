# API Specification

Base path: **`/api/v1/support`** · JSON · authentication: `Authorization: Bearer <JWT>` on every call.
The machine-readable contract is [openapi.json](openapi.json) (generated from the running application; the live
Swagger UI is at `/swagger-ui.html`).

## Errors (RFC 9457)

All errors are `application/problem+json`:

```json
{ "status": 422, "title": "Unprocessable Content", "detail": "La priorité Critique est attribuée uniquement par le support…",
  "code": "CRITICAL_PRIORITY_RESERVED", "rule": "BR-036" }
```

| Status | Meaning |
| --- | --- |
| 400 | Invalid input (`VALIDATION_FAILED` with `errors` per field, `UNKNOWN_CATEGORY`, `INVALID_SORT`, `MALFORMED_REQUEST`…) |
| 401 | Missing/invalid/expired token |
| 403 | Authenticated but not allowed (`FORBIDDEN` + rule, `ACCOUNT_DISABLED`) |
| 404 | Not found — **also returned to requesters for tickets that are not theirs**, so existence is not disclosed |
| 409 | Conflicting state (`TICKET_CLOSED`, `REOPEN_ALREADY_PENDING`, `REOPEN_PENDING`, `CONCURRENT_MODIFICATION`) |
| 413 | Upload larger than the multipart limit |
| 422 | Business rule violated (`INVALID_TRANSITION`, `INVALID_ASSIGNEE`, `ATTACHMENT_*`, `REFERENCE_NOT_ACCESSIBLE`…) |
| 503 | A dependency (ecosystem service) is unavailable |

## Endpoints

### Tickets

| Method & path | Who | Description | Rules |
| --- | --- | --- | --- |
| `POST /tickets` (JSON or multipart `ticket` + `files`) | Customer, seller, driver, admin | Create a ticket (status `NEW`, number, SLA). Optional `orderRef` / `deliveryRef` must belong to the caller. | FR-001/002, BR-003/004/009/019/036/037 |
| `GET /tickets` | All | List/search (see below). Requesters always get only their own tickets. | FR-014, BR-015 |
| `GET /tickets/{id}` | Owner, support | Detail with `allowedActions` and `allowedStatusTargets`; SLA and internal refs for support only. `legacyNumber`: old number (`TKT-YYYY-NNNNNN`, 6 digits) of a ticket imported from the former Maplenou support, otherwise `null` (DEC-CS10-17). | FR-012 |
| `GET /tickets/{id}/messages?visibility=PUBLIC\|INTERNAL` | Owner, support | Conversation; internal notes only for support. | FR-007/008, BR-016 |
| `POST /tickets/{id}/messages` (JSON or multipart `message` + `files`) | Owner, operational support | Customer-visible message (≤ 5 files). `clientMessageId` makes retries idempotent. | FR-007/010 |
| `POST /tickets/{id}/internal-notes` | Support (assigned agent, triage, managers) | Internal note. | FR-008 |
| `GET /tickets/{id}/attachments/{attachmentId}` | Owner (public files), support | Download. | BR-046 |
| `GET /tickets/{id}/history` | Owner (requester-visible entries), support (all) | Audit trail. | FR-009 |
| `PUT /tickets/{id}/status` `{status, comment}` | Assigned agent, manager, admin | Valid manual transitions only. | FR-005/015, BR-010 |
| `PUT /tickets/{id}/priority` `{priority, reason}` | Support with triage authority | Recalculates SLA; Critical audited. | BR-034/036/038 |
| `PUT /tickets/{id}/category` `{categoryCode}` | Support with triage authority | | BR-005 |
| `PUT /tickets/{id}/assign` `{agentId?}` | Agent (take unassigned), manager/admin (any) | Empty body = assign to me. | FR-006, BR-012/013/057/058/059 |
| `POST /tickets/{id}/escalate` `{reason}` | Support with triage authority | Flags and notifies managers. | BR-064 |
| `POST /tickets/{id}/reopen-requests` `{reason}` | Requester of a `RESOLVED` ticket | `CLOSED` → 409. | FR-017, BR-048/052 |
| `PUT /tickets/{id}/reopen-requests/{rid}` `{decision, note, targetStatus?}` | Assigned agent, manager, admin | Approve (→ `IN_PROGRESS` by default, new SLA period) or reject. | BR-050/051/055 |
| `GET /tickets/{id}/context` | Support | Requester profile, order, payment, delivery, seller, refund. | FR-011, BR-019 |

### Other resources

| Method & path | Who | Description |
| --- | --- | --- |
| `GET /me`, `GET /me/references` | All | Caller identity; own orders (customer/seller) or deliveries (driver) for the create form |
| `GET /categories` | All | Active categories |
| `GET /dashboard` | Support | Counts, distribution by priority/category, recent tickets (screen B1) |
| `GET /reports/summary?from&to` | Manager, admin | Basic report over a period (≤ 366 days) |
| `GET /agents` | Support | Active assignable agents with open load |
| `GET /admin/agents`, `PUT /admin/agents/{id}/active` | Manager (read) / admin (write) | Support staff and activation |
| `GET /admin/categories`, `PUT /admin/categories/{code}` | Manager (read) / admin (write) | Rename / deactivate categories |
| `GET /admin/holidays`, `PUT/DELETE /admin/holidays/{date}` | Manager (read) / admin (write) | Business calendar (holidays count only with `SLA_MODE=business-hours`) |
| `GET /admin/sla-settings` | Manager, admin | How deadlines are counted: `{mode, zone, opening, closing, minutesPerDay}` — `mode` `always` (24/7, `opening`/`closing` `null`, `minutesPerDay` 1440) or `business-hours` (DEC-CS10-16) |
| `GET /admin/staff-roles` | Admin | Support roles given to Maplenou accounts: `[{phone, role, assignedAt}]` (DEC-CS10-17) |
| `PUT /admin/staff-roles` `{phone, role}` | Admin | Give or change a support role (`SUPPORT_AGENT`, `SUPPORT_MANAGER`, `ADMINISTRATOR`) by phone number; applies at the next sign-in. 400 `INVALID_PHONE`, `INVALID_STAFF_ROLE` |
| `DELETE /admin/staff-roles/{phone}` | Admin | Remove a support role (204) |
| `POST /admin/legacy-import` | Admin | Import the tickets of the former Maplenou support from Bakend (`/internal/v1/support-tickets`); idempotent; returns `{imported, skipped, updated, totalInSource}` (`updated` = status changes made in the old module since the last run, noted in the ticket history as `LEGACY_UPDATED`, DEC-CS12-02). Also runs every `LEGACY_SYNC_INTERVAL` when `LEGACY_SYNC_ENABLED=true`. 422 `LEGACY_IMPORT_NOT_CONFIGURED`, 503 `ECOSYSTEM_UNAVAILABLE` |
| `GET /notifications`, `GET /notifications/unread-count`, `POST /notifications/{id}/read`, `POST /notifications/read-all` | All | In-app notifications |
| `POST /notifications/devices`, `DELETE /notifications/devices/{token}` | All | Register/unregister an FCM device token (Flutter/web apps) |
| `GET /notifications/push-status` | All | Push channel of the server (`firebase` or `log-only`) and the caller's number of registered devices |
| `POST /notifications/devices/test` | All | Sends a test push to the caller's devices at once; 422 `NO_DEVICE_REGISTERED`, 502 `PUSH_DELIVERY_FAILED` |

Demo only (`demo` profile): `GET /api/v1/dev/users`, `POST /api/v1/dev/token`.

### Sign-in with a Maplenou account (DEC-CS10-17)

Public endpoints outside the base path, present only when `MAPLENOU_LOGIN_ENABLED=true`. The phone number and
password are checked by Bakend (`/api/auth/**`); the response carries the support service's own token (8 h).

| Method & path | Body | Response / errors |
| --- | --- | --- |
| `POST /api/v1/auth/maplenou/login` | `{phoneNumber, password}` | `{mfaRequired, mfaToken, accessToken, expiresAt, user}` — with `mfaRequired: true` only `mfaToken` is set; 400 `INVALID_PHONE`, 401 `INVALID_CREDENTIALS`, 403 `ACCOUNT_DISABLED`, 429 `TOO_MANY_ATTEMPTS` (10 per phone number and minute, or Bakend's limit), 502/503 `CORE_UNAVAILABLE` |
| `POST /api/v1/auth/maplenou/mfa` | `{mfaToken, code}` (6 digits) | same response; 401 `INVALID_MFA_CODE` |

### Added in CS-11 (DEC-CS11-02 … 09)

| Method and path | Who | Purpose |
| --- | --- | --- |
| `GET /api/v1/support/vocabulary` | everyone signed in | statuses (with kind and FR/EN labels), manual transitions, priorities; `ETag` + `Cache-Control: private, max-age=300` |
| `PUT /api/v1/support/admin/workflow` | administrator | replace statuses and transitions; refused with `INVALID_WORKFLOW` / `STATUS_IN_USE` |
| `PUT /api/v1/support/admin/priorities` | administrator | replace priorities (new ones get default SLA targets); `INVALID_PRIORITIES` / `PRIORITY_IN_USE` |
| `GET`, `PUT /api/v1/support/admin/permissions` | administrator | permission matrix and its defaults; `INVALID_PERMISSIONS` |
| `GET /api/v1/support/admin/sla-policies`, `PUT …/sla-policies/{priority}` | supervisor / administrator | SLA targets per priority |
| `POST /api/v1/support/admin/categories` | administrator | add a category (`CATEGORY_EXISTS`, `INVALID_CATEGORY_CODE`); `POST` and `PUT` also take `nameEn` and `descriptionEn` (CS-12; on `PUT`, `descriptionEn` absent = unchanged, empty = removed), `PUT` takes `sortOrder` |
| `GET /api/v1/support/admin/notification-templates`, `PUT …/{type}/{language}` | supervisor / administrator | notification texts per language |
| `PUT /api/v1/support/me/preferences` `{language}` | everyone signed in | language of the caller's notifications |
| `POST /api/v1/support/tickets/{id}/satisfaction` `{rating, comment}` | requester (action RATE) | rate a resolved or closed ticket once (201) |
| `GET`, `POST /api/v1/support/canned-replies`, `PUT`, `DELETE …/{id}` | staff (read) / supervisors (write) | canned replies; `?all=true` includes inactive ones |
| `GET /api/v1/support/tickets/export.csv` | supervisor | CSV of the tickets matching the queue filters (`lang=fr|en`) |
| `GET /api/v1/support/ai/status` | staff | whether AI help is available |
| `POST /api/v1/support/tickets/{id}/ai/suggestion` `{apply}` | staff (USE_AI_ASSIST) | apply or dismiss the assistant's category/priority suggestion |
| `POST /api/v1/support/tickets/{id}/ai/draft-reply` `{language}` | staff (USE_AI_ASSIST) | reply draft from the user guide (`AI_UNAVAILABLE` when off) |
| `POST /api/v1/support/tickets/{id}/actions/refund-request` `{subOrderId?, reason, productCondition}` | REQUEST_REFUND | ask Maplenou to open a return for the buyer |
| `POST /api/v1/support/tickets/{id}/actions/reschedule-delivery` `{reason}` | RESCHEDULE_DELIVERY | ask Maplenou to release the delivery |
| `POST /api/v1/support/tickets/{id}/actions/contact-driver` `{message}` | CONTACT_DRIVER | message to the courier in the Maplenou app |
| `GET /actuator/prometheus` | `Authorization: Bearer <METRICS_TOKEN>` | Prometheus metrics (404 without a configured token) |

Ticket responses add `statusKind`, `sla.pausedSince`, `sla.resolution.state = PAUSED`, `satisfaction`,
`aiSuggestion` and `externalActions` (staff only); `references.all` lists every linked reference. Every response
carries `X-Request-Id`; `429 TOO_MANY_REQUESTS` comes with `Retry-After`. Full contract: `openapi.json` (59 paths).

## Search and filters (FR-014)

This section closes the FR-014 refinement deferred by CS-01 and CS-02 (AC-IA-010).

`GET /tickets` query parameters — all optional and combinable (AND):

| Parameter | Values | Notes |
| --- | --- | --- |
| `q` | text | A complete ticket number (`TKT-2026-00125`, or the 6-digit old number of an imported ticket) is an exact match on the number or the old number; otherwise case-insensitive "contains" on ticket number, old number of an imported ticket (`legacyNumber`, e.g. `TKT-2026-000017`) or subject |
| `status` | repeatable status | e.g. `status=NEW&status=ASSIGNED` |
| `open` | `true` | `NEW`, `ASSIGNED`, `IN_PROGRESS`, `WAITING_FOR_CUSTOMER` |
| `priority` | repeatable priority | |
| `category` | repeatable category code | |
| `assignee` | agent id, `me`, `unassigned` | support only |
| `requesterId`, `requesterRole` | uuid / `CUSTOMER`·`SELLER`·`DELIVERY_DRIVER` | support only (ignored for requesters) |
| `escalated` | `true`/`false` | support only |
| `slaBreached` | `true` | open tickets whose first-response or resolution target has passed |
| `createdFrom`, `createdTo` | `YYYY-MM-DD` | inclusive, business time zone |
| `page`, `size` | default 0 / 20, max size 100 | |
| `sort` | `createdAt`, `updatedAt`, `priorityRank`, `ticketNumber`, `status` + `,asc`/`,desc` | default `createdAt,desc` |

Response: `{ items, page, size, totalItems, totalPages }`. Items include `slaState` for support users only.

**Acceptance criteria added for FR-014** (verified by `SearchIT`):

- **AC-026** Given support filters by any combination of status, priority, category, assignee, requester role,
  escalation, SLA breach and creation dates, only matching tickets are returned, paginated and sorted as requested.
- **AC-027** Given a requester sends any filter, results are still limited to the requester's own tickets.
- **AC-028** Given a complete ticket number, exactly that ticket is returned; free text matches ticket number or subject.
- **AC-029** Given an unsupported sort field or malformed filter value, the request is rejected with 400.
