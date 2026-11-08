# Agents IA

Les agents de code IA (Claude Code, GitHub Copilot, Cursor, Codex et similaires) doivent travailler dans ce dépôt
comme un nouveau développeur : lire d'abord le contexte écrit, rester dans le périmètre approuvé et laisser le
référentiel à jour. Cette page présente les fichiers d'instructions et la façon de reproduire le projet à l'identique.

## Fichiers d'instructions

| Fichier | Rôle |
| --- | --- |
| `AGENTS.md` (racine du dépôt) | **instructions de référence** pour tout agent : contexte du projet, ordre de lecture, règles, commandes |
| `backend/AGENTS.md`, `frontend/AGENTS.md`, `assistant/AGENTS.md`, `documentation/AGENTS.md` | règles propres à chaque partie, à lire quand on travaille dans ce dossier |
| fichiers `CLAUDE.md` | contiennent `@AGENTS.md` : Claude Code importe ainsi les mêmes instructions au lieu d'une copie divergente |
| `.github/copilot-instructions.md`, `.github/instructions/mermaid.instructions.md` | instructions de GitHub Copilot (conventions des diagrammes Mermaid) |

Modifiez `AGENTS.md`, jamais une copie : les outils qui lisent un autre nom de fichier l'importent ou y renvoient.

## Ordre de lecture

1. `AGENTS.md` (celui de la racine, puis celui du dossier où vous travaillez).
2. [Project continuity](/developer/en/project/PROJECT-CONTINUITY/) — phase actuelle, décisions en attente, prochaine étape.
3. [Development matrix](/developer/en/project/DEVELOPMENT-MATRIX/) — élément actif et ses dépendances.
4. [Reference architecture](/developer/en/architecture/reference-architecture/) — styles et principes suivis par le code.
5. Puis les documents utiles à la tâche : [architecture decisions](/developer/en/architecture/decisions/), exigences,
   UX/UI, et ce guide du développeur.

## Règles pour les agents

- **Rester dans le périmètre approuvé.** Ne réaliser que l'élément actif de la matrice. Proposer les nouvelles idées
  dans l'[upgrade plan](/developer/en/governance/UPGRADE-PLAN/) au lieu de les réaliser. Ne jamais présenter un
  travail prévu comme fait.
- **Respecter l'architecture.** Règles métier côté serveur (`TicketAccessPolicy`, `ticket.domain`) ; systèmes
  externes derrière des ports ; effets de bord après commit ; changements de base sous forme de nouvelles migrations
  Flyway.
- **Garder la parité français/anglais.** Chaque texte d'interface dans les deux dictionnaires ; chaque page des
  guides utilisateur, personnel et développeur dans les deux langues avec le même nom de fichier ; le référentiel du
  projet reste en anglais.
- **Lancer les tests avec Docker** — aucun outil local n'est supposé : `./scripts/test.sh` (ou `.\scripts\test.ps1`)
  et `python3 documentation/build.py --strict` pour la documentation. Indiquer les commandes lancées et leur résultat.
- **Mettre à jour le référentiel.** Matrice, point de continuité, changelog, traçabilité, documents
  d'architecture/API/base de données et diagrammes, comme le liste la [définition de « terminé »](contributing.md).
- **Configurer via `.env`.** Un nouveau paramètre de la pile Docker est une variable documentée dans `.env.example`
  et utilisée sous la forme `${VAR:?…}` dans les fichiers Compose ; ne jamais écrire en dur un port, un identifiant,
  un modèle ou une limite dans `docker-compose*.yml`.
- **Borner chaque cache et garder des images petites.** Un cache a une taille maximale et une durée de vie, est vidé
  après le commit et ne contient jamais de données de tickets côté serveur ni ne mélange les données de plusieurs
  utilisateurs ([Cache et espace disque](caching.md)). Les images sont multi-étapes, sur une base légère et fixée
  déjà utilisée par le projet, avec des cache mounts BuildKit et un `.dockerignore`.
- **Utiliser les fichiers de verrouillage.** Les dépendances Python se gèrent avec uv : modifier `pyproject.toml`,
  lancer `uv lock`, committer `uv.lock` (à la racine pour la documentation, dans `assistant/` pour l'assistant) ; il
  n'y a pas de `requirements.txt`.
- **Vérifier avec les commandes documentées** — [Tests](testing.md) les liste avec et sans Docker, en PowerShell et
  en Bash, y compris le test de fumée d'une pile démarrée.
- **Ne jamais** committer de secrets ni de fichiers `.env`, activer le profil `demo` hors usage local, modifier une
  migration appliquée, ni exposer publiquement les sites de documentation internes.

## Reproduire le projet à l'identique

Chaque version importante est fixée dans le dépôt :

| Composant | Version | Fixée dans |
| --- | --- | --- |
| Java | 21 | `backend/pom.xml` (`java.version`), `backend/Dockerfile` (`eclipse-temurin:21-jdk-noble` → `21-jre-alpine`), `docker-compose.test.yml` |
| Spring Boot | 4.1.1 | `backend/pom.xml` (parent) |
| Maven | wrapper | `backend/mvnw`, `backend/.mvn/wrapper/maven-wrapper.properties` |
| springdoc / Firebase Admin / Testcontainers | 3.1.1 / 9.11.0 / 2.0.5 | `backend/pom.xml` |
| PostgreSQL | 16 | `docker-compose.yml`, `docker-compose.test.yml`, `IntegrationTest` (`postgres:16-alpine`) |
| Node.js | 22 | `frontend/Dockerfile` et `docker-compose.test.yml` (`node:22-alpine`), CI |
| Next.js / React | 16.4.3 / 19.3.0 | `frontend/package.json`, `frontend/package-lock.json` |
| Playwright | 1.56.0 | `frontend/package.json`, image de `docker-compose.test.yml` |
| Python | 3.12 | `.python-version`, `assistant/.python-version`, `assistant/Dockerfile` et `documentation/Dockerfile` (`python:3.12-slim-bookworm`) |
| uv | 0.13.0 | `assistant/Dockerfile`, `documentation/Dockerfile` (`ghcr.io/astral-sh/uv:0.13.0`) |
| Zensical | 0.0.69 | `pyproject.toml`, `uv.lock` |
| Dépendances de l'assistant | starlette 0.48.0, uvicorn 0.37.0, httpx 0.28.1, pytest 8.4.2 | `assistant/pyproject.toml`, `assistant/uv.lock` |
| nginx | 1.29 | `documentation/Dockerfile` (`nginx:1.29-alpine`) |
| Modèles Ollama | Ollama cloud : `gpt-oss:20b` (`.env.example`) ; Ollama local : `qwen2.5:3b`, `nomic-embed-text` (défauts de `assistant/app/config.py`) | `.env.example`, `assistant/app/config.py` |

Installez à partir des fichiers de verrouillage : `npm ci --legacy-peer-deps` (jamais `npm install`), `./mvnw` (jamais
la version de Maven du système), `uv sync` à la racine et dans `assistant/` (utilise `uv.lock` et
`assistant/uv.lock` ; les images lancent `uv sync --locked`, qui échoue si un fichier de verrouillage n'est pas à
jour). La seule image non fixée est l'image Ollama (`OLLAMA_IMAGE=ollama/ollama:latest` dans `.env.example`) ;
fixez-la dans `.env` pour des déploiements identiques.

Depuis un clone propre, ces commandes reproduisent la pile, les tests et la documentation :

```bash
cp .env.example .env                                 # Windows: copy .env.example .env
docker compose up -d --build --wait                  # app + API with demo data
docker compose --profile docs up -d --build --wait   # + documentation and assistant
./scripts/test.sh all                                # backend, frontend, assistant, docs, e2e
./scripts/smoke-test.sh                              # the running stack answers end to end
python3 documentation/build.py --strict              # all documentation sites
```

## Rendre le travail

Terminez une session comme le [Reproduction guide](/developer/en/governance/REPRODUCTION/) le demande à tout
développeur : tests lancés et rapportés, documentation dans les deux langues, matrice et point de continuité à jour,
questions ouvertes consignées, et prochaine étape sans ambiguïté.
