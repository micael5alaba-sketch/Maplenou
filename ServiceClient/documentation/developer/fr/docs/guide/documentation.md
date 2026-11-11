# Sites de documentation

La documentation fait partie du produit : un site par public, chacun en français et en anglais, construit avec
[Zensical](https://zensical.org) 0.0.69 (Markdown compatible Material for MkDocs). Chaque site alimente l'index de
recherche lu par l'[assistant de documentation](assistant.md).

## Trois publics, deux langues

| Site | Public | Publication | Français | Anglais |
| --- | --- | --- | --- | --- |
| `user` | clients, vendeurs, livreurs | **publique** | `documentation/user/fr/zensical.toml` | `documentation/user/en/zensical.toml` |
| `staff` | agents, responsables et administrateurs du support Maplenou | interne | `documentation/staff/fr/zensical.toml` | `documentation/staff/en/zensical.toml` |
| `developer` | développeurs et agents de code IA | interne | `documentation/developer/fr/zensical.toml` | `zensical.toml` (racine du dépôt, `docs_dir = "docs"`) |

Le site développeur anglais est à la racine du dépôt pour pouvoir utiliser `docs/`, où se trouve le référentiel du
projet. Ce référentiel (exigences, UX/UI, architecture, validation, gouvernance, projet) est tenu **en anglais
uniquement** ; le site développeur français traduit le guide et renvoie au référentiel anglais depuis sa page
« Référentiel du projet (anglais) ».

Les sites construits sont publiés sous `/<site>/<lang>/` (par exemple `/user/fr/`, `/developer/en/`), à côté d'une
page portail `/index.html` qui mène à chaque site et à chaque langue.

## Organisation des dossiers

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

Chaque `zensical.toml` déclare les mêmes fonctionnalités de thème, la palette à trois choix (système, clair, sombre,
comme l'application web), les extensions Markdown (admonitions, onglets, listes de tâches, Mermaid via
`pymdownx.superfences`…) et `extra.alternate` pour le sélecteur de langue. La navigation de chaque site est la liste
`nav` de son `zensical.toml`.

## build.py

`documentation/build.py` exécute deux étapes pour chaque site sélectionné :

1. **prepare** — supprime puis recrée `<docs_dir>/assets/maplenou/` à partir de `documentation/shared/`, puis écrit
   `assets/maplenou/config.js` avec `window.MAPLENOU_DOCS = {site, lang, endpoint}` (lu par le widget de
   l'assistant) et le libellé du public affiché dans l'en-tête. Ces dossiers sont générés et ignorés par Git.
2. **build** — exécute `python -m zensical build --clean -f <zensical.toml>` (plus `--strict` si demandé) et copie le
   résultat dans `<out>/<site>/<lang>/`. Il écrit enfin `<out>/index.html`.

| Option | Défaut | Effet |
| --- | --- | --- |
| `--sites` | `$DOCS_SITES` ou `user,staff,developer` | publics à construire |
| `--langs` | `fr,en` | langues à construire |
| `--out` | `./site` | dossier de sortie |
| `--endpoint` | `$ASSISTANT_ENDPOINT` ou `/api/assistant` | URL de l'assistant vue depuis le navigateur |
| `--strict` | désactivé | arrêt sur avertissement (page ou ancre manquante…) — utilisé en CI |
| `--prepare-only` | désactivé | copie seulement les ressources partagées (avant `zensical serve`) |

```bash
python documentation/build.py                               # every site
python documentation/build.py --sites developer --strict    # what you run before a docs commit
python documentation/build.py --sites user                  # public build (user guide only)
```

La construction se termine par `Built <n> site(s) into <out>`.

## Aperçu pendant la rédaction

Préparez une fois les ressources partagées, puis servez un site avec rechargement automatique :

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

`zensical serve` utilise `localhost:8000` par défaut, un port souvent pris par d'autres applications, et le
conteneur `docs` utilise `8006` : les commandes ci-dessus passent donc `-a localhost:8007` / `8008`. Le widget de l'assistant a besoin de l'API de l'assistant ; dans un simple aperçu il
indique que l'assistant est indisponible, ce qui est normal.

### Zensical Studio

Zensical Studio est une extension d'éditeur (VS Code, Cursor, Zed, IDE JetBrains) qui s'active lorsque l'espace de
travail contient un `zensical.toml` et offre un aperçu en direct et la navigation dans le projet. En 2026, c'est une
bêta publique distribuée sous un contrat de licence (EULA) commercial : vérifiez ses conditions de licence avant de
l'utiliser pour ce projet. Elle est facultative — `zensical serve` donne le même aperçu.

## Règles de rédaction

- **Mêmes noms de fichiers en français et en anglais.** `language-switch.js` fait correspondre `/user/fr/x/` à
  `/user/en/x/` ; si la page équivalente manque, le lecteur arrive sur l'accueil de l'autre langue. Ajoutez,
  renommez ou supprimez les pages dans les deux langues dans le même changement et gardez les deux listes `nav` dans
  le même ordre.
- **Un seul `# Titre` par page**, puis des sections `##` descriptives.
- **Des sections `##` autonomes.** Chaque section est à elle seule une entrée de recherche et un passage pour
  l'assistant : nommez le sujet dans le titre (« Ajouter une pièce jointe », pas « Étape suivante ») et ne comptez pas
  sur la section précédente pour être compris. Gardez des sections ciblées (l'assistant lit environ les 1 200 premiers
  caractères d'une section).
- **Les libellés d'interface** s'écrivent `<span class="ui">Créer un ticket</span>`, exactement comme l'interface les
  affiche dans cette langue.
- **Les liens entre pages** sont des liens `.md` relatifs (`[Tests](testing.md)`) ; `--strict` les vérifie. Les liens
  du site développeur français vers le référentiel anglais sont absolus (`/developer/en/requirements/business-rules/`).
- **Admonitions** (`!!! note`, `!!! warning`), **onglets** (`=== "Bash"` / `=== "PowerShell"`), tableaux et diagrammes
  Mermaid (```` ```mermaid ````) sont disponibles sur tous les sites.
- Ne décrivez que ce que fait le code ; ne présentez jamais un comportement prévu comme disponible.
- Lancez `python documentation/build.py --sites <site> --strict` avant de committer.

## Publication publique et interne

Seul le guide **utilisateur** est public. Les sites du personnel et des développeurs décrivent des procédures internes
et le code, et ne doivent être servis que sur des réseaux internes.

| Paramètre | Instance publique | Instance interne |
| --- | --- | --- |
| Sites construits | `DOCS_SITES=user` (ou `--sites user`) | `user,staff,developer` |
| Assistant | `ASSISTANT_SITES=user` | `user,staff,developer` |

L'assistant fait respecter son périmètre : une question sur un site absent de `ASSISTANT_SITES` reçoit
`404 UNKNOWN_SITE`, sans révéler quels sites existent. Le service `docs` de `docker-compose.yml` construit l'image à
partir de `documentation/Dockerfile` (contexte : racine du dépôt, `DOCS_SITES` lu dans `.env`) et la sert sur le
port 80 du conteneur. Voir [Exploitation](operations.md).

### L'image docs (documentation/Dockerfile)

`documentation/Dockerfile` ne fait que construire et servir les sites ; son contexte de build est la racine du dépôt,
réduite par la liste d'autorisation `.dockerignore` de la racine (`pyproject.toml`, `uv.lock`, `zensical.toml`,
`docs/`, `documentation/`).

| Étape | Base | Ce qu'elle fait |
| --- | --- | --- |
| `builder` | `python:3.12-slim-bookworm` + uv 0.13.0 | `uv sync --locked --no-dev` (Zensical 0.0.69 depuis le `uv.lock` racine, cache d'uv en cache mount BuildKit), puis `build.py --sites "$DOCS_SITES" --endpoint "$ASSISTANT_ENDPOINT" --out /out --strict`, puis compresse en gzip chaque fichier HTML, CSS, JS, JSON, SVG et XML de plus de 1 Ko (`.gz` à côté de l'original) |
| `runtime` | `nginx:1.29-alpine` | uniquement `documentation/nginx.conf.template` et les fichiers générés ; `HEALTHCHECK` sur `/healthz` |

Arguments de build : `DOCS_SITES` (par défaut `user,staff,developer` ; Compose transmet `DOCS_SITES` depuis `.env`) et
`ASSISTANT_ENDPOINT` (par défaut `/api/assistant`). Un lien cassé fait échouer la construction de l'image : avec
Docker, `scripts/test.sh docs` (`docker compose --profile docs build docs`) est donc le contrôle strict de la
documentation.

```bash
docker build -f documentation/Dockerfile -t maplenou-docs .                              # all six sites
docker build -f documentation/Dockerfile --build-arg DOCS_SITES=user -t maplenou-docs .  # public build
```

Les sites ne sont construits qu'ici : l'assistant n'en a pas de copie et lit leurs index de recherche sur ce serveur,
en HTTP (`ASSISTANT_INDEX_URL=http://docs`, voir [Assistant de documentation](assistant.md)).

`documentation/nginx.conf.template` :

- transmet `/api/assistant/` à `http://assistant:8000` avec le streaming (`proxy_buffering off`), `X-Real-IP`, une
  limite de corps de 16 Ko et `Cache-Control: no-store` ; le nom de l'assistant est résolu à chaque requête par le DNS
  de Docker (`resolver 127.0.0.11`), si bien que nginx démarre même lorsque l'assistant ne tourne pas ;
- sert les fichiers précompressés (`gzip_static on`) et compresse les autres ;
- définit les en-têtes de cache (voir [Cache et espace disque](caching.md)) :

| Fichiers | `Cache-Control` |
| --- | --- |
| JavaScript et CSS du thème avec une empreinte dans le nom (`*.<hash>.min.js`, `*.<hash>.min.css`) | `public, max-age=31536000, immutable` |
| nos ressources partagées `assets/maplenou/` (widget de l'assistant, `config.js`) | `no-cache` (revalidées : un nouveau build est vu tout de suite) |
| autres images, polices, CSS et JS | `public, max-age=86400` (un jour) |
| pages, index `search.json` et portail | `no-cache` (revalidés avec `ETag` / `Last-Modified`, `304` peu coûteux) |

### Aperçu local des sites construits sans Docker

`documentation/serve.py` joue le rôle de nginx : il sert `./site` et transmet `/api/assistant/*` à un assistant (en
gardant le streaming). Il a besoin de Starlette, uvicorn et httpx, que fournit l'environnement de l'assistant.
Lancez chaque commande depuis la racine du dépôt, chacune dans son terminal :

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

Sans `ASSISTANT_INDEX_URL`, l'assistant lit les index dans `ASSISTANT_INDEX_ROOT`. Sans Ollama local (`OLLAMA_URL`,
par défaut `http://localhost:11434`), il répond en mode « recherche seule ».

## Captures d'écran avec repères numérotés

Les guides utilisateur et du personnel montrent des captures annotées de l'application (CS-12) : chaque image entoure
les éléments dont la page parle, avec une pastille numérotée, et une légende numérotée en dessous (dans la page, donc
traduite et trouvée par la recherche et l'assistant). Elles sont à côté des pages :
`documentation/<site>/<langue>/docs/images/<nom>.jpg`, et sont prises par un seul script pour pouvoir être refaites
après chaque changement d'interface :

```bash
docker compose up -d --build --wait          # pile de démonstration (personas et tickets d'exemple) ; FIREBASE_WEB_* dans .env
cd frontend && npx playwright install chromium
node scripts/guide-screenshots.mjs           # toutes les images, en français et en anglais (~2 minutes)
node scripts/guide-screenshots.mjs --only queue,new-ticket --base http://localhost:3001
```

- La liste des images est le tableau `SHOTS` de `frontend/scripts/guide-screenshots.mjs` : persona, taille téléphone
  ou ordinateur, écran, et les repères dans l'ordre de la légende (localisateurs Playwright par rôle et libellé dans
  les deux langues).
- Taille téléphone pour les écrans des demandeurs (412 px de large), ordinateur pour ceux du personnel (1 280 px) ;
  thème clair ; JPEG qualité 82 (environ 3 Mo pour les 32 images).
- Quand un écran change : lancez le script, vérifiez les images et mettez à jour la légende de la page si les repères
  ont changé. Un repère introuvable arrête cette image avec `mark N not found`.
- Seules des données de démonstration apparaissent (jamais un vrai compte).

## Migration depuis MkDocs

Le projet utilisait MkDocs avec Material for MkDocs. Zensical lit le même Markdown et les mêmes extensions ; la
configuration est passée de `mkdocs.yml` (YAML) à `zensical.toml` (TOML, une table `[project]`). Le `zensical.toml`
racine remplace `mkdocs.yml` et conserve son contenu `docs/` et sa navigation. Les pages n'ont pas besoin de
changement, sauf les liens, que `--strict` vérifie désormais, ancres comprises.

| MkDocs | Zensical |
| --- | --- |
| `mkdocs.yml` | `zensical.toml` (`[project]`, `[project.theme]`, `[project.markdown_extensions.*]`) |
| `pip install mkdocs-material` | `pip install zensical==0.0.69` ou `uv sync` |
| `mkdocs serve` | `zensical serve -f <zensical.toml>` |
| `mkdocs build --strict` | `python documentation/build.py --strict` |
