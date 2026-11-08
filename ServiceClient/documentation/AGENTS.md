# AGENTS.md — documentation sites (Zensical)

Read the root `AGENTS.md` first. Full guide: `docs/guide/documentation.md`.

## Layout

| Site | French | English | Audience |
| --- | --- | --- | --- |
| user | `documentation/user/fr/` | `documentation/user/en/` | public |
| staff | `documentation/staff/fr/` | `documentation/staff/en/` | internal (support agents, managers, admins) |
| developer | `documentation/developer/fr/` | `zensical.toml` + `docs/` (repo root) | internal |

`shared/` holds the brand stylesheet, the language-switch script and the assistant widget; `build.py` copies them
into each `docs/assets/maplenou/` (git-ignored), writes `config.js`, builds every site into `site/<site>/<lang>/`
and a portal `site/index.html`. `Dockerfile` (uv builder → `nginx:alpine`, pre-gzipped files) and `nginx.conf.template` serve them; nginx proxies
`/api/assistant/` to the `assistant` service (built from `assistant/Dockerfile`). `DOCS_SITES` in `.env` chooses
which sites are built into the image (public deployment: `user` only).

## Rules

- Same file names in FR and EN; one `# Title` per page; descriptive, self-contained `##` sections
  (they are the search entries and the assistant's references).
- Name UI elements with the app's exact label in that language: `<span class="ui">Créer un ticket</span>`
  (labels: `frontend/src/lib/i18n/messages/*.ts`).
- Facts come from `docs/requirements` and the code; never invent a rule, delay or permission.
- The user guide is public: no internal notes, SLA internals, admin or staff procedures in it.
- Project records in `docs/` stay English-only; the French developer site links to them (`records.md`).
- Cache rules (`nginx.conf.template`): fingerprinted theme assets 1 year `immutable`, our shared assets `no-cache`,
  pages `no-cache`, `/api/assistant/` `no-store`. Do not add long caching to files whose name does not change.
- Screenshots: `documentation/<site>/<lang>/docs/images/*.jpg` come from `frontend/scripts/guide-screenshots.mjs`
  (numbered circles + a numbered legend in the page); re-run it after a UI change and keep the legend in step.
- Dependencies: root `pyproject.toml` + `uv.lock` (`zensical==0.0.69` only).
- Before committing: `uv sync --locked && uv run python documentation/build.py --strict`. Preview one site:
  `uv run python documentation/build.py --prepare-only` then `uv run zensical serve -f <its zensical.toml>`.
