# Maplenou Customer Service Backend

Spring Boot 4.1 (Java 21) REST API for the Customer Service and Ticket Management module.

## Responsibilities

- Ticket management and lifecycle
- Assignment and escalation
- Messages and internal notes
- Attachments
- Ticket history/audit
- SLA on a business calendar
- Authorization
- Integration with Maplenou services (orders, payments, deliveries, sellers, refunds)
- Notifications (in-app + Firebase Cloud Messaging)

## Architecture

The backend follows the application/domain/infrastructure separation documented in
[`../docs/architecture/`](../docs/architecture/README.md) — start with `README.md` and `decisions.md`.
The API contract is [`../docs/architecture/openapi.json`](../docs/architecture/openapi.json); the database schema is
`src/main/resources/db/migration/` (Flyway).

## Run locally

Requirements: Java 21 and PostgreSQL 16 (or use `docker compose up postgres` from the repository root).

```bash
# create the user and database once (defaults in application.yml)
psql -U postgres -c "CREATE USER maplenou WITH PASSWORD 'maplenou'" -c "CREATE DATABASE maplenou_support OWNER maplenou"

# demo profile: demo login, sample orders/deliveries, seeded tickets
SPRING_PROFILES_ACTIVE=demo ./mvnw spring-boot:run      # Windows: mvnw.cmd spring-boot:run
```

API: <http://localhost:8081/api/v1/support> · Swagger UI: <http://localhost:8081/swagger-ui.html> ·
health: <http://localhost:8081/actuator/health>.

## Configuration (environment variables)

| Variable | Default | Purpose |
| --- | --- | --- |
| `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` | local `maplenou_support` | PostgreSQL |
| `JWT_SECRET` | empty (demo profile sets one) | HS256 secret for local/demo tokens. **Leave empty in production** and set `spring.security.oauth2.resourceserver.jwt.issuer-uri` |
| `JWT_ISSUER` | `maplenou-support-dev` | Expected issuer |
| `DEV_LOGIN_ENABLED` | `false` (`true` in demo) | Demo login endpoints — never in production |
| `ATTACHMENTS_DIR` | `./data/attachments` | Private attachment storage |
| `FIREBASE_CREDENTIALS_FILE` | empty | Firebase service account; empty = push logged only |
| `ECOSYSTEM_MODE` | `demo` | `demo` (sample data) or `http` (core services) |
| `ECOSYSTEM_BASE_URL`, `ECOSYSTEM_SERVICE_TOKEN` | | Core services when `ECOSYSTEM_MODE=http` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3001` | Allowed browser origins for direct API calls |

## Tests

```bash
./mvnw verify                 # unit tests + integration tests (*IT) with Testcontainers PostgreSQL
TEST_DATABASE_URL=jdbc:postgresql://localhost:5432/maplenou_test ./mvnw verify   # use a local database instead of Docker
```

The suite covers unit, integration, API, authorization, ticket lifecycle, attachments, SLA, notifications,
ecosystem context and the roadmap scenarios (53 unit + 62 integration tests).
