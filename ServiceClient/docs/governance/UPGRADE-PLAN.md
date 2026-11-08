# Upgrade and Enrichment Plan

This file is the controlled resource for improving the project without destabilizing the active MVP.

## Rule

An upgrade is not automatically part of the current implementation. It must first be classified and approved.

## Upgrade categories

| Category | Example | When to consider |
| --- | --- | --- |
| Security | stronger authorization/audit | Before production/pilot |
| Reliability | retries, idempotency, resilience | After core lifecycle |
| Performance | indexes, caching | After measured need |
| Observability | metrics, structured logs, tracing | Before pilot |
| Developer experience | local tooling, CI improvements | Continuously when low-risk |
| Documentation | diagrams, API examples | Whenever behavior changes |
| UX | search/filter/accessibility | After core workflow |
| Intelligence | AI assistance | Post-MVP unless explicitly approved |

## Proposal format

Every proposed upgrade should specify:

- Problem
- Expected benefit
- Scope
- Dependencies
- Risks
- Testing impact
- Documentation impact
- Estimated effort
- Whether it affects the MVP deadline

## Approval

An upgrade enters the active development matrix only after explicit project approval.

## Candidate upgrades delivered (CS-10 and CS-11)

Every item of the former backlog was approved by the project owner (29–30/10/2026) and delivered. Decisions:
[DEC-CS11-01 … 10](../architecture/decisions.md); validation: [CS-11](../validation/CS-11-VALIDATION.md).

| Candidate | Category | Delivered | Where |
| --- | --- | --- | --- |
| Identity-provider integration (replace demo login) | Security | CS-10 | sign-in with the Maplenou account through Bakend `/api/auth/*` (`identity` module), demo login only with the `demo` profile |
| Validate and enable the core-services HTTP contract | Reliability | CS-10, CS-11 | Bakend `supportbridge` (`/internal/v1/**`), `ECOSYSTEM_MODE=http`; contract tests `BakendContractTest` (here) and `OrderLifecycleIntegrationTest` (Bakend) |
| Firebase project + device tests with the Flutter apps | Reliability | CS-11 | requesters' pushes relayed to the Maplenou app by Bakend (`MAPLENOU_PUSH_RELAY`, Bakend FCM tokens); the app opens the ticket (`url` in the push data). Device test with your Firebase project: [firebase guide](../guide/firebase.md) |
| Rate limiting, centralized logs, metrics, alerts | Security / Observability | CS-11 | `RateLimitFilter` (429), JSON logs `LOG_FORMAT=ecs` + `X-Request-Id`, `/actuator/prometheus` (`METRICS_TOKEN`), `monitoring` profile with alert rules |
| Cloudinary storage with authenticated delivery, antivirus scan | Security | CS-11 | `ATTACHMENT_STORAGE=cloudinary` (signed, private, 5-minute download links), `ANTIVIRUS=clamav` + `antivirus` profile, fail-closed |
| Pause the SLA while waiting for the customer | UX / rules | CS-11 | DEC-CS11-03: statuses marked "clock paused" stop the resolution clock; the due date moves by the paused time |
| Automatic closure of resolved tickets after N days | UX / rules | CS-11 | DEC-CS11-04: `SUPPORT_AUTO_CLOSE_AFTER=P7D` |
| Customer satisfaction rating and dashboard score | UX | CS-11 | DEC-CS11-05: rating 1–5 once per resolved/closed ticket, dashboard and report score |
| Canned replies ("Réponse type") | UX | CS-11 | managed by supervisors (Replies and messages), inserted in the reply composer |
| CSV export of tickets | UX | CS-11 | `GET /tickets/export.csv` with the queue filters (managers, administrators), spreadsheet-safe |
| Help-centre articles and search on the help page | UX | CS-11 | search of the public user guide (`/api/assistant/search`) and useful articles on the help home |
| Adding categories from the back-office | Rules | CS-11 | BR-006 amended (DEC-CS11-06): administrators add categories, with an English name |
| Actions on other modules from the ticket (refund, re-schedule delivery, contact driver) | Integration | CS-11 | DEC-CS11-07: requests sent through Bakend, which applies its own rules and audits them; the outcome is recorded on the ticket |
| Automatic classification, automatic replies, AI assistance | Intelligence | CS-11 | DEC-CS11-08: category/priority suggestion and reply drafts, always decided by an agent; public assistant with the user guide only |
| Transactional outbox for domain events | Reliability | CS-11 | DEC-CS11-01: `outbox_events`, delivered after commit, retried by a poller |
| Enforce module boundaries | Maintainability | CS-11 | hexagonal modules checked by `ArchitectureTest` (ArchUnit) on every build |
| Configurable vocabularies (statuses, priorities, transitions, SLA policies, permissions) | Reusability | CS-11 | DEC-CS11-02: tables + administration screens, lifecycle and safety rules checked |
| Observability: structured logs with correlation id, metrics, health dashboards | Observability | CS-11 | see the rate-limit line; Grafana in the `monitoring` profile |
| Message templates per language for notifications | UX / i18n | CS-11 | DEC-CS11-09: `notification_templates` FR/EN, each person's language (`PUT /me/preferences`) |

## Backlog of candidate upgrades (not approved)

| Candidate | Category | Source | Note |
| --- | --- | --- | --- |
| Foreground push banner in the Flutter app (`flutter_local_notifications`) | UX | Flutter app TODO (16/10/2026) | needs a test on a real Android/iOS device |
| Distributed tracing (OpenTelemetry) and a central log store (Loki) | Observability | CS-11 | logs are already JSON with a request id; add when several instances run |
| Ready-made Grafana dashboards | Observability | CS-11 | Prometheus and alert rules are provisioned, dashboards are built by hand for now |
| Separate admin roles (e.g. an admin who cannot change permissions) | Security | Bakend CLAUDE.md §11 | one `ADMINISTRATOR` role for now |
| E-mail notifications (templates already per language) | UX | reference architecture §4.3 | push and in-app only today |
