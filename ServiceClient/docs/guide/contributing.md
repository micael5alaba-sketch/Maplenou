# Contributing

This page condenses `CONTRIBUTING.md`, the [development matrix](../project/DEVELOPMENT-MATRIX.md) and the governance
records into a working checklist. The rule behind all of it: approved work is never changed silently, and every
change leaves the repository reproducible.

## Before you start

Read, in this order: `README.md`, [Project continuity](../project/PROJECT-CONTINUITY.md),
[Development matrix](../project/DEVELOPMENT-MATRIX.md), [References](../governance/REFERENCES.md) and the README of the
part you will change (`backend/README.md`, `frontend/README.md`, `frontend/I18N.md`). Do not create a new plan when
an existing one already covers the task.

## Development workflow

1. Select an existing matrix item and confirm its status and dependencies.
2. Implement only the approved scope.
3. Add or update tests.
4. Update the documentation when behaviour or architecture changes.
5. Update the development matrix, and the continuity checkpoint when the current work changes.
6. Commit with a Conventional Commit.
7. Open a pull request against `main`.

`CONTRIBUTING.md` does not prescribe a branch naming scheme. Work on a topic branch, never directly on `main`: the CI
workflows run on pull requests and on pushes to `main`, and the documentation is deployed from `main`.

## Commit messages

Use [Conventional Commits](https://www.conventionalcommits.org) with a scope:

```text
feat(scope): add ticket assignment
fix(scope): reject invalid ticket transition
docs(scope): document ticket lifecycle
test(scope): add ticket lifecycle tests
refactor(scope): reorganize support application layer
chore(scope): update CI configuration
```

## Pull request description

GitHub fills the description from `.github/PULL_REQUEST_TEMPLATE.md` (summary and definition-of-done checklist);
reviewers are requested from `.github/CODEOWNERS` (placeholder `@OWNER` until Maplenou names its team). Bugs and
change requests are opened with the forms of `.github/ISSUE_TEMPLATE/`. A pull request explains:

- what changed and why;
- which matrix item it implements;
- the tests executed;
- the documentation updated;
- any architectural or business-rule impact.

## Definition of done

A change is done when every applicable box is ticked:

- [ ] Tests added or updated, and the suites pass: `./scripts/test.sh` (or the relevant suite; see [Testing](testing.md)).
- [ ] User-visible strings exist in **French and English** (`defineMessages` makes `npm run typecheck` fail otherwise).
- [ ] New API error codes have an English text in `frontend/src/lib/i18n/messages/errors.ts`.
- [ ] Documentation updated **in both languages** with the same file names: user and staff guides for visible
      behaviour, the developer guide (`docs/guide/` and `documentation/developer/fr/docs/guide/`) for code changes.
- [ ] Project records updated where relevant (English only): requirements traceability, architecture, database
      model, API specification, regenerated `openapi.json`, UML/Mermaid diagrams.
- [ ] `python documentation/build.py --strict` passes (at least for the sites you touched).
- [ ] Development matrix, continuity checkpoint and `CHANGELOG.md` (`[Unreleased]`, Keep a Changelog) updated.
- [ ] No secret, `.env` file, build output or generated asset committed.
- [ ] Configuration of the Docker stack comes from `.env`: a new setting is a variable documented in `.env.example`
      and used as `${VAR:?…}` in the Compose files — never a value hard-coded in `docker-compose*.yml`.
- [ ] Every new cache is bounded (maximum size and lifetime), is invalidated after commit and never mixes users'
      data — see [Caching and disk space](caching.md).
- [ ] Docker images stay small: multi-stage build, pinned slim base (reuse an image the project already uses),
      BuildKit cache mounts, `.dockerignore` up to date; check the size with `docker images` and `docker system df`.

At the matrix level, an item is `DONE` only when implementation, validation, diagrams, CI, continuity updates **and
project-owner approval** are complete. Status vocabulary: `PLANNED`, `IN_PROGRESS`, `BLOCKED`, `DONE`, `DEFERRED`,
`CANCELLED`.

## Change control

- **New ideas** (product or architecture) are proposed before implementation: record them in
  [Upgrade plan](../governance/UPGRADE-PLAN.md) (problem, benefit, scope, dependencies, risks, testing and
  documentation impact, effort, deadline impact). An upgrade enters the matrix only after explicit approval.
- **Approved items** (CS-01, CS-02 and the approved decisions DEC-001 … DEC-007) are not modified silently; a needed
  change becomes a change request.
- **Decisions awaiting approval** are listed in [Architecture decisions](../architecture/decisions.md) and the
  continuity checkpoint; do not build on an interpretation the owner has not confirmed without saying so.
- **Database changes** are new Flyway migrations; a migration applied anywhere is never edited.
- **Releases** follow `RELEASE.md`: Semantic Versioning, changelog, green CI, security review of sensitive changes,
  a Git tag. A release describes only what was implemented.

## Security reports and formatting

Security problems are reported privately, as described in `SECURITY.md` — never in a public issue. `.editorconfig`
sets the formatting for every editor (UTF-8, LF line endings — CRLF for PowerShell and batch scripts —, final newline, 2 spaces; 4 for Java, Python and PowerShell).
`LICENSE` is a proprietary-licence placeholder that Maplenou must confirm.

## Handoff

Before handing work over (to a person or an agent): clean working tree, matrix and continuity checkpoint updated,
blockers and unfinished work documented, important decisions recorded, and the next item unambiguous — as required
by the [Reproduction guide](../governance/REPRODUCTION.md).
