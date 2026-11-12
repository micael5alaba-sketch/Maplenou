# Development Matrix

| ID | Period | Work package | Dependency | Status | Continuation |
| --- | --- | --- | --- | --- | --- |
| CS-01 | Sep W1 | Requirements and business rules | None | **DONE** | Closed |
| CS-02 | Sep W2 | UX/UI | CS-01 | **DONE** | Closed |
| CS-03 | Sep W3 | Database/API foundation | CS-01 | **IN_PROGRESS** | Validated 2026-10-08 — [awaiting owner approval](../validation/CS-03-VALIDATION.md) |
| CS-04 | Sep W4 | Frontend foundation | CS-02 | **IN_PROGRESS** | Validated 2026-10-08 — [awaiting owner approval](../validation/CS-04-VALIDATION.md) |
| CS-05 | Oct W1 | Ticket backend | CS-03 | **IN_PROGRESS** | Validated 2026-10-08 — [awaiting owner approval](../validation/CS-05-VALIDATION.md) |
| CS-06 | Oct W2 | Frontend/backend integration | CS-04, CS-05 | **IN_PROGRESS** | Validated 2026-10-08 — [awaiting owner approval](../validation/CS-06-VALIDATION.md) |
| CS-07 | Oct W3 | Ecosystem integration | CS-05 | **IN_PROGRESS** | Validated with demo adapter — [awaiting owner approval + core-team contract](../validation/CS-07-VALIDATION.md) |
| CS-08 | Oct W4 | Notifications and testing | CS-06, CS-07 | **IN_PROGRESS** | Validated (simulated FCM) — [awaiting owner approval](../validation/CS-08-VALIDATION.md) |
| CS-09 | End Oct | Security and final validation | CS-08 | **IN_PROGRESS** | Validated for internal demo — [awaiting owner approval](../validation/CS-09-FINAL-VALIDATION.md) |
| CS-10 | Oct (owner request) | Languages FR/EN and themes, three documentation sites (Zensical) with Ollama assistant, design deliverables (Claude Design canvas), AI-agent files, Docker-only workflow; iteration 3: slim images, `.env`-only Compose, bounded caching, helper scripts | CS-04, CS-09 | **IN_PROGRESS** | Validated 2026-10-09 — [awaiting owner approval](../validation/CS-10-VALIDATION.md) |
| CS-12 | Oct (owner request) | Render deployment (Blueprint, guards, Firebase), CI on `ci/**` branches, checks with/without Docker, coexistence with the former support module (sync, read-only, exact old-number search), annotated guide screenshots, all screens in the design canvas, complexity review, English category descriptions, translated permission names | CS-11 | **IN_PROGRESS** | Validated 2026-10-31 — [awaiting owner approval](../validation/CS-12-VALIDATION.md) |
| CS-11 | Oct (owner request) | Whole UPGRADE-PLAN backlog: configurable vocabulary ("domains"), hexagonal modules + ArchUnit, outbox, SLA pause, auto-close, satisfaction, canned replies, CSV export, help search, categories from the back-office, actions on Maplenou through Bakend, AI help + public assistant, templates per language, rate limits, logs, metrics, alerts, antivirus, Cloudinary, public profile with HTTPS | CS-10 | **IN_PROGRESS** | Validated 2026-10-31 — [awaiting owner approval](../validation/CS-11-VALIDATION.md) |

## Status vocabulary

`PLANNED`, `IN_PROGRESS`, `BLOCKED`, `DONE`, `DEFERRED`, `CANCELLED`

## Definition of Done

A matrix item is `DONE` only when its required implementation or documentation, validation, relevant diagrams, CI validation where applicable, continuity updates, and project approval are complete.

CS-03 … CS-11 meet every criterion except **project approval** (and, for CI, a first green run on GitHub — all
suites pass locally). They therefore stay `IN_PROGRESS` until the owner approves them.

## Current checkpoint

Implementation of CS-03 → CS-09 delivered on 2026-10-08. Next step: owner review and approval — see
[PROJECT-CONTINUITY](PROJECT-CONTINUITY.md).
