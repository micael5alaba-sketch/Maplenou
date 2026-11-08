# CS-12 Validation — Render, CI branches, coexistence with the former support, guides and mockups

**Work package:** CS-12 (requested by the project owner on 2026-10-31)
**Result:** PASSED — technically validated, awaiting project-owner approval (GitHub run and Render creation need the
owner's accounts, see "Known limits")
**Date:** 2026-10-31

## Scope requested

1. Finish the Firebase part and put the app on Render.
2. Screenshots with circled, numbered parts in the user guide, and in the staff guide where useful.
3. Checks with and without Docker for developers.
4. Update the mockups to every screen since the start.
5. Check the complexity of the algorithms and optimise them.
6. Run the app at the end.
7. Work on a branch, push it, read the GitHub Actions and fix what fails before merging to `main`.
8. Avoid problems between this work and the program the owner cloned (and Bakend's former support module).

Owner answers (2026-10-31): Render scope = customer service + help centre (Bakend and `Site_vitrine` stay where they
are); the owner adds the repository access for the push; mockups in the same Claude Design canvas; check both
Bakend's former support module and the merge with `main`.

## What was delivered

| Item | Delivered | Decision |
| --- | --- | --- |
| Render Blueprint, `postgresql://` database URL, public-deployment guard, configurable docs upstream, Firebase secret file and web values | `render.yaml`, `DatabaseUrlEnvironmentPostProcessor`, `PublicDeploymentGuard`, `nginx.conf.template`, [Render guide](../guide/render.md) | DEC-CS12-03 |
| Workflows on `ci/**` branches and by hand; local equivalent | `.github/workflows/*.yml`, `scripts/check.sh`, `scripts/check.ps1`, [Repository](../guide/repository.md#branches-and-checks), [Testing](../guide/testing.md#check-with-or-without-docker) | DEC-CS12-01 |
| Coexistence with the former support module | V6 `legacy_ticket_sync`, `LegacySyncScheduler`, `LEGACY_UPDATED`, Bakend `SUPPORT_LEGACY_READ_ONLY`, exact old-number search | DEC-CS12-02 |
| Annotated screenshots | 32 pictures in the user and staff guides (FR/EN), `frontend/scripts/guide-screenshots.mjs` | DEC-CS12-04 |
| Mockups | design canvas: 13 screens, 163 artboards, components `RequesterScreens` and `SupportScreens` | DEC-CS12-05 |
| Complexity review | [Complexity review](../architecture/complexity-review.md) | DEC-CS12-06 |

## Evidence

| Check | Result |
| --- | --- |
| Backend `mvn verify` against PostgreSQL 16 | **102 unit + 87 integration tests passed** (new: `DatabaseUrlEnvironmentPostProcessorTest`, `PublicDeploymentGuardTest`; `MaplenouIntegrationIT` now covers the status change noted once and the exact search by a 6-digit old number; `CachingIT` checks the English category descriptions) |
| Bakend `SupportTicketServiceTest` | passed (the old admin page answers 409 when `SUPPORT_LEGACY_READ_ONLY=true`) |
| Frontend lint, typecheck (FR/EN key parity), unit tests, production build | passed — **45 tests** |
| Browser end-to-end against the running stack | **7 passed, 1 skipped** (mobile-only test on desktop) |
| Postman collection with newman | **40 requests, 59 checks, 0 failures** |
| `scripts/smoke-test.sh` against the running stack (API, web app, docs + assistant through the new nginx template) | passed; internal AI endpoints answer 404 through the docs server |
| Assistant `pytest` | **35 passed** (new `test_bm25.py`: the inverted index gives exactly the textbook scores) |
| `documentation/build.py --strict` | passed (six sites, with the screenshots) |
| Backend started with `DATABASE_URL=postgresql://user:pass@host:5432/db` | migrated V1→V6 and served the demo |
| `nginx -t` on the rendered template, Compose values and Render values | valid (found and fixed the unquoted `{8}` regular expression) |
| `render.yaml` | valid YAML; fields checked against the Render Blueprint reference |
| Merge with the owner's clone | the branch starts from `origin/main` (1862e21): fast-forward; the owner's uncommitted `frontend/web`, `frontend/windows`, `Bakend/start-all.ps1` and untracked `Mobile_frontend/` are not touched by the branch |

## Problems found and fixed during validation

- Bakend's former tickets are numbered `TKT-YYYY-NNNNNN` (6 digits), not `SUP-…` as the documentation said; a
  complete old number was searched only among new numbers and never found → exact match on both.
- `documentation/nginx.conf` did not load (`{8}` in an unquoted regular expression) → quoted, now a template.
- `Bakend/mvnw` lacked the executable bit in Git → the Bakend workflow would fail with "Permission denied".
- Workflows ran only on `main` and pull requests → also `ci/**` and by hand.
- Running every workflow's commands on a clean checkout (`scripts/check.sh --native` … `ci`): `Site_vitrine`'s
  `npm ci` refused its lock file (picomatch placement) → the site workflow would have failed at its first step;
  lock file resynchronised (same versions). All other suites passed; Flutter skipped (no SDK here).
- English screens showed French category descriptions → V6 `description_en`.
- « Droits » showed raw action codes; « Statuts et priorités » showed French labels in English → translated.

## Known limits

- **GitHub push and Actions:** the session's Git proxy still refuses `micael5alaba-sketch/Maplenou` (403) and the
  computer's Linux workspace has no GitHub credentials. The branch is ready as a Git bundle; the owner pushes it
  (commands in the final report) or adds the repository to the session.
- **Render:** creating the Blueprint needs the owner's Render account and the secrets (Bakend's public URL,
  `SUPPORT_SERVICE_TOKEN`, `CLOUDINARY_URL`, `OLLAMA_API_KEY`, the VAPID key, the Firebase service-account file).
- Docker Hub, GHCR and other registries are not reachable from the build environment: images were not built here;
  the Dockerfiles changed only in the docs image (template + entrypoint script), checked with a local nginx.
- `scripts/check.ps1` was not run here (no PowerShell in the build environment); `check.sh` was.
- Flutter was not compiled here (no SDK): the `frontend-ci.yml` result is to be read on GitHub.

## Decisions

DEC-CS12-01 … DEC-CS12-06 in [decisions.md](../architecture/decisions.md).
