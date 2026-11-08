# Tests

Chaque suite tourne dans des conteneurs : Java, Node.js et Python ne sont pas nécessaires. Les mêmes suites tournent
en natif lorsque les outils sont installés, ainsi que dans GitHub Actions.

## Pyramide des tests

| Niveau | Outil | Emplacement | Ce qui est vérifié |
| --- | --- | --- | --- |
| Backend unitaire (96) | JUnit 5 (Surefire, `*Test`), ArchUnit | `backend/src/test/java/.../` | `ArchitectureTest` (couches hexagonales et frontières des modules), règles du flux et de la matrice des droits (`WorkflowTest`, `TicketAccessPolicyTest`), calendriers ouvré et 24 h/24, objectifs, pause et états SLA, règles des pièces jointes, sûreté du CSV (`CsvWriterTest`), adaptateurs face à de faux services locaux : `CloudinaryStorageTest`, `ClamAvScannerTest`, `BakendContractTest` |
| Backend intégration (87) | test Spring Boot + MockMvc (Failsafe, `*IT`) | `backend/src/test/java/.../it/` | HTTP → sécurité avec de vrais JWT → services → PostgreSQL 16 avec le schéma Flyway ; `CachingIT` : ETag/`304` des catégories et cache vidé après une modification d'un administrateur ; `SlaModeIT` : échéances 24 h/24 et `admin/sla-settings` ; `MaplenouIntegrationIT` : connexion Maplenou (avec 2FA), rôles support par téléphone, import des anciens tickets, avec le faux cœur `FakeMaplenouCore` ; `SupportFeaturesIT` : pause SLA, fermeture automatique, satisfaction, réponses types, export CSV, vocabulaire configurable, actions sur Maplenou, outbox, modèles par langue ; `OperationsIT` : suggestion et brouillon IA (faux assistant), limite 429, jeton des métriques, identifiant de requête |
| Frontend unitaire/composants (42) | Vitest 4 + jsdom + Testing Library | `frontend/src/**/*.test.{ts,tsx}` | client d'API et son cache (`apiCache.test.ts`), pièces jointes, libellés, utilitaires i18n et parité des clés, vocabulaire configurable (`vocabulary.test.ts`), composants (satisfaction, statuts personnalisés) |
| Frontend contrôles statiques | ESLint, `tsc --noEmit`, `next build` | `frontend/` | règles de lint, types (dont la parité des clés FR/EN), build de production |
| Assistant (33) | pytest | `assistant/tests/` | recherche, citations, replis, flux, périmètre, validation, caches des embeddings et des réponses, limitation de débit, Ollama cloud (`test_cloud.py`), recherche d'aide et aide IA des agents (`test_support.py` : jeton, listes, texte non fiable, guide utilisateur seulement) |
| De bout en bout | Playwright 1.56 | `frontend/e2e/scenario.spec.ts` | le parcours complet d'un ticket dans un vrai navigateur, sur bureau et mobile |
| Documentation | `build.py --strict` (lancé aussi par la construction de l'image docs) | `documentation/`, `docs/` | chaque page se construit, aucune page ni ancre manquante |
| Pile démarrée | `scripts/smoke-test.*` | `scripts/` | une pile démarrée répond de bout en bout (voir plus bas) |
| Scénario d'API | collection Postman / newman (40 requêtes, 59 vérifications) | `backend/http/postman/` | tout le parcours d'un ticket par l'API REST, avec vérifications — voir [Tester l'API](api-testing.md) |

Les tests d'intégration du backend étendent `it.IntegrationTest`, qui émet de vrais jetons avec `AccessTokenIssuer`, crée
ses propres utilisateurs pour chaque test (les tests ne partagent jamais de données) et désactive le répartiteur push.
Il utilise `TEST_DATABASE_URL` (avec `TEST_DATABASE_USERNAME`, `TEST_DATABASE_PASSWORD`, `maplenou` par défaut)
lorsqu'elle est définie, sinon il démarre un `postgres:16-alpine` via Testcontainers.

## Commandes pour vérifier le projet

Le projet se vérifie de deux façons : **avec Docker** (rien d'autre à installer) ou **sans Docker** (avec les outils
natifs). Les deux exécutent les mêmes tests. Les commandes sont données pour Windows PowerShell et pour Bash (macOS,
Linux, Git Bash) ; lancez-les depuis la racine du dépôt.

### Avec Docker

| Objectif | PowerShell (Windows) | Bash (macOS, Linux) |
| --- | --- | --- |
| Créer la configuration (une fois ; les scripts de test le font si `.env` manque) | `copy .env.example .env` | `cp .env.example .env` |
| Toutes les suites : backend, frontend, assistant, docs, e2e | `.\scripts\test.ps1 all` | `./scripts/test.sh all` |
| Backend : 102 tests unitaires + 87 tests d'intégration, PostgreSQL en mémoire | `.\scripts\test.ps1 backend` | `./scripts/test.sh backend` |
| Frontend : lint, types, 45 tests unitaires, build de production | `.\scripts\test.ps1 frontend` | `./scripts/test.sh frontend` |
| Assistant : 35 tests, faux Ollama | `.\scripts\test.ps1 assistant` | `./scripts/test.sh assistant` |
| Documentation : construction stricte des six sites (image docs) | `.\scripts\test.ps1 docs` | `./scripts/test.sh docs` |
| De bout en bout : démarre la pile, puis Playwright | `.\scripts\test.ps1 e2e` | `./scripts/test.sh e2e` |
| Démarrer l'application et attendre qu'elle soit en bonne santé | `docker compose up -d --build --wait` | `docker compose up -d --build --wait` |
| … avec la documentation et l'assistant (Ollama cloud par défaut) | `docker compose --profile docs up -d --build --wait` | `docker compose --profile docs up -d --build --wait` |
| … idem, avec le conteneur Ollama (`OLLAMA_URL=http://ollama:11434`) | `docker compose --profile docs --profile ollama up -d --build --wait` | `docker compose --profile docs --profile ollama up -d --build --wait` |
| Diagnostiquer une pile qui ne démarre pas (lecture seule) | `.\scripts\doctor.ps1` | `./scripts/doctor.sh` |
| Test de fumée de la pile démarrée | `.\scripts\smoke-test.ps1` | `./scripts/smoke-test.sh` |
| Scénario d'API avec vérifications (collection Postman, Node.js requis pour `npx`) | `npx newman run backend/http/postman/Maplenou-Support-API.postman_collection.json -e backend/http/postman/Maplenou-local.postman_environment.json` | même commande |
| Essayer un point d'accès dans le navigateur (Swagger UI) | <http://localhost:8081/swagger-ui.html> | <http://localhost:8081/swagger-ui.html> |
| État et santé des conteneurs | `docker compose ps` | `docker compose ps` |
| Journaux d'un service | `docker compose logs -f backend` | `docker compose logs -f backend` |
| Espace disque utilisé par Docker | `docker system df` | `docker system df` |
| Libérer de l'espace disque | `.\scripts\clean.ps1` | `./scripts/clean.sh` |

`scripts/test.sh` (macOS, Linux) et `scripts/test.ps1` (Windows) prennent un argument,
`backend|frontend|assistant|docs|e2e|all` (`all` par défaut), s'arrêtent à la première suite en échec, puis
suppriment les conteneurs de test. Les volumes de cache (`maven-cache`, `npm-cache`, `frontend-node-modules`,
`e2e-node-modules`) sont conservés : les exécutions suivantes ne retéléchargent pas les dépendances. Les services sont
définis dans `docker-compose.test.yml`, qui lit ses valeurs dans `.env` (`TEST_POSTGRES_*`, rotation des journaux) et
réutilise les images de base de l'application :

| Suite | Service Compose | Image | Commande |
| --- | --- | --- | --- |
| `backend` | `backend-tests` + `test-db` | `eclipse-temurin:21-jdk-noble`, `postgres:16-alpine` (base en `tmpfs`) | `./mvnw -B verify` avec `TEST_DATABASE_URL=jdbc:postgresql://test-db:5432/<TEST_POSTGRES_DB>` |
| `frontend` | `frontend-tests` | `node:22-alpine` | `npm ci --legacy-peer-deps` → `lint` → `typecheck` → `test` → `build` |
| `assistant` | `assistant-tests` | `./assistant`, cible de build `test` | `python -m pytest -q` avec les petits index propres aux tests |
| `docs` | `docs` de `docker-compose.yml` (profil `docs`) | `documentation/Dockerfile` | `docker compose --profile docs build docs` : `build.py --strict` s'exécute pendant la construction de l'image |
| `e2e` | `e2e` | `mcr.microsoft.com/playwright:v1.56.0-noble` | `npx playwright test` sur `http://frontend:3000` |

Les scripts ne sont que des raccourcis ; vous pouvez appeler Compose directement :

```bash
docker compose -f docker-compose.test.yml run --rm backend-tests
docker compose -f docker-compose.test.yml run --rm frontend-tests
docker compose -f docker-compose.test.yml run --rm assistant-tests
docker compose --profile docs build docs
docker compose -f docker-compose.test.yml down -v      # also deletes the cache volumes

# end-to-end: start the full stack first, then run Playwright in the same Compose project
docker compose up -d --build --wait
docker compose -f docker-compose.yml -f docker-compose.test.yml run --rm e2e
```

La suite e2e laisse la pile applicative en marche ; arrêtez-la avec `docker compose down` (ajoutez `-v` pour
réinitialiser les données de démonstration).

### Sans Docker

Installez d'abord les outils (voir [Démarrage](getting-started.md)) : Java 21, Node.js 22, Python 3.12 et uv.

| Objectif | PowerShell (Windows) | Bash (macOS, Linux) |
| --- | --- | --- |
| Backend, base fournie par Testcontainers (Docker requis) | `cd backend; .\mvnw.cmd verify` | `cd backend && ./mvnw verify` |
| Backend sur un PostgreSQL local (sans Docker) | `cd backend; $env:TEST_DATABASE_URL="jdbc:postgresql://localhost:5432/maplenou_test"; .\mvnw.cmd verify` | `cd backend && TEST_DATABASE_URL=jdbc:postgresql://localhost:5432/maplenou_test ./mvnw verify` |
| Une classe de test du backend | `.\mvnw.cmd "-Dtest=BusinessCalendarTest" test` | `./mvnw -Dtest=BusinessCalendarTest test` |
| Une classe de test d'intégration | `.\mvnw.cmd "-Dit.test=CachingIT" verify` | `./mvnw -Dit.test=CachingIT verify` |
| Frontend : lint, types, tests unitaires, build | `cd frontend; npm ci --legacy-peer-deps; npm run lint; npm run typecheck; npm test; npm run build` | `cd frontend && npm ci --legacy-peer-deps && npm run lint && npm run typecheck && npm test && npm run build` |
| Assistant | `cd assistant; uv sync; uv run pytest` | `cd assistant && uv sync && uv run pytest` |
| Documentation, tous les sites (strict) | `uv sync; uv run python documentation/build.py --strict` | `uv sync && uv run python documentation/build.py --strict` |
| Documentation, un seul site avec Zensical | `uv run python documentation/build.py --prepare-only; uv run zensical build --strict -f zensical.toml` | `uv run python documentation/build.py --prepare-only && uv run zensical build --strict -f zensical.toml` |
| De bout en bout (application lancée sur le port 3001) | `cd frontend; npx playwright install chromium; npx playwright test` | `cd frontend && npx playwright install chromium && npx playwright test` |
| Test de fumée d'une pile native | `.\scripts\smoke-test.ps1 -Api http://localhost:8081 -App http://localhost:3001` | `./scripts/smoke-test.sh http://localhost:8081 http://localhost:3001` |

- La base de test de `TEST_DATABASE_URL` doit exister (`maplenou_test`) ; l'utilisateur et le mot de passe viennent de
  `TEST_DATABASE_USERNAME` et `TEST_DATABASE_PASSWORD` (`maplenou` par défaut).
- Dans Windows PowerShell 5.1, `;` lance la commande suivante même si la précédente a échoué : vérifiez chaque
  résultat (PowerShell 7 accepte aussi `&&`).
- Playwright lit `E2E_BASE_URL` (par défaut `http://localhost:3001`) ; son rapport HTML est écrit dans
  `frontend/playwright-report/`.
- Le test de fumée a besoin d'un backend avec le profil `demo` (connexion de démonstration) ; son troisième argument
  (`-Docs`) est l'URL de la documentation, vérifiée seulement si elle répond.

## Vérifier avec ou sans Docker

`scripts/check.sh` (Windows : `scripts\check.ps1`) lance **les mêmes commandes que les GitHub Actions** et choisit,
pour chaque suite, l'outil installé sur l'ordinateur s'il existe, sinon Docker. Contrairement à `scripts/test.sh`, il
ne s'arrête pas au premier échec : il termine par une ligne par suite (`PASS`, `FAIL` ou `SKIP` avec la raison) — le
moyen le plus rapide de savoir ce que répondront les Actions avant de pousser.

| Objectif | PowerShell (Windows) | Bash (macOS, Linux) |
| --- | --- | --- |
| Suites de ServiceClient, chacune sans Docker si possible, sinon dans Docker | `.\scripts\check.ps1` | `./scripts/check.sh` |
| Forcer Docker (rien d'installé à part Docker) | `.\scripts\check.ps1 -Mode docker` | `./scripts/check.sh --docker` |
| Forcer les outils installés (sans Docker) | `.\scripts\check.ps1 -Mode native` | `./scripts/check.sh --native` |
| Tout ce que vérifie une pull request vers `main` (ajoute `Site_vitrine`, Bakend, Flutter) | `.\scripts\check.ps1 ci` | `./scripts/check.sh ci` |
| Seulement certaines suites | `.\scripts\check.ps1 -Mode native backend docs` | `./scripts/check.sh --native backend docs` |

| Suite | Sans Docker (ce que lance le workflow) | Docker |
| --- | --- | --- |
| `backend` | `./mvnw -B verify` (Testcontainers, ou `TEST_DATABASE_URL`) | `backend-tests` + `test-db` |
| `frontend` | `npm ci --legacy-peer-deps`, `lint`, `typecheck`, `test`, `build` | `frontend-tests` |
| `assistant` | `uv sync --locked`, `uv run pytest -q` | `assistant-tests` |
| `docs` | `uv sync --locked`, `documentation/build.py --strict` | construction de l'image docs |
| `e2e` | `npx playwright test` contre `E2E_BASE_URL` (application déjà démarrée) | démarre la pile, puis le service `e2e` |
| `site` | `../Site_vitrine` : `npm ci`, `lint`, `typecheck`, `test`, `build` | `node:22-alpine`, `node_modules` dans un volume nommé |
| `bakend` | `../Bakend` : `./mvnw test` (son PostgreSQL et son Redis démarrés) | `SKIP` — sans Docker ou `Bakend/start-all.ps1` |
| `flutter` | `../frontend` : `flutter pub get`, `flutter analyze`, `flutter test` | `SKIP` — installez Flutter |

## Test de fumée d'une pile démarrée

`scripts/smoke-test.sh` (curl) et `scripts/smoke-test.ps1` (PowerShell seul) vérifient une pile déjà démarrée, avec
Docker ou en natif, et s'arrêtent au premier échec :

1. `/actuator/health` du backend vaut `UP` et la connexion de démonstration renvoie des jetons pour Jean K. et
   Sophie N. ;
2. `GET /categories` renvoie un `ETag`, et la même requête avec `If-None-Match` renvoie `304` ;
3. Jean crée un ticket, Sophie le prend en charge, répond et le résout ;
4. un client reçoit `403` sur le tableau de bord du support ;
5. `GET /notifications/push-status` affiche le canal push du backend : `firebase` (la clé de
   `FIREBASE_CREDENTIALS_FILE` est chargée) ou `log-only` — voir [Firebase](firebase.md) ;
6. l'application web sert `/login`, en anglais avec le cookie `maplenou_locale=en` ;
7. si la documentation répond sur son URL : le `search.json` des six sites et `/api/assistant/health`.

Le test crée un vrai ticket (« Smoke test ») dans la base. Arguments : URL de l'API, de l'application web et de la
documentation (par défaut `http://localhost:8081`, `http://localhost:3001`, `http://localhost:8006`).

## Tests manuels de l'API

`backend/http/support-api.http` est une collection de requêtes pour le REST Client de VS Code et le client HTTP
JetBrains : santé, jetons de démonstration, puis un parcours complet d'un ticket, à envoyer de haut en bas. Voir
[Comment le backend traite une requête](backend-processing.md).

Le même parcours existe en collection Postman avec vérifications automatiques (`backend/http/postman/`, exécutable
aussi avec `newman`), et Swagger UI (<http://localhost:8081/swagger-ui.html>) permet d'essayer n'importe quel point
d'accès avec un jeton de démonstration. Voir [Tester l'API avec Swagger UI et Postman](api-testing.md).

## Ce que couvre le scénario de bout en bout

`frontend/e2e/scenario.spec.ts` pilote la véritable interface avec les personas de démonstration (langue française),
sur le profil `demo` du backend :

1. **Cycle de vie complet.** Jean K. (client) crée un ticket dans la catégorie livraison, lié à la commande
   `CMD-2026-0456`, en priorité élevée et avec une pièce jointe PNG, et obtient un numéro `TKT-YYYY-NNNNN…`.
   Sophie N. (agente) le retrouve dans la file par son numéro, voit le contexte de commande et de livraison, le prend
   en charge, ajoute une note interne, répond et demande des informations. Jean voit « Action requise », jamais la
   note interne, et répond (le ticket repasse « En cours »). Sophie le résout après une boîte de confirmation. Jean
   demande une réouverture ; Sophie l'accepte, résout de nouveau puis clôture le ticket. Jean voit l'état fermé, aucun
   bouton de réouverture, et les notifications in-app ; Sophie voit l'historique audité complet.
2. **Accès refusé.** Marie K. (vendeuse) qui ouvre `/support` est redirigée vers la page d'accès refusé.
3. **Ticket d'un collègue en lecture seule.** Sophie peut ouvrir un ticket attribué à un autre agent mais voit
   « Lecture seule » et aucun bouton d'envoi.
4. **Mobile** (`@mobile`, profil Pixel 7). Kossi D. (livreur) signale un incident de livraison depuis un téléphone.
5. **Langue et thème.** Passer en anglais et en mode sombre modifie `<html lang>` et `data-theme`, résiste à un
   rechargement (cookies), et le retour au français fonctionne.

Le projet `desktop` (Desktop Chrome) exécute tous les tests sauf celui marqué `@mobile`, qui y est ignoré ; le projet
`mobile` n'exécute que `@mobile`.

## Construction stricte de la documentation

Traitez la documentation comme du code : construisez-la en mode strict avant chaque commit qui touche `docs/` ou
`documentation/`. Avec Docker, `scripts/test.sh docs` fait de même via l'image docs.

```bash
python3 documentation/build.py --sites developer --strict   # must end with "Built 2 site(s)"
python3 documentation/build.py --strict                     # all six sites
```

Le mode strict s'arrête au moindre avertissement, par exemple un lien vers une page inexistante ou vers une
`#ancre` manquante.

## Intégration continue

| Workflow | Déclencheur | Exécute |
| --- | --- | --- |
| `.github/workflows/service-client-backend.yml` | changements dans `backend/` | `./mvnw -B verify` (Testcontainers), publie les rapports Surefire/Failsafe |
| `.github/workflows/service-client-frontend.yml` | changements dans `frontend/` | `npm ci`, lint, typecheck, test, build |
| `.github/workflows/service-client-e2e.yml` | chaque push sur `main` et chaque pull request | service PostgreSQL, jar du backend avec `demo`, `next start`, Playwright ; publie le rapport et les journaux |
| `.github/workflows/service-client-assistant.yml` | changements dans `assistant/` | Python 3.12, installation des dépendances, `python -m pytest -q` |
| `.github/workflows/service-client-docs.yml` | push et pull request sur `main` | `python documentation/build.py --strict` (six sites) ; sur `main`, construit **uniquement le guide utilisateur** et le déploie sur GitHub Pages |
