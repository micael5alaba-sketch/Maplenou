# Documentation sites

The documentation is part of the product: one site per audience, each in French and English, built with
[Zensical](https://zensical.org) 0.0.69 (Material-for-MkDocs-compatible Markdown). Each site feeds the search
index read by the [documentation assistant](assistant.md).

## Three audiences, two languages

| Site | Audience | Published | French | English |
| --- | --- | --- | --- | --- |
| `user` | customers, sellers, delivery drivers | **public** | `documentation/user/fr/zensical.toml` | `documentation/user/en/zensical.toml` |
| `staff` | Maplenou support agents, managers, administrators | internal | `documentation/staff/fr/zensical.toml` | `documentation/staff/en/zensical.toml` |
| `developer` | developers and AI coding agents | internal | `documentation/developer/fr/zensical.toml` | `zensical.toml` (repository root, `docs_dir = "docs"`) |

The English developer site lives at the repository root so it can use `docs/`, where the project records are kept.
The project records (requirements, UX/UI, architecture, validation, governance, project) are maintained **in English
only**; the French developer site translates the guide and links to the English records from its
« Référentiel du projet (anglais) » page.

Built sites are published under `/<site>/<lang>/` (for example `/user/fr/`, `/developer/en/`), next to a portal
page `/index.html` that links to every site and language.

## Folder layout

```text
documentation/
├── build.py                    prepare + build every site, write the portal page
├── Dockerfile                  docs image: builds the sites (strict), serves them with nginx
├── nginx.conf.template         static files, cache headers, /api/assistant/ proxy
├── serve.py                    local preview of ./site without Docker (plays the role of nginx)
├── shared/                     copied into each site as docs/assets/maplenou/
│   ├── stylesheets/maplenou.css      brand colours, audience ribbon, .ui labels
│   ├── javascripts/language-switch.js keeps the reader on the same page when switching language
│   └── assistant/assistant.{js,css}  "Ask a question" widget
├── user/{fr,en}/               zensical.toml + docs/*.md
├── staff/{fr,en}/              zensical.toml + docs/*.md
└── developer/fr/               zensical.toml + docs/index.md, docs/guide/*.md, docs/records.md
zensical.toml                   English developer site → docs/
docs/                           index.md, guide/*.md and the project records
```

Every `zensical.toml` declares the same theme features, the three-way palette (system, light, dark, matching the
web app), the Markdown extensions (admonitions, tabs, task lists, Mermaid through `pymdownx.superfences`…) and
`extra.alternate` for the language selector. The navigation of each site is the `nav` list of its `zensical.toml`.

## build.py

`documentation/build.py` runs two steps for each selected site:

1. **prepare** — deletes and re-creates `<docs_dir>/assets/maplenou/` from `documentation/shared/`, then writes
   `assets/maplenou/config.js` with `window.MAPLENOU_DOCS = {site, lang, endpoint}` (read by the assistant widget)
   and the audience label shown in the header. These folders are generated and git-ignored.
2. **build** — runs `python -m zensical build --clean -f <zensical.toml>` (plus `--strict` when asked) and copies
   the result to `<out>/<site>/<lang>/`. Finally it writes `<out>/index.html`.

| Option | Default | Effect |
| --- | --- | --- |
| `--sites` | `$DOCS_SITES` or `user,staff,developer` | which audiences to build |
| `--langs` | `fr,en` | which languages to build |
| `--out` | `./site` | output folder |
| `--endpoint` | `$ASSISTANT_ENDPOINT` or `/api/assistant` | assistant URL as seen from the browser |
| `--strict` | off | abort on warnings (missing page, missing anchor…) — used in CI |
| `--prepare-only` | off | only copy the shared assets (before `zensical serve`) |

```bash
python documentation/build.py                               # every site
python documentation/build.py --sites developer --strict    # what you run before a docs commit
python documentation/build.py --sites user                  # public build (user guide only)
```

The build ends with `Built <n> site(s) into <out>`.

## Preview while editing

Prepare the shared assets once, then serve one site with live reload:

=== "Bash"

    ```bash
    uv sync --locked                                     # zensical 0.0.69 from uv.lock
    uv run python documentation/build.py --prepare-only
    uv run zensical serve -f zensical.toml -a localhost:8007        # English developer site
    uv run zensical serve -f documentation/user/fr/zensical.toml -a localhost:8008
    ```

=== "PowerShell"

    ```powershell
    uv sync
    uv run python documentation/build.py --prepare-only
    uv run zensical serve -f zensical.toml -a localhost:8007
    uv run zensical serve -f documentation/user/fr/zensical.toml -a localhost:8008
    ```

`zensical serve` uses `localhost:8000` by default, a port that other applications often use, and the `docs`
container uses `8006`: the commands above therefore pass `-a localhost:8007` / `8008`. The assistant widget needs the assistant API; in a plain preview it reports that the
assistant is unavailable, which is expected.

### Zensical Studio

Zensical Studio is an editor extension (VS Code, Cursor, Zed, JetBrains IDEs) that activates when the workspace
contains a `zensical.toml` and offers a live preview and navigation of the project. As of 2026 it is a public beta
distributed under a commercial EULA: check its licence terms before using it for this project. It is optional —
`zensical serve` gives the same preview.

## Writing rules

- **Same file names in French and English.** `language-switch.js` maps `/user/fr/x/` to `/user/en/x/`; a missing
  counterpart sends the reader to the other language's home page. Add, rename or delete pages in both languages in
  the same change and keep both `nav` lists in the same order.
- **One `# Title` per page**, then descriptive `##` sections.
- **Self-contained `##` sections.** Each section is a search entry and an assistant passage on its own: name the
  subject in the heading ("Add an attachment", not "Next step"), and do not rely on the previous section to make
  sense. Keep sections focused (the assistant reads about the first 1,200 characters of a section).
- **UI labels** are written as `<span class="ui">Créer un ticket</span>`, exactly as the interface shows them in that
  language.
- **Links between pages** are relative `.md` links (`[Testing](testing.md)`); they are checked by `--strict`. Links
  from the French developer site to the English records are absolute (`/developer/en/requirements/business-rules/`).
- **Admonitions** (`!!! note`, `!!! warning`), **tabs** (`=== "Bash"` / `=== "PowerShell"`), tables and Mermaid
  diagrams (```` ```mermaid ````) are available on every site.
- Describe only what the code does; never document planned behaviour as available.
- Run `python documentation/build.py --sites <site> --strict` before committing.

## Public and internal publishing

Only the **user** guide is public. The staff and developer sites describe internal procedures and code, and must
only be served on internal networks.

| Setting | Public instance | Internal instance |
| --- | --- | --- |
| Sites built | `DOCS_SITES=user` (or `--sites user`) | `user,staff,developer` |
| Assistant | `ASSISTANT_SITES=user` | `user,staff,developer` |

The assistant enforces its scope: a question about a site that is not in `ASSISTANT_SITES` gets `404 UNKNOWN_SITE`,
without revealing which sites exist. The `docs` service of `docker-compose.yml` builds the image from
`documentation/Dockerfile` (context: repository root, `DOCS_SITES` from `.env`) and serves it on port 80 of the
container. See [Operations](operations.md).

### The docs image (documentation/Dockerfile)

`documentation/Dockerfile` only builds and serves the sites; its build context is the repository root, reduced by
the allow-list `.dockerignore` at the root (`pyproject.toml`, `uv.lock`, `zensical.toml`, `docs/`, `documentation/`).

| Stage | Base | What it does |
| --- | --- | --- |
| `builder` | `python:3.12-slim-bookworm` + uv 0.13.0 | `uv sync --locked --no-dev` (Zensical 0.0.69 from the root `uv.lock`, uv cache as a BuildKit cache mount), then `build.py --sites "$DOCS_SITES" --endpoint "$ASSISTANT_ENDPOINT" --out /out --strict`, then gzips every HTML, CSS, JS, JSON, SVG and XML file larger than 1 KB (`.gz` next to the original) |
| `runtime` | `nginx:1.29-alpine` | only `documentation/nginx.conf.template` and the generated files; `HEALTHCHECK` on `/healthz` |

Build arguments: `DOCS_SITES` (default `user,staff,developer`; Compose passes `DOCS_SITES` from `.env`) and
`ASSISTANT_ENDPOINT` (default `/api/assistant`). A broken link fails the image build, so
`scripts/test.sh docs` (`docker compose --profile docs build docs`) is the strict documentation check with Docker.

```bash
docker build -f documentation/Dockerfile -t maplenou-docs .                              # all six sites
docker build -f documentation/Dockerfile --build-arg DOCS_SITES=user -t maplenou-docs .  # public build
```

The sites are built only here: the assistant has no copy of them and reads their search indexes from this server
over HTTP (`ASSISTANT_INDEX_URL=http://docs`, see [Documentation assistant](assistant.md)).

`documentation/nginx.conf.template`:

- forwards `/api/assistant/` to `http://assistant:8000` with streaming (`proxy_buffering off`), `X-Real-IP`, a 16 KB
  body limit and `Cache-Control: no-store`; the assistant's name is resolved per request through Docker's DNS
  (`resolver 127.0.0.11`), so nginx starts even when the assistant is not running;
- serves the pre-compressed files (`gzip_static on`) and compresses the others;
- sets cache headers (see [Caching and disk space](caching.md)):

| Files | `Cache-Control` |
| --- | --- |
| theme JavaScript and CSS with a content hash in their name (`*.<hash>.min.js`, `*.<hash>.min.css`) | `public, max-age=31536000, immutable` |
| our shared assets `assets/maplenou/` (assistant widget, `config.js`) | `no-cache` (revalidated, so a new build is seen at once) |
| other images, fonts, CSS and JS | `public, max-age=86400` (one day) |
| pages, `search.json` indexes and the portal | `no-cache` (revalidated with `ETag` / `Last-Modified`, cheap `304`) |

### Local preview of the built sites without Docker

`documentation/serve.py` plays the role of nginx: it serves `./site` and forwards `/api/assistant/*` to an assistant
(streaming kept). It needs Starlette, uvicorn and httpx, which the assistant's environment provides. Run each
command from the repository root, in its own terminal:

=== "Bash"

    ```bash
    uv sync && uv run python documentation/build.py                     # builds every site into ./site
    (cd assistant && uv sync && ASSISTANT_INDEX_ROOT=../site uv run uvicorn --factory app.main:create_app --port 8001)
    uv run --project assistant python documentation/serve.py --port 8006 --assistant http://localhost:8001
    ```

=== "PowerShell"

    ```powershell
    uv sync; uv run python documentation/build.py
    cd assistant; uv sync; $env:ASSISTANT_INDEX_ROOT="../site"; uv run uvicorn --factory app.main:create_app --port 8001
    uv run --project assistant python documentation/serve.py --port 8006 --assistant http://localhost:8001
    ```

Without `ASSISTANT_INDEX_URL`, the assistant reads the indexes from `ASSISTANT_INDEX_ROOT`. Without a local Ollama
(`OLLAMA_URL`, default `http://localhost:11434`) it answers in retrieval-only mode.

## Screenshots with numbered marks

The user and staff guides show annotated screenshots of the app (CS-12): each picture circles the parts the page
talks about, with a numbered badge, and a numbered legend under it (in the page, so it is translated and found by the
search and the assistant). They live next to the pages: `documentation/<site>/<lang>/docs/images/<name>.jpg`, and
are taken by one script so they can be redone after any UI change:

```bash
docker compose up -d --build --wait          # demo stack (personas and sample tickets); FIREBASE_WEB_* set in .env
cd frontend && npx playwright install chromium
node scripts/guide-screenshots.mjs           # every picture, French and English (~2 minutes)
node scripts/guide-screenshots.mjs --only queue,new-ticket --base http://localhost:3001
```

- The list of pictures is the `SHOTS` table of `frontend/scripts/guide-screenshots.mjs`: persona, phone or desktop
  size, the screen, and the marks in legend order (Playwright locators by role and label in both languages).
- Phone size for the requester screens (412 px wide), desktop for the staff screens (1,280 px); light theme; JPEG
  quality 82 (about 3 MB for the 32 pictures).
- When a screen changes: run the script, check the pictures, and update the legend of the page if the marks changed.
  A missing mark stops that picture with `mark N not found`.
- Only demo data appears (never a real account).

## Migration from MkDocs

The project used MkDocs with Material for MkDocs. Zensical reads the same Markdown and the same extensions; the
configuration moved from `mkdocs.yml` (YAML) to `zensical.toml` (TOML, a `[project]` table). The root `zensical.toml`
replaces `mkdocs.yml` and keeps its `docs/` content and navigation. Pages need no change except links, which
`--strict` now checks, including anchors.

| MkDocs | Zensical |
| --- | --- |
| `mkdocs.yml` | `zensical.toml` (`[project]`, `[project.theme]`, `[project.markdown_extensions.*]`) |
| `pip install mkdocs-material` | `pip install zensical==0.0.69` or `uv sync` |
| `mkdocs serve` | `zensical serve -f <zensical.toml>` |
| `mkdocs build --strict` | `python documentation/build.py --strict` |
