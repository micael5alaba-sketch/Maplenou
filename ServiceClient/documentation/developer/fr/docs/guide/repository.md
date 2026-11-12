# Visite du dépôt

Un seul dépôt contient le backend, le frontend, les sites de documentation, l'assistant de documentation, le
référentiel du projet et la CI. Cette page décrit le contenu de chaque dossier et l'endroit où placer un nouveau
travail.

## Organisation générale

```text
.
├── AGENTS.md                      instructions for AI coding agents (CLAUDE.md imports it)
├── README.md · CONTRIBUTING.md · CHANGELOG.md · RELEASE.md
├── SECURITY.md                    how to report a vulnerability
├── LICENSE                        proprietary licence (placeholder, to be confirmed by Maplenou)
├── .editorconfig                  same formatting in every editor (UTF-8, LF, indentation)
├── .dockerignore                  allow-list build context of documentation/Dockerfile (repository root)
├── docker-compose.yml             local stack: postgres, backend, frontend (+ docs profile: docs, assistant; ollama profile: ollama)
├── docker-compose.test.yml        every test suite in containers
├── docker-compose.override.example.yml   publishes PostgreSQL on host port 5433 (copy to *.override.yml)
├── .env.example                   every variable of the Compose files, documented (copy to .env)
├── zensical.toml                  English developer documentation site (docs/)
├── pyproject.toml · uv.lock · .python-version   documentation toolchain (zensical 0.0.69)
├── .github/
│   ├── workflows/                 backend.yml, frontend.yml, assistant.yml, e2e.yml, docs.yml
│   ├── ISSUE_TEMPLATE/            bug_report.yml, change_request.yml, config.yml
│   ├── PULL_REQUEST_TEMPLATE.md
│   ├── CODEOWNERS                 automatic reviewers (placeholder @OWNER)
│   ├── copilot-instructions.md
│   └── instructions/mermaid.instructions.md
├── backend/                       Spring Boot 4.1 API (Java 21, PostgreSQL 16, Flyway)
├── frontend/                      Next.js 16 web app (React 19, TypeScript)
├── assistant/                     documentation assistant (Python 3.12, Starlette, Ollama, uv)
├── documentation/                 build script, shared assets and the user/staff/French developer sites
├── docs/                          English developer site: this guide + the project records
├── scripts/                       test, smoke-test, doctor, env-sync, clean, backup (.sh for macOS/Linux, .ps1 for Windows)
├── deploy/                        Caddyfile and guard of the public profile (domains, HTTPS)
├── monitoring/                    Prometheus configuration, alert rules, Grafana provisioning (monitoring profile)
└── secrets/                       secret files (Firebase key), git-ignored except README.md, mounted read-only into the backend
```

## Backend — `backend/`

| Chemin | Contenu |
| --- | --- |
| `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/` | build Maven (parent `spring-boot-starter-parent` 4.1.1) et wrapper |
| `Dockerfile`, `.dockerignore` | build multi-étapes : `eclipse-temurin:21-jdk-noble` (wrapper Maven, jar en couches) → `eclipse-temurin:21-jre-alpine`, utilisateur non root, contrôle de santé |
| `src/main/java/com/maplenou/support/` | code de l'application, un paquet par module (voir [Backend](backend.md)) |
| `src/main/resources/application.yml` | configuration, paramètres `maplenou.*` et profil `demo` |
| `src/main/java/.../<module>/domain`, `application`, `application/port`, `adapter/in/…`, `adapter/out/…` | chaque module (`ticket`, `sla`, `notification`, `attachment`, `context`, `identity`, `report`) en couches hexagonales ; `shared` (noyau), `platform` (socle technique), `demo` — voir [Backend](backend.md) |
| `ticket/application/LegacyImportService.java`, `ticket/adapter/in/web/LegacyImportController.java`, `context/adapter/out/maplenou/HttpLegacyTicketSource.java` | import unique des anciens tickets du support Maplenou depuis Bakend `/internal/v1/support-tickets` |
| `src/main/resources/db/migration/` | migrations Flyway `V1__support_schema.sql`, `V2__notification_i18n.sql`, `V3__maplenou_integration.sql` (colonnes des anciens tickets, `support_staff_assignments`), `V4__configurable_vocabulary.sql` (statuts, transitions, priorités, objectifs SLA, droits, références de contexte), `V5__outbox_feedback_ai.sql` (outbox, modèles par langue, satisfaction, réponses types, suggestions IA, actions externes) |
| `src/test/java/.../` | `ArchitectureTest` (règles ArchUnit de couches et de modules), tests unitaires (`*Test` : `ticket/domain/WorkflowTest`, `TicketAccessPolicyTest`, `sla/domain/*`, adaptateurs `CloudinaryStorageTest`, `ClamAvScannerTest`, `BakendContractTest`, `CsvWriterTest`) et tests d'intégration (`it/*IT`, classe de base `IntegrationTest` ; `SupportFeaturesIT`, `OperationsIT`, `MaplenouIntegrationIT` avec le faux Bakend `it/FakeMaplenouCore`) |
| `http/support-api.http` | collection de requêtes manuelles (REST Client de VS Code, client HTTP JetBrains) — voir [Traitements du backend](backend-processing.md) |
| `http/postman/` | collection Postman `Maplenou-Support-API.postman_collection.json` (40 requêtes, 8 dossiers, 59 vérifications) et environnement `Maplenou-local.postman_environment.json` — voir [Tester l'API](api-testing.md) |
| `README.md` | lancement du backend et variables d'environnement |

## Frontend — `frontend/`

| Chemin | Contenu |
| --- | --- |
| `src/app/` | routes App Router : `login`, `(requester)/aide…`, `support/…`, `api/` (gestionnaires de route du BFF, `api/push-config` pour les valeurs Firebase publiques, `api/auth-config` pour le mode de connexion `AUTH_MODE`) |
| `src/components/` | composants partagés (`ui.tsx`, coquilles de navigation, `TicketQueue`, `Conversation`, `TicketTools` — suggestion IA, actions Maplenou, satisfaction, réponses types —, `HelpSearch`, `Preferences`…) |
| `src/lib/` | client d'API (`api.ts`, `useApi.ts`, `apiCache.ts`), vocabulaire configurable (`vocabulary.tsx`), session, types, libellés, pièces jointes, push dans le navigateur (`push.ts`) |
| `public/push-sw.js` | service worker qui affiche les notifications push et ouvre le ticket au clic (aucun cache) — voir [Firebase](firebase.md) |
| `src/lib/i18n/` | dictionnaires FR/EN et utilitaires (`messages/<espace>.ts`) |
| `src/app/globals.css` | jetons de design (clair et sombre) et styles des composants |
| `e2e/` | scénario Playwright (`scenario.spec.ts`) et fichiers de test |
| `I18N.md` | règles des langues et des thèmes |
| `Dockerfile`, `.dockerignore`, `next.config.ts` | image multi-étapes `node:22-alpine` avec le serveur standalone, en-têtes de sécurité |
| `package.json`, `package-lock.json` | Next 16.4.3 / React 19.3.0 fixés, scripts `dev`, `build`, `lint`, `typecheck`, `test`, `e2e` |

## Assistant — `assistant/`

| Chemin | Contenu |
| --- | --- |
| `app/main.py` | API HTTP (`/api/assistant/health`, `/api/assistant/ask`), limitation de débit, validation, tâches de démarrage (index, téléchargement des modèles, embeddings) |
| `app/service.py` | recherche + génération, références, streaming, solutions de repli |
| `app/index.py`, `app/text.py` | lecture de `search.json`, BM25, similarité cosinus, fusion des classements, découpage en mots |
| `app/ollama.py` | client HTTP Ollama minimal (y compris le téléchargement des modèles) |
| `app/cache.py` | `TtlLru`, le cache borné des réponses |
| `app/prompts.py` | prompts système et textes fixes en français et en anglais |
| `app/config.py` | paramètres lus dans les variables d'environnement |
| `tests/` | suite pytest avec un faux Ollama (`test_cloud.py` : clé Ollama cloud, `think`, aucun téléchargement, aucun embedding) |
| `pyproject.toml`, `uv.lock`, `.python-version` | projet uv : dépendances fixées, groupe `dev` (pytest), réglages de pytest, Python 3.12 |
| `Dockerfile`, `.dockerignore` | étapes `builder`, `test` (lance pytest) et `runtime` |

## Documentation — `documentation/` et `docs/`

| Chemin | Contenu |
| --- | --- |
| `documentation/build.py` | prépare et construit les sites, écrit la page portail |
| `documentation/Dockerfile`, `documentation/nginx.conf.template` | image docs : construit les sites (en mode strict) et les sert avec nginx |
| `documentation/serve.py` | aperçu local des sites construits sans Docker (joue le rôle de nginx) |
| `documentation/shared/` | CSS, sélecteur de langue et widget de l'assistant copiés dans chaque site |
| `documentation/user/{fr,en}/` | guide utilisateur public (`zensical.toml` + `docs/`) |
| `documentation/staff/{fr,en}/` | guide interne du personnel du support |
| `documentation/developer/fr/` | guide du développeur en français ; renvoie vers le référentiel anglais |
| `docs/` (avec le `zensical.toml` racine) | guide du développeur en anglais (`docs/guide/`) et référentiel du projet |
| `docs/project/`, `docs/requirements/`, `docs/ux-ui/`, `docs/architecture/`, `docs/diagrams/`, `docs/validation/`, `docs/governance/` | référentiel du projet, tenu en anglais uniquement |

## Scripts et secrets — `scripts/`, `secrets/`

| Chemin | Contenu |
| --- | --- |
| `scripts/test.*` | toutes les suites de tests dans des conteneurs |
| `scripts/smoke-test.*` | vérification de bout en bout d'une pile démarrée (affiche aussi le canal push) |
| `scripts/doctor.ps1`, `scripts/doctor.sh` | diagnostic en lecture seule : Docker, `.env` comparé à `.env.example`, `docker compose config`, ports hôte, Ollama de l'ordinateur, conteneurs et leurs journaux, santé de l'assistant |
| `scripts/clean.*`, `scripts/backup.*` | libérer de l'espace disque ; sauvegarder et restaurer la base et les pièces jointes |
| `secrets/` | fichiers secrets montés en lecture seule sur `/run/secrets/maplenou` dans le backend (`firebase-service-account.json`) ; tout sauf `README.md` est ignoré par Git — voir [Firebase](firebase.md) |

## Fichiers générés et ignorés

Ils sont produits par les builds et listés dans `.gitignore` ; ne les committez pas et ne les modifiez pas à la main :
`site/`, `.cache/`, `docs/assets/maplenou/`, `documentation/*/*/docs/assets/maplenou/`, `documentation/*/*/site/`,
`frontend/node_modules/`, `frontend/.next/`, `backend/target/`, `.venv/`, `assistant/.venv/`, `.env`,
`docker-compose.override.yml`, `backups/`, `secrets/*` (sauf `secrets/README.md`). `scripts/clean.sh` (`scripts\clean.ps1`) supprime les résultats de build
parmi eux.

## Où placer les nouveautés

| Vous ajoutez… | Placez-le dans… |
| --- | --- |
| Une capacité métier du backend | un paquet sous `backend/src/main/java/com/maplenou/support/` (web → service → domaine → dépôt) |
| Des règles de ticket indépendantes du framework | `ticket/domain/` (avec un test unitaire simple) |
| Une modification de la base | une nouvelle `backend/src/main/resources/db/migration/V4__<description>.sql` — ne modifiez jamais V1, V2 ni V3 |
| Un système externe | une interface de port et des adaptateurs, comme `context/EcosystemGateway` ou `notification/PushSender` |
| Un écran web | `frontend/src/app/…/page.tsx`, ses textes dans `frontend/src/lib/i18n/messages/<espace>.ts` |
| Un élément d'interface réutilisable | `frontend/src/components/`, couleurs sous forme de jetons dans `globals.css` |
| De l'aide pour les utilisateurs | `documentation/user/fr/docs/` **et** `documentation/user/en/docs/` (mêmes noms de fichiers) |
| Des procédures pour le personnel du support | `documentation/staff/fr/docs/` et `documentation/staff/en/docs/` |
| Des pages du guide du développeur | `docs/guide/` (anglais) et `documentation/developer/fr/docs/guide/` (français) |
| Exigences, décisions, rapports de validation | le dossier correspondant sous `docs/` (anglais uniquement) |
| Une idée d'amélioration | d'abord `docs/governance/UPGRADE-PLAN.md` — voir [Contribuer](contributing.md) |
| Un job de CI | `.github/workflows/service-client-*.yml` à la racine du dépôt Maplenou (GitHub ne lit les workflows que là) |
| Une valeur de configuration de la pile Docker | une variable de `.env.example` utilisée sous la forme `${VAR:?…}` dans le fichier Compose — jamais une valeur écrite en dur |
| Un cache | un cache borné (taille et durée de vie) — voir [Cache et espace disque](caching.md) |

## Branches et vérifications

`main` est la branche de production (Render la déploie). Travaillez sur une branche dont le nom commence par `ci/`
(par exemple `ci/service-client-cs12`) pour que les GitHub Actions s'exécutent avant que quoi que ce soit n'arrive
sur `main` (DEC-CS12-01) :

1. `git switch -c ci/<sujet>` → commit → `git push -u origin ci/<sujet>` : chaque workflow dont le dossier a
   changé s'exécute sur le push (`push: branches: [main, "ci/**"]`).
2. Ouvrez une pull request `ci/<sujet>` → `main` : les mêmes workflows s'exécutent sur le résultat de la fusion.
3. Lisez les résultats : GitHub → **Actions**, ou `gh run list --branch ci/<sujet>` puis
   `gh run view <id> --log-failed`. Chaque workflow peut aussi être lancé à la main (**Run workflow**,
   `workflow_dispatch`).
4. Fusionnez seulement quand toutes les vérifications sont vertes.

| Workflow (`.github/workflows/`) | S'exécute quand … change | Même vérification sur votre ordinateur |
| --- | --- | --- |
| `service-client-backend.yml` | `ServiceClient/backend/**` | `scripts/check.sh backend` |
| `service-client-frontend.yml` | `ServiceClient/frontend/**` | `scripts/check.sh frontend` |
| `service-client-e2e.yml` | backend ou frontend | `scripts/check.sh e2e` |
| `service-client-assistant.yml` | `ServiceClient/assistant/**` | `scripts/check.sh assistant` |
| `service-client-docs.yml` | `ServiceClient/docs/**`, `documentation/**` | `scripts/check.sh docs` |
| `site-vitrine-ci.yml` | `Site_vitrine/**` | `cd ../Site_vitrine && npm ci && npm run lint && npm run typecheck && npm test && npm run build` |
| `backend-ci.yml` (Bakend) | `Bakend/**` | `cd ../Bakend && ./mvnw test` (PostgreSQL et Redis démarrés) |
| `frontend-ci.yml` (Flutter) | `frontend/**` | `cd ../frontend && flutter analyze && flutter test` |

`scripts/check.sh` (Windows : `scripts\check.ps1`) lance les vérifications soit directement, soit dans des
conteneurs Docker, voir [Tests](testing.md#verifier-avec-ou-sans-docker).
