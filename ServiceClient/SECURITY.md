# Security policy

## Supported versions

| Version | Supported |
| --- | --- |
| `main` (0.x, MVP candidate) | yes |
| older tags | no |

## Reporting a vulnerability

Please **do not open a public issue** for a security problem.

1. Send a private report to the project owner (security contact: _to be defined by Maplenou — see the
   owner checklist in `docs/project/PROJECT-CONTINUITY.md`_), or use GitHub's
   "Report a vulnerability" (private security advisory) on the repository.
2. Describe the problem, the affected component (`backend`, `frontend`, `assistant`, `documentation`), the steps
   to reproduce and the impact. Never include real customer data.
3. You will receive an acknowledgement within 5 business days. Fixes are released as soon as possible and
   credited unless you prefer otherwise.

## Scope and known limits of the MVP

The security review is in `docs/architecture/security.md`. Before any production use:

- disable the `demo` Spring profile (demo login and seed data) and set a strong `JWT_SECRET`, or better, verify
  tokens from the Maplenou identity provider (`issuer-uri`);
- change every password in `.env` and keep `.env` out of version control (it is git-ignored);
- publish only the **user** documentation publicly (`DOCS_SITES=user`, `ASSISTANT_SITES=user`);
- put the stack behind HTTPS and a reverse proxy; do not publish PostgreSQL or Ollama ports.

## Dependencies

Versions are pinned (`backend/pom.xml` + Maven wrapper, `frontend/package-lock.json`, `uv.lock`,
`assistant/uv.lock`). Upgrades follow `docs/governance/UPGRADE-PLAN.md`.
