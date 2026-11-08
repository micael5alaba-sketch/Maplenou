# Exploitation

Cette page couvre la configuration, les secrets, les contrôles de santé, les journaux, les sauvegardes, l'espace
disque et la publication. Le dépôt fournit une pile Docker Compose locale pour les démonstrations et le développement ; les points
encore ouverts pour la production sont listés dans la [security review](/developer/en/architecture/security/)
(« Before production »).

## Référence de configuration

### Pile Docker (.env)

`docker-compose.yml` et `docker-compose.test.yml` prennent toutes leurs valeurs dans `.env` (modèle et documentation :
`.env.example` ; créez-le avec `cp .env.example .env`, sous Windows `copy .env.example .env`). Une variable
obligatoire manquante arrête Compose avec `Set <NAME> in .env`. Rien n'est écrit en dur dans les fichiers Compose.

| Variable | `.env.example` | Utilisée par | Rôle |
| --- | --- | --- | --- |
| `FRONTEND_HOST_PORT` | `3001` | `frontend` | port hôte de l'application web (3000 est le back-office d'administration Maplenou) |
| `BACKEND_HOST_PORT` | `8081` | `backend` | port hôte de l'API (8080 est Bakend, l'API cœur de Maplenou) ; dans le conteneur, le backend écoute sur 8080 |
| `DOCS_HOST_PORT` | `8006` | `docs` | port hôte de la documentation (profil `docs`) |
| `POSTGRES_HOST_PORT` | `5433` | `docker-compose.override.yml` | port hôte de PostgreSQL, seulement avec la surcharge |
| `POSTGRES_DB` | `maplenou_support` | `postgres`, `backend` | nom de la base (fait aussi partie de `DATABASE_URL`) |
| `POSTGRES_USER` | `maplenou` | `postgres`, `backend` (`DATABASE_USERNAME`), scripts de sauvegarde | utilisateur de la base |
| `POSTGRES_PASSWORD` | `maplenou` | `postgres`, `backend` (`DATABASE_PASSWORD`) | mot de passe de la base — à changer hors des démonstrations locales |
| `SPRING_PROFILES_ACTIVE` | `demo` | `backend` | `demo` = connexion de démonstration, données d'exemple, tickets créés ; jamais en production |
| `JWT_SECRET` | `demo-only-secret-change-me-0123456789abcdef` | `backend` | secret HS256 des jetons propres au service support (démonstration et connexion Maplenou), au moins 32 caractères ; une valeur aléatoire hors des démonstrations locales |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3001` | `backend` | origines navigateur autorisées à appeler l'API directement |
| `SLA_MODE` | `always` | `backend` | horloge SLA : `always` = 24 h/24, 7 j/7, week-ends et jours fériés compris (DEC-CS10-16) ; `business-hours` = du lundi au vendredi 08:00–18:00 `Africa/Lome`, hors jours fériés du « Calendrier SLA » |
| `MAPLENOU_CORE_URL` | `http://host.docker.internal:8080` | `backend` | Bakend (API cœur de Maplenou) vu depuis le conteneur — connexion, commandes réelles, import des anciens tickets ; voir [Intégration à la plateforme Maplenou](maplenou-integration.md) |
| `MAPLENOU_LOGIN_ENABLED` | `false` | `backend` | `true` = connexion avec un compte Maplenou (numéro de téléphone + mot de passe, vérifiés par Bakend) ; définissez aussi `AUTH_MODE=maplenou` |
| `AUTH_MODE` | `demo` | `frontend` | page de connexion de l'application web : `demo` (personas, profil `demo` du backend) ou `maplenou` (compte Maplenou) |
| `SESSION_COOKIE_SECURE` | `false` | `frontend` | `true` derrière HTTPS (exigé par le profil `public`) : le cookie de session ne circule qu'en HTTPS |
| `ECOSYSTEM_MODE` | `demo` | `backend` | `demo` (commandes d'exemple) ou `http` (commandes et livraisons réelles lues dans Bakend `/internal/v1/**`, nécessite `SUPPORT_SERVICE_TOKEN`) |
| `SUPPORT_SERVICE_TOKEN` | vide | `backend` | **secret** partagé avec Bakend (même valeur dans l'environnement de Bakend), 32 caractères aléatoires ou plus ; nécessaire pour `ECOSYSTEM_MODE=http` et pour l'import des anciens tickets ; vide = non relié (peut être vide) |
| `FIREBASE_CREDENTIALS_FILE` | vide | `backend` | clé du compte de service Firebase dans le conteneur : `/run/secrets/maplenou/firebase-service-account.json` (fichier enregistré dans `secrets/`) ; vide = push seulement journalisé (variable facultative) — voir [Firebase](firebase.md) |
| `FIREBASE_WEB_API_KEY`, `FIREBASE_WEB_AUTH_DOMAIN`, `FIREBASE_WEB_PROJECT_ID`, `FIREBASE_WEB_MESSAGING_SENDER_ID`, `FIREBASE_WEB_APP_ID`, `FIREBASE_WEB_VAPID_KEY` | vides | `frontend` | valeurs publiques de l'application web Firebase, lues à l'exécution par `/api/push-config` ; toutes vides = pas de push dans le navigateur (variables facultatives) |
| `HOST_BIND_ADDRESS` | `127.0.0.1` | tous les ports publiés | adresse sur laquelle écoutent les ports hôtes : `127.0.0.1` = cet ordinateur seulement (recommandé, aussi sur un serveur derrière Caddy) ; `0.0.0.0` = tous les réseaux |
| `MAPLENOU_PUSH_RELAY` | `false` | `backend` | `true` = les notifications des demandeurs sont aussi poussées vers l'app mobile Maplenou par Bakend (`POST /internal/v1/users/{id}/push`) ; nécessite `SUPPORT_SERVICE_TOKEN` |
| `SUPPORT_APP_URL` | `http://localhost:3001` | `backend` | adresse de cette application web vue par les utilisateurs ; les push renvoient vers `<SUPPORT_APP_URL>/?ticket=<id>` (déploiement public : `https://<SUPPORT_DOMAIN>`) |
| `SUPPORT_AUTO_CLOSE_AFTER` | `P7D` | `backend` | un ticket résolu se ferme tout seul après cette durée ISO-8601 s'il n'est pas rouvert (DEC-CS11-04) ; `P0D` = jamais |
| `LEGACY_SYNC_ENABLED`, `LEGACY_SYNC_INTERVAL` | `false`, `PT15M` | `backend` | importe les nouveaux tickets de l'ancien module support de Bakend et note ses changements de statut à intervalle régulier (DEC-CS12-02, [Intégration Maplenou](maplenou-integration.md)) |
| `ATTACHMENT_STORAGE` | `local` | `backend` | `local` (volume `attachments`) ou `cloudinary` (fichiers privés, distribution authentifiée, liens de téléchargement signés valables 5 minutes) |
| `CLOUDINARY_URL` | vide | `backend` | **secret** `cloudinary://<api_key>:<api_secret>@<cloud_name>` (console Cloudinary › API keys) ; nécessaire avec `ATTACHMENT_STORAGE=cloudinary` (peut être vide) |
| `CLOUDINARY_FOLDER` | `maplenou-support` | `backend` | dossier Cloudinary des pièces jointes |
| `ANTIVIRUS` | `none` | `backend` | `none` ou `clamav` (chaque fichier est analysé avant d'être enregistré ; démarrez `--profile antivirus`) |
| `ANTIVIRUS_FAIL_CLOSED` | `true` | `backend` | `true` = refuser les envois tant que ClamAV ne répond pas ; `false` = les accepter sans analyse |
| `CLAMAV_IMAGE` | `clamav/clamav:1.4.3` | `clamav` (`--profile antivirus`) | image ClamAV (environ 1 Go de RAM, signatures ~300 Mo dans le volume `clamav`) |
| `RATE_LIMIT_ENABLED`, `RATE_LIMIT_PER_MINUTE`, `RATE_LIMIT_WRITES_PER_MINUTE` | `true`, `300`, `60` | `backend` | requêtes (toutes / écritures) par minute et par utilisateur connecté, ou par adresse ; au-delà, `429 Too Many Requests` avec `Retry-After` |
| `FORWARD_HEADERS_STRATEGY` | `none` | `backend` | `native` derrière un proxy de confiance (le Caddy du profil `public`) : l'adresse du client vient de `X-Forwarded-For` |
| `LOG_FORMAT` | vide | `backend` (`LOGGING_STRUCTURED_FORMAT_CONSOLE`) | vide = journaux texte lisibles ; `ecs` = un objet JSON par ligne pour un outil de journaux (Loki, Elastic, Datadog) (peut être vide) |
| `METRICS_TOKEN` | vide | `backend`, `prometheus` | **secret** jeton porteur de `/actuator/prometheus`, 24 caractères aléatoires ou plus ; vide = point d'accès absent ; obligatoire avec `--profile monitoring` (peut être vide) |
| `PROMETHEUS_IMAGE`, `PROMETHEUS_RETENTION` | `prom/prometheus:v3.5.0`, `15d` | `prometheus` (`--profile monitoring`) | image Prometheus et durée de conservation des métriques (plafonnée aussi à 1 Go) |
| `GRAFANA_IMAGE`, `GRAFANA_HOST_PORT`, `GRAFANA_ADMIN_PASSWORD` | `grafana/grafana:12.1.1`, `3003`, `change-me-grafana` | `grafana` (`--profile monitoring`) | image Grafana, port hôte et mot de passe `admin` (à changer) |
| `AI_ASSIST_ENABLED` | `false` | `backend` | `true` = aide IA dans les tickets : suggestion de catégorie/priorité et brouillons de réponse (DEC-CS11-08) ; nécessite `--profile docs` et le jeton ci-dessous |
| `ASSISTANT_INTERNAL_TOKEN` | vide | `backend`, `assistant` | **secret** partagé par le backend et l'assistant pour `/api/assistant/internal/*`, 24 caractères aléatoires ou plus ; vide = aide IA désactivée (peut être vide) |
| `SUPPORT_DOMAIN`, `HELP_DOMAIN`, `SITE_DOMAIN` | `support.example.com`, `aide.example.com`, `www.example.com` | `caddy`, `site` (`--profile public`) | noms de domaine publics de l'application web, du centre d'aide (guide utilisateur + assistant) et du site public ; leurs enregistrements DNS pointent vers le serveur — voir [Déploiement](deployment.md) |
| `ACME_EMAIL` | `admin@example.com` | `caddy` | e-mail du compte Let's Encrypt (alertes d'expiration des certificats) |
| `CADDY_IMAGE` | `caddy:2.10.0-alpine` | `caddy` | image Caddy |
| `MAPLENOU_APP_URL`, `ANDROID_APP_URL`, `IOS_APP_URL` | vides | `site` (`--profile public`) | liens du site public (vide = bouton masqué) |
| `REFERENCE_CACHE_TTL` | `PT10M` | `backend` | durée de vie des données de référence en mémoire (catégories, vocabulaire, calendrier et objectifs SLA, textes des notifications), durée ISO-8601 |
| `DOCS_PUBLIC_URL` | `http://localhost:8006` | `frontend` (`DOCS_BASE_URL`), `assistant` (`ASSISTANT_PUBLIC_BASE_URL`, `ASSISTANT_ALLOWED_ORIGINS`) | adresse publique de la documentation |
| `DOCS_SITES` | `user,staff,developer` | `docs` (argument de build) | sites construits dans l'image docs ; `user` pour un déploiement public |
| `ASSISTANT_SITES` | `user,staff,developer` | `assistant` | sites sur lesquels l'assistant répond ; `user` pour un déploiement public |
| `ASSISTANT_CHAT_MODEL` | `gpt-oss:20b` | `assistant` | modèle de conversation : un modèle de l'offre Ollama cloud (liste : <https://ollama.com/api/tags>) ; avec un Ollama local `qwen2.5:3b` |
| `ASSISTANT_EMBED_MODEL` | vide | `assistant` | modèle d'embeddings ; vide = recherche par mots-clés (BM25) seulement, le choix habituel avec Ollama cloud ; avec un Ollama local `nomic-embed-text` (peut être vide) |
| `ASSISTANT_THINK` | `low` | `assistant` | effort de raisonnement des modèles `gpt-oss` (`low`, `medium`, `high`) ; vide pour les modèles sans raisonnement (peut être vide) |
| `ASSISTANT_RATE_LIMIT` | `20` | `assistant` | questions par minute et par visiteur |
| `ASSISTANT_ANSWER_CACHE_SIZE` | `256` | `assistant` | réponses gardées en mémoire (`0` désactive le cache des réponses) |
| `ASSISTANT_ANSWER_CACHE_TTL` | `3600` | `assistant` | durée de vie d'une réponse en cache, en secondes |
| `OLLAMA_URL` | `https://ollama.com` | `assistant` | Ollama utilisé par l'assistant : Ollama cloud (par défaut), `http://host.docker.internal:11434` (installé sur l'ordinateur) ou `http://ollama:11434` (conteneur) — voir [Ollama : cloud, sur l'ordinateur ou dans Docker](assistant.md#ollama-cloud-sur-lordinateur-ou-dans-docker) |
| `OLLAMA_API_KEY` | vide | `assistant` | **secret** — clé Ollama cloud, créée sur <https://ollama.com/settings/keys> ; côté serveur seulement, jamais dans le navigateur ; vide pour un Ollama local (peut être vide) |
| `ASSISTANT_PULL_MODELS` | `false` | `assistant` | `true` = télécharger les modèles manquants dans un Ollama local dès qu'il est joignable (environ 2,3 Go une fois) ; ignoré avec Ollama cloud |
| `COMPOSE_PROFILES` | en commentaire | `docker compose` | facultatif : profils démarrés par un simple `docker compose up` (`docs`, ou `docs,ollama`) |
| `OLLAMA_IMAGE` | `ollama/ollama:latest` | `ollama` (`--profile ollama` uniquement) | image Ollama ; fixez une version (`ollama/ollama:<version>`) pour des déploiements reproductibles |
| `OLLAMA_KEEP_ALIVE` | `10m` | `ollama` (`--profile ollama` uniquement) | temps d'inactivité après lequel un modèle est déchargé de la mémoire |
| `LOG_MAX_SIZE` | `10m` | tous les conteneurs | taille d'un fichier de journal avant rotation |
| `LOG_MAX_FILES` | `3` | tous les conteneurs | fichiers de journal conservés par conteneur |
| `TEST_POSTGRES_DB` | `maplenou_test` | `test-db`, `backend-tests` | nom de la base de test |
| `TEST_POSTGRES_USER` | `maplenou` | `test-db`, `backend-tests` | utilisateur de la base de test |
| `TEST_POSTGRES_PASSWORD` | `maplenou` | `test-db`, `backend-tests` | mot de passe de la base de test |

Quelques valeurs sont fixées dans `docker-compose.yml` car elles décrivent le réseau Compose, pas le déploiement :
`BACKEND_URL=http://backend:8080` (frontend), `DATABASE_URL=jdbc:postgresql://postgres:5432/${POSTGRES_DB}`,
`ASSISTANT_INDEX_URL=http://docs`, `OLLAMA_MAX_LOADED_MODELS=1` et `OLLAMA_NUM_PARALLEL=1`. Les services
`assistant` et `backend` associent aussi `host.docker.internal` à l'hôte (`extra_hosts: host-gateway`, nécessaire
sous Linux) pour pouvoir joindre un Ollama, ou Bakend, lancé sur l'ordinateur.

### Backend (`backend/src/main/resources/application.yml`)

| Variable | Défaut | Rôle |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | aucun (`demo` dans Compose) | `demo` active la connexion de démonstration et les données d'exemple |
| `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` | `jdbc:postgresql://localhost:5432/maplenou_support`, `maplenou`, `maplenou` | connexion PostgreSQL |
| `JWT_SECRET` | vide (le profil `demo` en définit un) | secret HS256 des jetons locaux/de démonstration ; au moins 32 octets |
| `JWT_ISSUER` | `maplenou-support-dev` | émetteur attendu des jetons |
| `DEV_LOGIN_ENABLED` | `false` (`true` en `demo`) | endpoints de connexion de démonstration |
| `ATTACHMENTS_DIR` | `./data/attachments` (`/data/attachments` dans l'image) | stockage privé des pièces jointes |
| `FIREBASE_CREDENTIALS_FILE` | vide | JSON du compte de service Firebase ; vide = push seulement journalisé |
| `SERVER_PORT` | `8081` (`8080` dans l'image Docker) | port HTTP d'un backend lancé nativement (`server.port`) ; 8080 est laissé à Bakend |
| `SLA_MODE` | `always` | `maplenou.sla.mode` : `always` (24 h/24, 7 j/7) ou `business-hours` |
| `MAPLENOU_CORE_URL` | `http://localhost:8080` | URL de base de Bakend (`maplenou.core.base-url` et `maplenou.ecosystem.base-url`) |
| `MAPLENOU_LOGIN_ENABLED` | `false` | `maplenou.core.login-enabled` : expose `POST /api/v1/auth/maplenou/login` et `/mfa` ; nécessite `JWT_SECRET` |
| `ECOSYSTEM_MODE` | `demo` | `demo` (données d'exemple) ou `http` (API interne de Bakend `/internal/v1/**`) |
| `SUPPORT_SERVICE_TOKEN` | vide | `maplenou.ecosystem.service-token`, envoyé à Bakend dans `X-Maplenou-Service-Token` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3001` | origines navigateur autorisées à appeler l'API directement |
| `REFERENCE_CACHE_TTL` | `PT10M` | durée de vie du cache des données de référence (catégories, calendrier ouvré) |

Paramètres fixes du même fichier : fuseau SLA `Africa/Lome` et horaires 08:00–18:00 (utilisés seulement avec
`SLA_MODE=business-hours`) ; délais d'attente vers Bakend 5 s (connexion) et 3 s (commandes, livraisons) ; pièces jointes 10 Mo par fichier, 5 par
message, 50 Mo par ticket ; répartiteur push toutes les 5 s, 5 tentatives ; limite multipart 10 Mo par fichier / 52 Mo
par requête ; cache des données de référence limité à 500 entrées ; compression gzip des réponses JSON de plus de
1 Ko.

### Frontend

| Variable | Défaut | Rôle |
| --- | --- | --- |
| `BACKEND_URL` | `http://localhost:8081` (`http://backend:8080` dans Compose) | URL de l'API utilisée côté serveur par le proxy |
| `AUTH_MODE` | `demo` | page de connexion : `demo` (personas) ou `maplenou` (compte Maplenou) ; lu à l'exécution par `/api/auth-config` |
| `SESSION_COOKIE_SECURE` | `false` | `true` derrière HTTPS pour que le cookie de session soit `Secure` |
| `DOCS_BASE_URL` | `http://localhost:8006` (depuis `DOCS_PUBLIC_URL` dans Compose) | liens de l'application vers la documentation |
| `FIREBASE_WEB_API_KEY`, `FIREBASE_WEB_AUTH_DOMAIN`, `FIREBASE_WEB_PROJECT_ID`, `FIREBASE_WEB_MESSAGING_SENDER_ID`, `FIREBASE_WEB_APP_ID`, `FIREBASE_WEB_VAPID_KEY` | vides | valeurs web Firebase publiques servies à l'exécution par `/api/push-config` (push dans le navigateur) ; voir [Firebase](firebase.md) |

### Construction de la documentation et assistant

| Variable | Utilisée par | Rôle |
| --- | --- | --- |
| `DOCS_SITES` | `documentation/build.py` | sites à construire (`user,staff,developer`) |
| `ASSISTANT_ENDPOINT` | `documentation/build.py` | URL de l'assistant écrite dans le `config.js` de chaque site (`/api/assistant`) |
| `ASSISTANT_*`, `OLLAMA_URL` | `assistant/` | voir [Assistant de documentation](assistant.md) |

Dans Docker, ces variables sont renseignées à partir de `.env` (tableau ci-dessus).

## Secrets

| Secret | Où | Règle |
| --- | --- | --- |
| `JWT_SECRET` | backend | signe les jetons propres au service support. Avec la connexion Maplenou (`MAPLENOU_LOGIN_ENABLED=true`), il est **obligatoire** : une valeur aléatoire de 32 caractères ou plus, jamais celle de démonstration. Sans connexion Maplenou, la production peut le laisser vide et renseigner `spring.security.oauth2.resourceserver.jwt.issuer-uri` avec un fournisseur d'identité |
| `POSTGRES_PASSWORD` | `postgres` et backend (`DATABASE_PASSWORD`) | à changer hors des démonstrations locales |
| `FIREBASE_CREDENTIALS_FILE` | backend | le fichier de clé est dans `secrets/`, monté en lecture seule ; ne le committez jamais |
| `SUPPORT_SERVICE_TOKEN` | backend et Bakend (même valeur des deux côtés) | protège l'API interne de Bakend `/internal/v1/**` ; 32 caractères aléatoires ou plus — voir [Intégration à la plateforme Maplenou](maplenou-integration.md#relier-les-commandes-et-livraisons-reelles) |
| `OLLAMA_API_KEY` | assistant | clé Ollama cloud ; côté serveur seulement, jamais envoyée au navigateur |
| `CLOUDINARY_URL` | backend | clé et secret API du compte Cloudinary (pièces jointes) ; ne jamais la commiter |
| `METRICS_TOKEN` | backend et Prometheus | protège `/actuator/prometheus` ; 24 caractères aléatoires ou plus |
| `ASSISTANT_INTERNAL_TOKEN` | backend et assistant | protège les points d'accès IA de l'assistant ; 24 caractères aléatoires ou plus |
| `GRAFANA_ADMIN_PASSWORD` | Grafana | changez la valeur d'exemple avant de démarrer le profil `monitoring` |

- En local, mettez les valeurs dans `.env` (ignoré par Git). Ailleurs, utilisez le gestionnaire de secrets de la
  plateforme — pas les fichiers Compose.
- **Les fichiers secrets vont dans `secrets/`.** Tout le contenu de ce dossier, sauf son `README.md`, est ignoré par
  Git et par toutes les constructions Docker ; Compose le monte **en lecture seule** dans le backend sur
  `/run/secrets/maplenou`. Il contient aujourd'hui la clé du compte de service Firebase
  `firebase-service-account.json` — voir [Notifications push Firebase](firebase.md). Les valeurs `FIREBASE_WEB_*`
  sont publiques et restent dans `.env`.
- L'image PostgreSQL n'applique `POSTGRES_PASSWORD` qu'à la première initialisation du volume `pgdata`. Pour le
  changer ensuite, changez aussi le mot de passe de l'utilisateur de la base (`ALTER USER`), ou recréez le volume.
- Générer un secret robuste :

=== "Bash"

    ```bash
    openssl rand -base64 48
    ```

=== "PowerShell"

    ```powershell
    [Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }))
    ```

## Le profil de démonstration ne doit jamais tourner en production

Le profil `demo` active `dev-login-enabled`, qui expose `GET /api/v1/dev/users` et `POST /api/v1/dev/token` :
quiconque atteint le backend peut obtenir un jeton pour n'importe quel persona de démonstration. Il crée aussi des
données d'exemple et définit un `JWT_SECRET` par défaut connu de tous.

!!! danger "Avant tout pilote ou déploiement en production"
    - Ne définissez pas `SPRING_PROFILES_ACTIVE=demo` et gardez `DEV_LOGIN_ENABLED=false`.
    - Connectez-vous avec les comptes Maplenou (`MAPLENOU_LOGIN_ENABLED=true`, `AUTH_MODE=maplenou`, un `JWT_SECRET`
      aléatoire différent de celui de démonstration) — voir [Intégration à la plateforme Maplenou](maplenou-integration.md) —
      ou laissez `JWT_SECRET` vide et configurez l'`issuer-uri` d'un fournisseur d'identité.
    - N'exposez jamais sur Internet les chemins `/internal/` de Bakend (règle du proxy inverse).
    - Servez les deux applications en HTTPS uniquement, définissez `SESSION_COOKIE_SECURE=true`, restreignez `CORS_ALLOWED_ORIGINS`.
    - Stockez les secrets dans un gestionnaire de secrets.
    - Voir la liste complète dans la [security review](/developer/en/architecture/security/).

## Contrôles de santé

| Service | Contrôle de santé | Remarques |
| --- | --- | --- |
| `postgres` | `pg_isready` | le backend ne démarre qu'une fois la base en bonne santé |
| `backend` | `HEALTHCHECK` de l'image : `wget` sur `http://127.0.0.1:8080/actuator/health` | point de terminaison public ; les sondes sont activées, donc `/actuator/health/liveness` et `/actuator/health/readiness` existent aussi ; le frontend ne démarre qu'une fois le backend en bonne santé |
| `frontend` | `HEALTHCHECK` de l'image : `fetch` de `/login` | — |
| `docs` | `HEALTHCHECK` de l'image : `/healthz` (nginx) | — |
| `assistant` | `HEALTHCHECK` de l'image : `/api/assistant/health` | le JSON indique Ollama, le modèle de conversation, les embeddings, `indexReady`, le cache des réponses et les sections par site |
| `ollama` | `ollama list` | seulement avec `--profile ollama` ; l'assistant ne l'attend pas et revérifie Ollama toutes les 30 s |

`docker compose up -d --build --wait` rend la main quand tous les conteneurs sont en bonne santé ;
`docker compose ps` affiche l'état de chacun. Quand quelque chose ne démarre pas, lancez le script de diagnostic en
lecture seule `scripts\doctor.ps1` (Windows) ou `scripts/doctor.sh` : il vérifie Docker et sa mémoire, les variables
absentes de `.env`, `docker compose config`, les ports hôte libres, Ollama (l'Ollama de l'ordinateur et ses modèles,
ou la clé et le modèle d'Ollama cloud), la plateforme Maplenou (Bakend sur le port 8080 et `SUPPORT_SERVICE_TOKEN`
quand l'intégration est active), l'état
et la santé de chaque conteneur (avec ses 25 dernières lignes de journal en cas d'échec) et la santé de l'assistant. Chaque service a `restart: unless-stopped` : il redémarre après un
plantage ou un redémarrage de Docker, sauf si vous l'avez arrêté.

Actuator n'expose que `health` (`management.endpoints.web.exposure.include`), et `SecurityConfig` n'autorise
sans authentification que `/actuator/health/**`. N'exposez d'autres points de terminaison que derrière une
authentification.

## Protection, supervision et alertes

| Quoi | Fonctionnement | Réglage |
| --- | --- | --- |
| Limites de débit | par utilisateur connecté (ou par adresse avant la connexion), comptées par minute ; au-delà, `429 Too Many Requests` avec `Retry-After` ; compteur `support_ratelimit_rejected_total` | `RATE_LIMIT_*` |
| Identifiant de requête | chaque réponse porte `X-Request-Id` (repris de la requête s'il fait 8 à 64 caractères sûrs, sinon généré) ; il figure dans chaque ligne de journal (`requestId`) — demandez-le aux utilisateurs qui signalent une erreur | toujours actif |
| Journaux | texte par défaut ; un objet JSON par ligne avec `LOG_FORMAT=ecs`, prêt pour Loki, Elastic ou Datadog. Les journaux tournent (`LOG_MAX_SIZE` × `LOG_MAX_FILES`) | `LOG_FORMAT` |
| Métriques | `/actuator/prometheus` (temps et erreurs HTTP, JVM, pool de connexions, `support_outbox_pending`, `support_outbox_failed`), seulement avec `Authorization: Bearer <METRICS_TOKEN>` ; absent sans jeton | `METRICS_TOKEN` |
| Pile de supervision | `docker compose --profile monitoring up -d` : Prometheus (interroge le backend toutes les 30 s, garde `PROMETHEUS_RETENTION`, 1 Go au plus) avec les règles d'alerte de `monitoring/alerts.yml` (API arrêtée, erreurs serveur, réponses lentes, retard des notifications, événements en échec, nombreux 429, pool de la base saturé) et Grafana sur `http://localhost:GRAFANA_HOST_PORT` (utilisateur `admin`, `GRAFANA_ADMIN_PASSWORD`, Prometheus déjà relié) | `--profile monitoring` |
| Antivirus | avec `ANTIVIRUS=clamav` et `--profile antivirus`, chaque pièce jointe passe par ClamAV avant d'être enregistrée ; un fichier infecté est refusé (`ATTACHMENT_INFECTED`) ; tant que ClamAV ne répond pas, les envois sont refusés (`ANTIVIRUS_UNAVAILABLE`) sauf si `ANTIVIRUS_FAIL_CLOSED=false`. Le premier démarrage télécharge les signatures (quelques minutes) | `ANTIVIRUS*` |
| Pièces jointes privées sur Cloudinary | `ATTACHMENT_STORAGE=cloudinary` : les fichiers sont envoyés comme ressources privées (« authenticated ») dans `CLOUDINARY_FOLDER` ; les téléchargements passent par l'API (contrôle des droits) avec un lien signé valable 5 minutes ; le secret n'arrive jamais au navigateur. Les fichiers locaux existants ne sont pas déplacés : gardez `local` ou copiez-les avant de changer | `ATTACHMENT_STORAGE`, `CLOUDINARY_*` |
| Événements métier | enregistrés dans `outbox_events` avec la modification et livrés juste après la validation ; un relais réessaie ce qui a échoué (plantage, redémarrage). `support_outbox_failed` > 0 signifie qu'un événement a échoué après tous les essais : lisez le journal du backend | toujours actif |
| Fermeture automatique | toutes les 15 minutes, les tickets résolus depuis plus de `SUPPORT_AUTO_CLOSE_AFTER` sont fermés (historique `AUTO_CLOSED`) | `SUPPORT_AUTO_CLOSE_AFTER` |

Vérifier le point de métriques à la main :

```bash
curl -s -H "Authorization: Bearer $METRICS_TOKEN" http://localhost:8081/actuator/prometheus | grep '^support_'
```

```powershell
$t = (Select-String '^METRICS_TOKEN=' .env).Line.Split('=',2)[1]
(Invoke-WebRequest http://localhost:8081/actuator/prometheus -Headers @{ Authorization = "Bearer $t" }).Content -split "`n" | Select-String '^support_'
```

## Journaux et rotation des journaux

Les journaux vont sur la sortie standard ; lisez-les avec Compose :

```bash
docker compose logs -f backend
docker compose logs -f frontend
docker compose --profile docs logs -f assistant ollama
```

Chaque conteneur des deux fichiers Compose utilise le pilote `json-file` avec rotation (ancre `x-logging`) : un
fichier de journal grossit jusqu'à `LOG_MAX_SIZE` (`10m`), puis est remplacé, et `LOG_MAX_FILES` (`3`) fichiers sont
conservés — au plus 30 Mo environ par conteneur avec les valeurs par défaut. Les nouvelles valeurs s'appliquent quand
`docker compose up -d` recrée les conteneurs.

Messages utiles du backend : `Firebase credentials not configured: push notifications are logged only.`,
`Push delivery failed for notification …`, `Unexpected error` (avec la trace de pile, jamais envoyée aux clients).
L'assistant journalise `Loaded {...}` avec le nombre de sections par site, `Ollama expected at <url>` (suivi de
`(cloud, API key set)` avec Ollama cloud), `Embeddings disabled (ASSISTANT_EMBED_MODEL empty): lexical retrieval`
sans modèle d'embeddings, `Pulling <model> into Ollama (first start
only)…` et `Model <model> ready` pendant le téléchargement des modèles, puis `Embeddings ready` une fois le classement
sémantique actif. Les journaux sont en texte brut ; journaux structurés, métriques et traces font partie des
améliorations candidates (observabilité, avant un pilote).

## Sauvegardes

L'état réside dans quatre volumes Compose :

| Volume | Contenu | À sauvegarder ? |
| --- | --- | --- |
| `pgdata` | la base PostgreSQL | oui |
| `attachments` | fichiers envoyés (`/data/attachments` dans le backend) | oui, avec la base (les lignes font référence aux fichiers) |
| `ollama` | modèles téléchargés | non, ils peuvent être retéléchargés |
| `assistant-cache` | embeddings de la documentation | non, ils sont recalculés |

`scripts/backup.sh` (macOS, Linux) et `scripts/backup.ps1` (Windows) sauvegardent la pile Docker en marche dans
`backups/<date>_<heure>/` (ignoré par Git) : un `pg_dump` de la base (`database.sql.gz` avec Bash,
`database.sql.zip` avec PowerShell) et `attachments.tar.gz`. Ils lisent `POSTGRES_USER` et `POSTGRES_DB` dans `.env`.
Seules les **7 sauvegardes les plus récentes** sont conservées ; les plus anciennes sont supprimées, si bien que les
sauvegardes ne peuvent pas remplir le disque.

=== "Bash"

    ```bash
    ./scripts/backup.sh                                     # new backup in backups/<date>_<time>/
    ./scripts/backup.sh restore backups/<date>_<time>       # REPLACES the current data
    ```

=== "PowerShell"

    ```powershell
    .\scripts\backup.ps1
    .\scripts\backup.ps1 -Restore backups\<date>_<time>
    ```

Une restauration supprime et recrée le schéma `public`, charge la sauvegarde, décompresse les pièces jointes dans le
backend et le redémarre. Restaurez une sauvegarde avec le script de la même famille (`.sh` ou `.ps1`) : les formats
de la base sauvegardée diffèrent.

!!! warning "Avant un pilote"
    Les scripts s'exécutent à la demande et écrivent à côté du dépôt. Planifiez-les (cron, Planificateur de tâches),
    copiez les sauvegardes sur une autre machine et testez régulièrement une restauration avant de vous y fier.

## Espace disque

Voyez ce qu'utilise Docker avec `docker system df`. Les scripts de nettoyage libèrent de l'espace sans toucher aux
autres projets :

| Commande | Supprime |
| --- | --- |
| `./scripts/clean.sh` · `.\scripts\clean.ps1` | résultats de build (`backend/target`, `frontend/.next`, rapports Playwright, sites construits, ressources de documentation générées, caches), les conteneurs du projet (la pile est arrêtée), les images orphelines, le cache de build Docker de plus de 7 jours |
| `./scripts/clean.sh --deps` · `.\scripts\clean.ps1 -Deps` | ce qui précède + `frontend/node_modules` et les environnements virtuels Python (`.venv`, `assistant/.venv`) |
| `./scripts/clean.sh --all` · `.\scripts\clean.ps1 -All` | ce qui précède + les volumes Docker des deux fichiers Compose : base, pièces jointes, modèles Ollama, caches — **les données sont perdues** (avec PowerShell, ajoutez `-Deps` pour supprimer aussi les dépendances) |

Les autres limites (rotation des journaux, images multi-étapes, une seule image Ollama, base de test en mémoire,
rétention des sauvegardes) sont listées dans [Cache et espace disque](caching.md).

## Publier la documentation publiquement

Seul le guide utilisateur peut être public :

1. Ne construisez que le site utilisateur : `DOCS_SITES=user` dans `.env` (ou
   `python documentation/build.py --sites user`).
2. Lancez l'assistant avec `ASSISTANT_SITES=user`, pour qu'il n'indexe ni ne réponde sur les sites internes.
3. Définissez `DOCS_PUBLIC_URL` (utilisée pour `ASSISTANT_PUBLIC_BASE_URL`, `ASSISTANT_ALLOWED_ORIGINS` et les liens
   de l'application vers la documentation) avec l'adresse HTTPS publique.
4. Gardez les sites du personnel et des développeurs, et toute instance de l'assistant qui les sert, sur un réseau
   interne.

Voir [Sites de documentation](documentation.md) pour la construction et [Assistant de documentation](assistant.md)
pour le modèle de sécurité de l'assistant.

## Mémoire et CPU d'Ollama

Le service `ollama` (`--profile ollama`) est configuré pour rester léger sur une machine ordinaire :

| Paramètre | Valeur | Effet |
| --- | --- | --- |
| `OLLAMA_KEEP_ALIVE` | `.env`, `10m` | un modèle inutilisé pendant cette durée est déchargé, ce qui libère sa RAM entre les questions |
| `OLLAMA_MAX_LOADED_MODELS` | `1` (fixé dans `docker-compose.yml`) | au plus un modèle en mémoire : le modèle d'embeddings et celui de conversation se relaient |
| `OLLAMA_NUM_PARALLEL` | `1` (fixé dans `docker-compose.yml`) | une requête à la fois, pour une consommation de CPU et de mémoire prévisible |

La première question après une période d'inactivité est plus lente, car le modèle est rechargé. Pour utiliser un GPU
NVIDIA, décommentez le bloc `deploy` du service `ollama` (nécessite le NVIDIA Container Toolkit). Avec Ollama
installé sur l'ordinateur, ces réglages sont ceux de l'application Ollama (variables d'environnement de mêmes noms).
Avec Ollama cloud (par défaut), rien ne tourne en local : mémoire et CPU sont ceux d'ollama.com, et l'offre gratuite
traite une requête à la fois avec une utilisation limitée (voir <https://ollama.com/pricing>).

## Mettre à niveau les modèles Ollama

1. Définissez `ASSISTANT_CHAT_MODEL` et/ou `ASSISTANT_EMBED_MODEL` dans `.env`. Avec Ollama cloud, choisissez un
   modèle de conversation listé par <https://ollama.com/api/tags> et gardez `ASSISTANT_EMBED_MODEL` vide ; les
   étapes 2 à 4 ne font alors que redémarrer l'assistant (rien n'est téléchargé, rien à supprimer).
2. `docker compose --profile docs up -d --build --wait` — `assistant` redémarre avec les nouvelles valeurs,
   télécharge les modèles manquants via l'API d'Ollama et recalcule les embeddings si le modèle d'embeddings a changé
   (la clé du cache inclut le modèle).
3. Vérifiez `curl http://localhost:8006/api/assistant/health` (`chatModel` doit être le nouveau nom, `embeddings: true`).
4. Supprimez les modèles inutilisés : `ollama list` puis `ollama rm <name>` avec l'Ollama de l'ordinateur, ou
   `docker compose exec ollama ollama list` puis `docker compose exec ollama ollama rm <name>` avec le conteneur.

`OLLAMA_IMAGE` vaut `ollama/ollama:latest` par défaut ; fixez une version dans `.env` si vous avez besoin de
déploiements identiques.
