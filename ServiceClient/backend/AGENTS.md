# AGENTS.md — backend (Spring Boot API)

Read the root `AGENTS.md` first.

## Layout (`src/main/java/com/maplenou/support`) — hexagonal modules (DEC-CS11-01)

| Module | `domain` (plain Java) | `application` (use cases + `port`) | `adapter` |
| --- | --- | --- | --- |
| `ticket` | `Ticket` aggregate, `vocabulary` (StatusKind, Workflow, Priorities), `PermissionMatrix`, `TicketAccessPolicy`, `event` | `TicketCommandService`, `TicketQueryService`, `VocabularyService`, `CategoryService`, `AutoCloseService`, `AiAssistService`, `ExternalActionService`, `CannedReplyService`, `TicketStatistics` | `in.web` controllers, `in.events.AiTriageListener`, `in.scheduler.AutoCloseScheduler`; `out.persistence` (JPA), `out.assistant` |
| `sla` | `BusinessCalendar`, `SlaPolicy`, `SlaState`, `SlaPeriod` | `SlaService`, `SlaPeriodService`, `SlaAdminService` | `in.web.SlaAdminController`, `out.persistence` |
| `notification` | `Notification`, `MessageTemplate` | `NotificationService`, `PushDispatcher`, `TemplateService` | `in.events`, `in.scheduler`, `in.web`; `out.push` (Firebase/log), `out.maplenou.BakendPushRelay`, `out.persistence` |
| `attachment` | `AttachmentPolicy` | `AttachmentService` (ports `BinaryStorage`, `MalwareScanner`) | `out.storage` (local/Cloudinary), `out.scan` (none/ClamAV) |
| `context` | `Ecosystem` summaries | `EcosystemService`, `ReferenceService` (ports `EcosystemGateway`, `EcosystemActions`) | `out.maplenou` (Bakend HTTP), `in.web` |
| `identity` | `SupportUser`, `MaplenouRoles` | `UserDirectory`, `StaffRoleService`, `CoreLoginService` | `in.security.UserSyncFilter`, `in.web`, `out.maplenou`, `out.security` (JWT), `out.persistence` |
| `report` | — | uses `ticket.application.TicketStatistics` | `in.web.DashboardController` |

`shared` = kernel (`Actor`, `Role`, `SupportException`, `PageQuery/PageResult`, `event`). `platform` = technical base
(config properties, security chains, error handler, outbox, rate limit, request id, metrics access). `demo` = demo
profile only (personas, seed data, demo ecosystem). Each module has one `adapter/<Module>Config` that builds its
adapters. **`ArchitectureTest` enforces the rules below on every build.**

## Rules

- Request flow: `adapter.in.web` controller → `application` service (transaction) → `TicketAccessPolicy` →
  `domain` → `port` → `adapter.out.persistence`. Never put a business rule in a controller or an adapter.
- Layer rules (ArchUnit): `domain` uses no framework and no other layer; `application` uses only
  `org.springframework.stereotype` / `transaction` (no JPA, web, HTTP, security); `adapter.in` never uses
  `adapter.out`; a module's adapters are private to it; modules depend on each other's `application`/`domain`
  only, without cycles; `@Entity` only in `adapter.out.persistence` or `platform`.
- Vocabulary is data (DEC-CS11-02): never compare a status code to decide a rule — use its `StatusKind`
  (`Workflow.kind(code)`); priorities by `rank` (higher = more urgent); permissions through `PermissionMatrix`.
- Throw `SupportException` with a stable `code` (UPPER_SNAKE) and the rule id; messages are French end-user text.
  A new code must also get an English text in `frontend/src/lib/i18n/messages/errors.ts`.
- Side effects run from domain events published through the **transactional outbox** (`DomainEventPublisher` →
  `outbox_events` → `OutboxRelay` after commit, then `@EventListener`s in a new transaction). Listeners must be
  idempotent-safe: an event can be delivered again after a crash.
- New table/column → new Flyway migration `V<n>__description.sql`; never edit an applied one. Update
  `docs/architecture/database-model.md`.
- API change → update `docs/architecture/api-specification.md` and regenerate `docs/architecture/openapi.json`
  from `GET /v3/api-docs` (demo profile), formatted with 2-space indentation.
- Caching: reference data only (categories, vocabulary, SLA calendar and policies, notification templates), as one
  in-memory snapshot per service with the TTL `REFERENCE_CACHE_TTL`, dropped **after commit** of a change
  (`TransactionSynchronization.afterCommit`) so a rolled-back transaction never empties or poisons it. Reference
  endpoints return `ETag` + `Cache-Control: private`. Never cache ticket data or anything per user.
- Manual API walkthrough: `http/support-api.http` and `http/postman/` (keep both in step with the API; run the
  collection with `npx newman run` before committing an API change); processing flows: `docs/guide/backend-processing.md`.
- Push: `notification/adapter/NotificationConfig` picks `FirebasePushSender` when `FIREBASE_CREDENTIALS_FILE` is
  readable (fails fast otherwise), else `LoggingPushSender`; requesters' pushes also go through `BakendPushRelay`
  when `MAPLENOU_PUSH_RELAY=true`. Push texts stay generic (ticket number, no message content); data carries
  `type`, `ticketId`, `source=support` and `url`. See `docs/guide/firebase.md`.
- Calls to Bakend (`context/adapter/out/maplenou`) use `/internal/v1/**` with `X-Maplenou-Service-Token`; keep the
  JSON field names in step with `Bakend/.../supportbridge/SupportBridgeDtos` (`BakendContractTest`).
- Dockerfile: Maven build with a `/root/.m2` cache mount, layered jar extracted, `eclipse-temurin:21-jre-alpine`
  runtime, non-root, health check on `/actuator/health`.
- Notifications are rendered from `notification_templates` in the recipient's language (V5) and keep
  language-neutral facts (`ticketNumber`, `detail`).

## Test

`./mvnw verify` — unit tests (`*Test`) and integration tests (`*IT`, failsafe) against PostgreSQL:
`TEST_DATABASE_URL=jdbc:postgresql://localhost:5432/maplenou_test` or Testcontainers. Without Java:
`scripts/test.sh backend`.
