# Contributing to Maplenou Customer Service

## Before starting

Read:

1. `README.md` and `AGENTS.md` (rules for humans and AI agents alike)
2. `docs/project/PROJECT-CONTINUITY.md`
3. `docs/project/DEVELOPMENT-MATRIX.md`
4. `docs/governance/REFERENCES.md`
5. The relevant backend/frontend README

Do not create a new plan when an existing plan already covers the task.

## Development workflow

1. Select an existing matrix item.
2. Confirm its status and dependencies.
3. Implement only the approved scope.
4. Add or update tests (`scripts/test.ps1 all` / `scripts/test.sh all` runs everything in Docker).
5. Update documentation when behavior or architecture changes — **in French and in English** (app strings in
   `frontend/src/lib/i18n/messages/`, documentation pages with the same file name in both languages).
6. Update the development matrix.
7. Update the continuity checkpoint when the current work changes.
8. Use a Conventional Commit.
9. Open a pull request.

## New ideas

A new product or architecture idea must be proposed before implementation. Record it in the appropriate planning/decision document and obtain project approval before adding it to the active development scope.

## Commits

Use Conventional Commits:

```text
feat(scope): add ticket assignment
fix(scope): reject invalid ticket transition
docs(scope): document ticket lifecycle
test(scope): add ticket lifecycle tests
refactor(scope): reorganize support application layer
chore(scope): update CI configuration
```

## Pull requests

Use the pull-request template (`.github/PULL_REQUEST_TEMPLATE.md`). A pull request should explain:

- What changed
- Why it changed
- Which matrix item it implements
- Tests executed
- Documentation updated
- Any architectural or business-rule impact

## Configuration, disk space and caching

- Configuration goes in `.env` (template: `.env.example`); never hard-code a value in `docker-compose*.yml` and
  never commit `.env`.
- Docker images are multi-stage, slim and use BuildKit cache mounts; do not add a large base image when an
  existing one fits (`docs/guide/caching.md`).
- Every cache must be bounded (size and lifetime) and must never mix users' data.
- `scripts/clean.ps1` / `scripts/clean.sh` frees disk space; `scripts/backup.*` backs up data with retention.

## Reproducibility

A contributor must be able to clone the repository, follow the documented setup, run the tests, build the application, and build the documentation without relying on undocumented local configuration.
