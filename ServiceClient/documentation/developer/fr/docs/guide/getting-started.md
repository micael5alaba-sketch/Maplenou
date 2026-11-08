# Démarrage

Cette page vous mène d'un clone tout neuf à une application, une documentation et un assistant en fonctionnement.
Seul Docker est nécessaire ; Java, Node.js et Python sont facultatifs et accélèrent seulement certaines boucles de
développement.

## Prérequis : Docker uniquement

| Outil | Version | Sert à |
| --- | --- | --- |
| Docker Engine ou Docker Desktop | avec Compose v2 (`docker compose`) | tout : application, documentation, assistant, tests |
| Git | toute version récente | cloner le dépôt |

Rien d'autre n'est à installer : le backend, le frontend, la construction de la documentation, l'assistant et toutes
les suites de tests tournent dans des conteneurs (`docker-compose.yml`, `docker-compose.test.yml`).

```bash
git clone <repository-url>
cd <repository-directory>
```

## Première étape : créer le fichier .env

Les deux fichiers Compose lisent **toutes** leurs valeurs (ports, identifiants, modèles, limites) dans `.env` ; rien
n'y est écrit en dur. Une variable manquante arrête Compose avec un message comme
`Set POSTGRES_DB in .env (copy .env.example)`. Créez le fichier une fois à partir du modèle documenté (`.env` est
ignoré par Git) :

=== "Bash"

    ```bash
    cp .env.example .env
    ```

=== "PowerShell"

    ```powershell
    copy .env.example .env
    ```

Les valeurs par défaut de `.env.example` sont prévues pour les démonstrations locales. Les scripts de test
(`scripts/test.sh`, `scripts/test.ps1`) créent automatiquement `.env` à partir de `.env.example` s'il manque.

## Lancer l'application avec les données de démonstration

```bash
docker compose up -d --build --wait
```

`--wait` rend la main lorsque tous les conteneurs sont démarrés et en bonne santé. Compose démarre trois services :

| Service | Image / construction | Port hôte | Rôle |
| --- | --- | --- | --- |
| `postgres` | `postgres:16-alpine` | non publié | base `POSTGRES_DB` (`maplenou_support`, volume `pgdata`) |
| `backend` | `./backend` (Spring Boot, Java 21) | `8081` (`BACKEND_HOST_PORT` ; 8080 dans le conteneur) | API REST avec le profil `SPRING_PROFILES_ACTIVE` (`demo`) ; pièces jointes dans le volume `attachments` |
| `frontend` | `./frontend` (Next.js standalone) | `3001` (`FRONTEND_HOST_PORT`) | application web ; appelle le backend sur `http://backend:8080` |

Les ports hôte 3001 et 8081 laissent 3000 et 8080 libres pour le back-office d'administration Maplenou et Bakend, qui
tournent sur le même ordinateur une fois ce projet devenu le dossier `ServiceClient/` du dépôt Maplenou (voir
[Intégration à la plateforme Maplenou](maplenou-integration.md)). Chaque service a un contrôle de santé et `restart: unless-stopped` ; le backend attend une base en bonne santé et le
frontend un backend en bonne santé. Les journaux des conteneurs font l'objet d'une rotation
(`LOG_MAX_SIZE` × `LOG_MAX_FILES`, voir [Exploitation](operations.md)).

Ouvrez ensuite :

| URL | Contenu |
| --- | --- |
| <http://localhost:3001> | application web — la page de connexion liste les personas de démonstration |
| <http://localhost:8081/swagger-ui.html> | documentation interactive de l'API (Swagger UI) — voir [Tester l'API](api-testing.md) |
| <http://localhost:8081/api/v1/support> | chemin de base de l'API (jeton bearer requis) |
| <http://localhost:8081/actuator/health> | santé du backend |

Le profil `demo` active une connexion de démonstration et crée des commandes, livraisons et tickets d'exemple. Les
personas viennent de `backend/src/main/java/com/maplenou/support/demo/DemoUsers.java` :

| Persona | Rôle | Arrive sur |
| --- | --- | --- |
| Jean K. | `CUSTOMER` | `/aide` |
| Marie K. | `SELLER` | `/aide` |
| Kossi D. | `DELIVERY_DRIVER` | `/aide` |
| Sophie N., Paul D. | `SUPPORT_AGENT` | `/support` |
| Amina K. | `SUPPORT_MANAGER` | `/support` |
| Admin Maplenou | `ADMINISTRATOR` | `/support` |

Suivez les journaux avec `docker compose logs -f backend`. Arrêtez la pile avec `docker compose down`.
`docker compose down -v` supprime aussi les volumes (base de données, pièces jointes, modèles Ollama téléchargés et
cache des embeddings de l'assistant) : le démarrage suivant recrée les données de démonstration. Pour vérifier de bout
en bout une pile démarrée, lancez `scripts/smoke-test.sh` (ou `scripts\smoke-test.ps1`) — voir [Tests](testing.md). Pour appeler l'API à la main,
utilisez Swagger UI ou la collection Postman de `backend/http/postman/` — voir
[Tester l'API (Swagger, Postman)](api-testing.md).

!!! warning "Démonstration uniquement"
    Le profil `demo` et le `JWT_SECRET` par défaut de `.env.example` sont réservés aux démonstrations locales. Ne les
    utilisez jamais en production — voir [Exploitation](operations.md). Pour vous connecter avec de vrais comptes
    Maplenou et lire les vraies commandes dans Bakend, suivez [Intégration à la plateforme Maplenou](maplenou-integration.md).

## Lancer la documentation et l'assistant

```bash
docker compose --profile docs up -d --build --wait
```

Le profil `docs` ajoute deux services aux trois précédents (trois avec le profil `ollama`) :

| Service | Rôle |
| --- | --- |
| `docs` | nginx qui sert les sites de documentation listés dans `DOCS_SITES`, construits une seule fois dans l'image (`documentation/Dockerfile`), sur le port hôte `8006` (`DOCS_HOST_PORT`) |
| `assistant` | l'API de l'assistant de documentation (`./assistant`), appelée par les sites sur `/api/assistant` ; lit les index de recherche sur le serveur `docs` (`ASSISTANT_INDEX_URL=http://docs`) et appelle Ollama sur `OLLAMA_URL` |
| `ollama` | seulement avec `--profile ollama` : `OLLAMA_IMAGE` (`ollama/ollama:latest`), modèles stockés dans le volume `ollama`, au plus un modèle en mémoire |

Ouvrez <http://localhost:8006> : la page portail mène à chaque site et à chaque langue.

L'assistant a besoin d'un serveur Ollama ; `OLLAMA_URL` dans `.env` le choisit :

- **Ollama cloud** (par défaut, `OLLAMA_URL=https://ollama.com`) : rien à installer et aucun modèle sur le disque.
  Créez un compte gratuit sur <https://ollama.com>, puis une clé sur <https://ollama.com/settings/keys>, et collez-la
  dans `.env` : `OLLAMA_API_KEY=…` (un secret : `.env` n'est jamais commité et la clé n'arrive jamais au navigateur).
  `.env.example` fixe déjà `ASSISTANT_CHAT_MODEL=gpt-oss:20b`, `ASSISTANT_EMBED_MODEL=` (vide : recherche par
  mots-clés seulement), `ASSISTANT_THINK=low` et `ASSISTANT_PULL_MODELS=false`.
- **Ollama installé sur l'ordinateur** (`OLLAMA_URL=http://host.docker.internal:11434`) : installez et lancez
  l'application Ollama pour Windows ou macOS, videz `OLLAMA_API_KEY` et `ASSISTANT_THINK`, définissez
  `ASSISTANT_CHAT_MODEL=qwen2.5:3b`, `ASSISTANT_EMBED_MODEL=nomic-embed-text` et `ASSISTANT_PULL_MODELS=true`, puis
  utilisez la commande ci-dessus. Aucune image Ollama n'est téléchargée.
- **Ollama dans un conteneur** (`OLLAMA_URL=http://ollama:11434`, autres valeurs comme pour l'ordinateur) : ajoutez le
  profil `ollama` — `docker compose --profile docs --profile ollama up -d --build --wait`.

La documentation démarre même si Ollama n'est pas encore joignable (ou si la clé manque) ; détails et dépannage dans
[Ollama : cloud, sur l'ordinateur ou dans Docker](assistant.md#ollama-cloud-sur-lordinateur-ou-dans-docker).

!!! info "Ollama local : le premier démarrage télécharge les modèles"
    Avec Ollama cloud, rien n'est téléchargé. Avec un Ollama local et `ASSISTANT_PULL_MODELS=true`, l'assistant
    télécharge à son premier démarrage le modèle d'embeddings `ASSISTANT_EMBED_MODEL` (`nomic-embed-text`) et le
    modèle de conversation `ASSISTANT_CHAT_MODEL` (`qwen2.5:3b`) via l'API d'Ollama dans l'Ollama qu'il utilise
    (environ 2,3 Go au total ; les démarrages suivants les trouvent et sautent le téléchargement).
    En attendant, l'assistant fonctionne quand même : il classe les sections par mots-clés et, sans modèle de
    conversation, répond avec les sections les plus proches. Dans tous les modes, tant que les index de recherche ne
    sont pas encore chargés depuis le serveur `docs`, les questions reçoivent `503 INDEX_LOADING`. Suivez la progression avec
    `docker compose --profile docs logs -f assistant` et `curl http://localhost:8006/api/assistant/health`.

## Configurer les ports et les secrets avec .env

Modifiez une valeur dans `.env`, puis relancez `docker compose up -d --build --wait`. Les valeurs que vous changerez
le plus probablement :

| Variable | Défaut | Utilisée par |
| --- | --- | --- |
| `FRONTEND_HOST_PORT` | `3001` | port hôte de l'application web (3000 est le back-office d'administration Maplenou) |
| `BACKEND_HOST_PORT` | `8081` | port hôte de l'API (8080 est Bakend) |
| `DOCS_HOST_PORT` | `8006` | port hôte de la documentation (profil `docs`) |
| `POSTGRES_HOST_PORT` | `5433` | utilisée seulement par `docker-compose.override.yml` |
| `POSTGRES_PASSWORD` | `maplenou` | mot de passe de la base (base et backend) |
| `JWT_SECRET` | `demo-only-secret-change-me-0123456789abcdef` | secret HS256 des jetons propres au service support (démonstration et connexion Maplenou) |
| `SLA_MODE` | `always` | horloge SLA : `always` = 24 h/24, 7 j/7 (week-ends et jours fériés compris), `business-hours` = du lundi au vendredi 08:00–18:00 hors jours fériés |
| `MAPLENOU_CORE_URL` | `http://host.docker.internal:8080` | Bakend (API cœur de Maplenou) vu depuis les conteneurs |
| `MAPLENOU_LOGIN_ENABLED` | `false` | `true` = connexion avec un compte Maplenou (avec `AUTH_MODE=maplenou` et `SPRING_PROFILES_ACTIVE=default`) |
| `AUTH_MODE` | `demo` | page de connexion de l'application web : `demo` (personas) ou `maplenou` |
| `ECOSYSTEM_MODE` | `demo` | `demo` (commandes d'exemple) ou `http` (commandes et livraisons réelles de Bakend) |
| `SUPPORT_SERVICE_TOKEN` | vide | secret partagé avec Bakend, nécessaire pour `ECOSYSTEM_MODE=http` et l'import des anciens tickets |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3001` | origines navigateur autorisées à appeler l'API |
| `DOCS_PUBLIC_URL` | `http://localhost:8006` | liens de l'application vers la documentation, URL des références de l'assistant et origine autorisée |
| `OLLAMA_URL` | `https://ollama.com` | Ollama utilisé par l'assistant : Ollama cloud (par défaut), `http://host.docker.internal:11434` (ordinateur) ou `http://ollama:11434` (conteneur, `--profile ollama`) |
| `OLLAMA_API_KEY` | vide | clé secrète Ollama cloud (<https://ollama.com/settings/keys>) ; vide pour un Ollama local |
| `ASSISTANT_CHAT_MODEL` | `gpt-oss:20b` | modèle de conversation (Ollama local : `qwen2.5:3b`) |
| `ASSISTANT_EMBED_MODEL` | vide | modèle d'embeddings ; vide = recherche par mots-clés seulement (Ollama local : `nomic-embed-text`) |
| `ASSISTANT_THINK` | `low` | effort de raisonnement de `gpt-oss` ; vide pour les modèles sans raisonnement |
| `ASSISTANT_PULL_MODELS` | `false` | `true` = l'assistant télécharge les modèles manquants dans un Ollama local |
| `COMPOSE_PROFILES` | en commentaire | facultatif : profils démarrés par un simple `docker compose up` (par exemple `docs`, ou `docs,ollama`) |
| `FIREBASE_CREDENTIALS_FILE` | vide | backend : clé du compte de service Firebase, `/run/secrets/maplenou/firebase-service-account.json` (fichier dans `secrets/`) ; vide = notifications push seulement écrites dans le journal — voir [Firebase](firebase.md) |
| `FIREBASE_WEB_API_KEY`, `FIREBASE_WEB_AUTH_DOMAIN`, `FIREBASE_WEB_PROJECT_ID`, `FIREBASE_WEB_MESSAGING_SENDER_ID`, `FIREBASE_WEB_APP_ID`, `FIREBASE_WEB_VAPID_KEY` | vides | application web : valeurs Firebase publiques pour le push dans le navigateur, lues à l'exécution par `/api/push-config` ; toutes vides = pas de push dans le navigateur |

Si vous changez `FRONTEND_HOST_PORT`, mettez à jour `CORS_ALLOWED_ORIGINS` ; si vous changez `DOCS_HOST_PORT`,
mettez le même port dans `DOCS_PUBLIC_URL`, pour que les liens et le CORS restent corrects. Toutes les variables sont
listées dans [Exploitation](operations.md).

## Quand quelque chose ne démarre pas : le script doctor

Lancez le script de diagnostic, en lecture seule :

=== "PowerShell"

    ```powershell
    scripts\doctor.ps1
    ```

=== "Bash"

    ```bash
    scripts/doctor.sh
    ```

Il vérifie que Docker tourne et dispose d'assez de mémoire, liste les variables de `.env.example` absentes de `.env`,
valide `docker compose config`, vérifie que les ports hôte (`FRONTEND_HOST_PORT`, `BACKEND_HOST_PORT`,
`DOCS_HOST_PORT`) sont libres, qu'Ollama fonctionne (l'Ollama de l'ordinateur et ses modèles, ou avec Ollama cloud
la clé et le modèle sur ollama.com), que Bakend répond quand l'intégration Maplenou est active, et affiche l'état
et la santé de chaque conteneur — avec ses 25 dernières lignes de journal en cas d'échec — ainsi que la santé de
l'assistant. Il ne modifie rien. Autres symptômes : [Dépannage](assistant.md#depannage).

## « Port 5432 is already allocated »

PostgreSQL n'est **pas publié** sur votre machine par défaut : le backend l'atteint dans le réseau Compose sous le nom
`postgres:5432`. Cela évite l'erreur classique `Bind for 0.0.0.0:5432 failed: port is already allocated` lorsqu'un
PostgreSQL est déjà installé localement.

Si vous avez besoin de la base depuis votre machine (client SQL, backend lancé en natif), publiez-la sur un autre port
avec l'exemple de surcharge — Compose charge automatiquement `docker-compose.override.yml`, et ce fichier est ignoré
par Git :

=== "Bash"

    ```bash
    cp docker-compose.override.example.yml docker-compose.override.yml
    docker compose up -d postgres
    psql -h localhost -p 5433 -U maplenou maplenou_support
    ```

=== "PowerShell"

    ```powershell
    Copy-Item docker-compose.override.example.yml docker-compose.override.yml
    docker compose up -d postgres
    psql -h localhost -p 5433 -U maplenou maplenou_support
    ```

Le port hôte est `POSTGRES_HOST_PORT` (`5433` par défaut). Si un autre port est déjà pris (`3001`, `8081`, `8006`),
définissez le `*_HOST_PORT` correspondant dans `.env` (évitez 3000 et 8080, utilisés par le back-office
d'administration Maplenou et Bakend). Pour savoir quel programme occupe un port :

=== "Bash"

    ```bash
    docker ps --format '{{.Names}}\t{{.Ports}}'
    lsof -i :5432          # macOS / Linux
    ```

=== "PowerShell"

    ```powershell
    docker ps --format '{{.Names}}\t{{.Ports}}'
    Get-NetTCPConnection -LocalPort 5432 | Select-Object OwningProcess
    ```

## Outils natifs facultatifs

Les outils natifs raccourcissent la boucle modification–exécution. Les versions sont fixées dans le dépôt (voir
[Agents IA](ai-agents.md) pour la liste complète).

| Partie | Outils | Remarques |
| --- | --- | --- |
| Backend | Java 21 (JDK) | Maven vient du wrapper `backend/mvnw` (`mvnw.cmd` sous Windows) |
| Frontend | Node.js 22 (npm 10) | installation avec `npm ci --legacy-peer-deps` |
| Documentation | Python 3.11+ et `uv` | `pyproject.toml` fixe `zensical==0.0.69` ; `uv.lock` le verrouille |
| Assistant | Python 3.12+ et `uv` (0.13.0 dans les images) | `assistant/pyproject.toml` et `assistant/uv.lock` (groupe dev : pytest) |

### Backend sur la base de Compose

Le backend natif a besoin de la base publiée sur l'hôte (surcharge ci-dessus) ; pointez-le donc vers le port `5433` :

=== "Bash"

    ```bash
    cp docker-compose.override.example.yml docker-compose.override.yml
    docker compose up -d postgres
    cd backend
    DATABASE_URL=jdbc:postgresql://localhost:5433/maplenou_support SPRING_PROFILES_ACTIVE=demo ./mvnw spring-boot:run
    ```

=== "PowerShell"

    ```powershell
    Copy-Item docker-compose.override.example.yml docker-compose.override.yml
    docker compose up -d postgres
    cd backend
    $env:DATABASE_URL="jdbc:postgresql://localhost:5433/maplenou_support"; $env:SPRING_PROFILES_ACTIVE="demo"; .\mvnw.cmd spring-boot:run
    ```

Le backend natif écoute sur `http://localhost:8081` (`server.port: ${SERVER_PORT:8081}`) : il peut tourner à côté de
Bakend sur 8080.

### Frontend sur un backend en fonctionnement

```bash
cd frontend
npm ci --legacy-peer-deps
cp .env.example .env.local     # then set BACKEND_URL=http://localhost:8081 (the native backend's port) in it
npm run dev                    # http://localhost:3001 (npm run dev/start use port 3001)
```

### Tests de l'assistant en natif

```bash
cd assistant
uv sync          # creates assistant/.venv from uv.lock, including the dev group (pytest)
uv run pytest
```

Lancer l'assistant lui-même sur des sites construits localement est décrit dans
[Sites de documentation](documentation.md).

### Aperçu de la documentation

```bash
uv sync                                          # installs zensical 0.0.69 from uv.lock
uv run python documentation/build.py --prepare-only
uv run zensical serve -f zensical.toml -a localhost:8007   # English developer site
```

Les détails, y compris pour les autres sites, sont dans [Sites de documentation](documentation.md).
