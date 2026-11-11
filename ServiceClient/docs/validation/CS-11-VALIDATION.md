# CS-11 Validation — Upgrade plan delivered, configurable domains, hexagonal modules

**Work package:** CS-11 (requested by the project owner on 2026-10-30)
**Result:** PASSED — technically validated, awaiting project-owner approval
**Date:** 2026-10-31

## Scope requested

1. Deliver every item of the [UPGRADE-PLAN](../governance/UPGRADE-PLAN.md) backlog, including AI help and the
   post-production problems.
2. Solve the "domains" problem (a ticketing core configurable for any domain, and public domain names) and the
   "hexagonal" problem properly.
3. Remove `REFERENCES-CS-02.md` if it is no longer needed.

Owner answers (2026-10-30): AI inside tickets **and** a public assistant online; pause the resolution clock while
waiting for the customer; automatic closure after 7 days; actions on other modules as requests through Bakend.

## What was delivered

| Item | Delivered | Decision |
| --- | --- | --- |
| Hexagonal modules (domain / application + ports / adapters), boundaries checked on every build | `ArchitectureTest` (15 checks) | DEC-CS11-01 |
| Transactional outbox for domain events | `outbox_events`, `OutboxRelay`, metrics | DEC-CS11-01 |
| Configurable vocabulary: statuses with a kind, transitions, priorities, SLA policies, permission matrix, generic context references | V4, « Statuts et priorités », « Droits », « Calendrier SLA » | DEC-CS11-02 |
| SLA pause while waiting for the requester | `sla_paused_since`, state PAUSED, due date moved | DEC-CS11-03 |
| Automatic closure of resolved tickets after 7 days | `SUPPORT_AUTO_CLOSE_AFTER=P7D` | DEC-CS11-04 |
| Customer satisfaction, dashboard and report score | `ticket_satisfaction`, action RATE | DEC-CS11-05 |
| Categories added by administrators (BR-006 amended), canned replies, CSV export, help-centre search | back-office screens, `export.csv`, `/api/assistant/search` | DEC-CS11-06 |
| Actions on Maplenou: return request, delivery release, message to the courier, push relay | Bakend `SupportBridgeActionService` + `HttpEcosystemActions` | DEC-CS11-07 |
| AI help (suggestion, reply drafts) and public assistant | `/api/assistant/internal/*`, `public` profile | DEC-CS11-08 |
| Notification templates per language, each person's language, Flutter app opens the ticket | V5, `PUT /me/preferences`, push `url` | DEC-CS11-09 |
| Rate limits, request id, JSON logs, Prometheus metrics, alert rules, Grafana, ClamAV, Cloudinary authenticated storage, Caddy with HTTPS on three domains, ports on 127.0.0.1 | `platform.observability`, `antivirus`/`monitoring`/`public` profiles | DEC-CS11-10 |
| `REFERENCES-CS-02.md` removed | merged into [REFERENCES](../governance/REFERENCES.md) | — |

## Evidence

| Check | Result |
| --- | --- |
| Backend `mvn verify` against PostgreSQL 16 | **96 unit + 87 integration tests passed** (incl. `ArchitectureTest`, `SupportFeaturesIT`, `OperationsIT`, `BakendContractTest`, `CloudinaryStorageTest` with the documented Cloudinary signature vector, `ClamAvScannerTest` with a fake clamd) |
| Upgrade of an existing database | iteration-5 backend (commit 63e4816) seeded a database at V3; the new backend migrated it to V5: 5 tickets kept, references moved to `ticket_context_refs` (ORDER 4, PAYMENT 4, DELIVERY 4, SELLER 4, REFUND 1), no double seeding |
| Bakend `mvn test` | **90 tests passed** (new `OrderLifecycleIntegrationTest` #14: delivery release, return request through the business rules, refusals, message to the courier, push relay, audit). One pre-existing test (`AuthAndUserIntegrationTest`) failed once on a random phone-number collision and passed when re-run |
| Frontend lint, typecheck (FR/EN key parity), unit tests, production build | passed — **42 tests** |
| Browser end-to-end against the running stack (demo profile) | **7 passed, 1 skipped** (mobile-only test on desktop); new scenario: canned reply inserted, CSV download, administration screens |
| Postman collection with newman | **40 requests, 59 checks, 0 failures** (new folder 7: vocabulary, Maplenou action, SLA pause, satisfaction, CSV) |
| `scripts/smoke-test.sh` | passed (vocabulary and satisfaction checks added; internal AI endpoints checked as not exposed when the docs run) |
| Assistant `pytest` | **33 passed** (search, token, lists enforced, untrusted text, user guide only) |
| `docker compose config` with every profile (`docs`, `antivirus`, `monitoring`, `public`) and the test file | valid |
| `documentation/build.py --strict` | passed (six sites) |
| `openapi.json` regenerated from `/v3/api-docs` | 59 paths, version 0.2.0 |

## Problems found and fixed during validation

- `priority_rank` stayed `SMALLINT` after V4 while the entity maps an `INTEGER` → widened in V4.
- An AI suggestion read outside a transaction hit a lazy collection → the persistence adapter is transactional.
- The frontend sorted priorities as "rank 1 = most urgent" while the backend uses "higher = more urgent"; the
  assistant also received the list in the wrong order → both corrected.
- Seeded priority labels "Faible"/"Normale" differed from the CS-02 reference vocabulary → "Basse"/"Moyenne".
- Product condition `USED` did not exist in Bakend (`NEW`/`GOOD`/`DAMAGED`) → aligned.
- Status/priority filters with an unknown code returned an empty list → `400 INVALID_FILTER`.
- The rate limiter trusted a client-supplied `X-Forwarded-For` → it now relies on the servlet's remote address,
  which Tomcat fills from the header only behind a trusted proxy (`FORWARD_HEADERS_STRATEGY=native`).
- `SESSION_COOKIE_SECURE` was not passed by Compose → passed, and required by the `public` profile.

## Known limits

- Docker Hub was not reachable from the build environment: the images of the new profiles (`clamav/clamav:1.4.3`,
  `prom/prometheus:v3.5.0`, `grafana/grafana:12.1.1`, `caddy:2.10.0-alpine`) are pinned but were not pulled here —
  run the profiles once on your computer or server.
- The Flutter change (open the ticket from a support push) was not compiled here (no Flutter SDK); it uses
  `url_launcher`, already a dependency of the app. Test it on a phone (see [Firebase](../guide/firebase.md)).
- A real Firebase project, a real ClamAV and a real Cloudinary account were replaced by local fakes in the tests.
- HTTPS certificates need real domains pointing to a server; see [Deployment](../guide/deployment.md).

## Decisions

DEC-CS11-01 … DEC-CS11-10 in [decisions.md](../architecture/decisions.md).
