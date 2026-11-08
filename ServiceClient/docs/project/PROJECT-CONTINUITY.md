# Project Continuity Checkpoint

**Current project:** Maplenou Customer Service — Ticket Management MVP
**Current phase:** CS-03 → CS-11 — implemented and technically validated
**Status:** **AWAITING PROJECT-OWNER APPROVAL** · CS-11 (2026-10-30/31): the whole UPGRADE-PLAN backlog, configurable
vocabulary ("domains"), hexagonal modules checked by ArchUnit, AI help and public assistant, production profiles —
owner decisions DEC-CS11-01 … DEC-CS11-10 ([validation](../validation/CS-11-VALIDATION.md))
**Last update:** 2026-10-31

## What exists now

| Area | Location | State |
| --- | --- | --- |
| Project location | Maplenou repository, folder `ServiceClient/` (next to `Bakend/`, `frontend/`, `Admin_frontend/maplenou/`, `Site_vitrine/`) | moved in iteration 5 (DEC-CS10-17); own PostgreSQL database; ports 3001 / 8081 / 8006 |
| Maplenou platform integration (sign-in bridge, support roles by phone, internal API, legacy import) | `backend/.../identity/`, `LegacyImportService`, V3; Bakend `supportbridge`; [guide](../guide/maplenou-integration.md) | implemented; demo mode stays the default in `.env.example`; Flutter / admin back-office not yet pointed to the new service |
| Requirements and UX baseline (CS-01, CS-02) | `docs/requirements/`, `docs/ux-ui/` | Approved |
| Architecture, decisions, database, API, integration, notifications, security | `docs/architecture/` | Proposed — needs approval |
| Backend (Spring Boot 4.1, Java 21, PostgreSQL 16, Flyway V1–V6), hexagonal modules, outbox, configurable vocabulary | `backend/` | 102 unit + 87 integration tests passing (CS-12), `ArchitectureTest` on every build |
| Frontend (Next.js 16, React 19, TypeScript), FR/EN, light/dark/system | `frontend/` | 45 unit tests + 7 browser scenarios passing |
| Documentation assistant (Python, Ollama cloud by default), help search, AI help for agents | `assistant/` | 35 tests passing |
| Production profiles (`antivirus`, `monitoring`, `public` with Caddy/HTTPS) | `docker-compose.yml`, `deploy/`, `monitoring/`; [deployment guide](../guide/deployment.md) | configuration validated; images to pull on the target machine; Render Blueprint `render.yaml` ([guide](../guide/render.md)) |
| Documentation: user / staff / developer × FR / EN (Zensical) | `documentation/`, `docs/`, `zensical.toml` | six sites build in strict mode |
| Design deliverables | Claude Design canvas (13 screens, EN/FR × light/dark × desktop/tablet/mobile), `/design`, `docs/ux-ui/screens.md`; annotated screenshots in the user and staff guides | delivered |
| AI-agent instructions | `AGENTS.md`, `CLAUDE.md` (root + per folder) | delivered |
| Local full stack | `docker-compose.yml` (+ `docs` profile), values in `.env` | demo profile; Docker is the only requirement |
| Helper scripts | `scripts/` (`test`, `smoke-test`, `clean`, `backup` — `.sh` and `.ps1`) | delivered |
| CI | root `.github/workflows/*.yml` of the Maplenou repository, also on `ci/**` branches and by hand; `scripts/check.sh` locally | not yet run on GitHub (push access pending) |
| Phase validation reports | `docs/validation/CS-03…CS-12` | PASSED, approval pending |

## Approved decisions

DEC-001 … DEC-007 (CS-01) — unchanged.

## Decisions awaiting approval

DEC-CS03-01 … DEC-CS03-18, DEC-CS10-01 … DEC-CS10-19 and DEC-CS11-01 … DEC-CS11-10 in
[decisions.md](../architecture/decisions.md). DEC-CS11-03, 04, 07 and 08 record the owner's answers of 2026-10-30.
DEC-CS10-16 … DEC-CS10-19 are the owner's own decisions of 2026-10-29 (SLA 24/7, Maplenou integration, Ollama cloud,
`Site_vitrine/`). Three are interpretations that need an explicit answer from the owner:

1. **DEC-CS03-05** — one "business day" in resolution targets = 10 business hours (08:00–18:00). *Superseded on
   2026-10-29 by DEC-CS10-16 (24/7, one day = 24 hours); still relevant only if `SLA_MODE=business-hours` is used.*
2. **DEC-CS03-08** — a Support Agent may triage *unassigned* tickets (priority, category, note, escalation) before taking them.
3. **DEC-CS03-16** — scope of the basic manager report.

## Continuation rule

1. The owner reviews the validation reports CS-03 … CS-11 and the decisions above.
2. For each approved package, set it to `DONE` in the [development matrix](DEVELOPMENT-MATRIX.md) and update the
   `PROJECT-PLAN`. A rejected item becomes a change request; do not silently modify approved behaviour.
3. Push the repository and confirm the five GitHub Actions workflows are green.

New features go to the remaining backlog of the [UPGRADE-PLAN](../governance/UPGRADE-PLAN.md) first.

## Open items before a pilot (not blockers for the internal demo)

- Switch production to Maplenou sign-in (`MAPLENOU_LOGIN_ENABLED=true`, `AUTH_MODE=maplenou`,
  `SPRING_PROFILES_ACTIVE=default`, random `JWT_SECRET`) and list the ServiceClient backend in Bakend's
  `app.trusted-proxy-ips` (or raise `AUTH_RATE_LIMIT_PER_MINUTE`).
- Set `SUPPORT_SERVICE_TOKEN` on both sides, switch `ECOSYSTEM_MODE=http`, block `/internal/` in Bakend's reverse
  proxy; give the support roles and run the import of the former tickets.
- Create an Ollama cloud key (`OLLAMA_API_KEY`) for the assistant, or configure a local Ollama.
- Create the Firebase project, put the key in `secrets/` and the web values in `.env` (docs/guide/firebase.md); test push in the browser, then on a phone with the Maplenou app through the Bakend relay (`MAPLENOU_PUSH_RELAY=true`).
- For a public deployment: domains, `public` profile and the settings of [Deployment](../guide/deployment.md);
  `METRICS_TOKEN` + `monitoring` profile; `ANTIVIRUS=clamav` + `antivirus` profile; decide `ATTACHMENT_STORAGE`.
- Turn on AI help when wanted: `AI_ASSIST_ENABLED=true` and `ASSISTANT_INTERNAL_TOKEN`.
- Load the official public-holiday calendar in *Calendrier SLA* — only needed if `SLA_MODE=business-hours` is used
  again (no effect in 24/7 mode).

## Last completed checkpoint

CS-03 → CS-09 implementation and technical validation (2026-10-08); CS-10 owner-requested enhancements (2026-10-09); CS-10 iteration 3 — design canvas, smaller Docker images, `.env`-only Compose, bounded caching, helper scripts (2026-10-09); iteration 4 — Firebase push (backend + browser), Postman/Swagger, Ollama on the computer or in Docker, docs on port 8006, doctor script (2026-10-14); iteration 5 — SLA 24/7, Ollama cloud by default, ports 3001/8081,
Maplenou integration (sign-in bridge, support roles, internal API, legacy import), `Site_vitrine/` recorded (2026-10-29);
CS-11 — every UPGRADE-PLAN item, configurable vocabulary, hexagonal modules, AI help, production profiles (2026-10-31);
CS-12 — Render Blueprint, CI on branches, checks with/without Docker, coexistence with the former support module,
annotated guide screenshots, every screen in the canvas, complexity review (2026-10-31).

## Next continuation point

Push the `ci/service-client-cs12` branch, read the GitHub Actions, merge to `main`, create the Render Blueprint.
Owner approval of CS-03 → CS-12, then pilot preparation (open items above). Still proposed (not done): a « Mes
tickets » screen inside the Flutter app and a link from the admin back-office; retire the core's former support
module only after the owner approves.

## Continuity sequence

```text
CS-01 Requirements                       DONE
        ↓
CS-02 UX/UI                              DONE
        ↓
CS-03 Database + Backend/API Foundation  validated — approval pending
        ↓
CS-04 Frontend Foundation                validated — approval pending
        ↓
CS-05 Ticket Backend                     validated — approval pending
        ↓
CS-06 Frontend/Backend Integration       validated — approval pending
        ↓
CS-07 Ecosystem Integration              validated (demo adapter) — approval pending
        ↓
CS-08 Notifications + Testing            validated (simulated FCM) — approval pending
        ↓
CS-09 Security + Final Validation        validated for internal demo — approval pending
        ↓
CS-10 Languages, themes, docs, assistant validated — approval pending
```

Continue from the first package that is not yet approved. Do not reopen completed phases without a validated change request.
