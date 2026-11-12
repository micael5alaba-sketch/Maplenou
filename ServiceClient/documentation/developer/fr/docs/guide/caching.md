# Cache et espace disque

Deux objectifs guident chaque cache du projet : **répondre plus vite à l'utilisateur final avec moins de réseau et
moins de travail**, et **ne jamais remplir un disque ni la mémoire** — chaque cache a une taille maximale et une
durée de vie, et ne mélange jamais les données de plusieurs utilisateurs.

## Vue d'ensemble

| Couche | Ce qui est mis en cache | Limite | Invalidation | Où |
| --- | --- | --- | --- | --- |
| Navigateur (application web) | réponses GET de l'API, en mémoire, par onglet | 100 réponses (LRU) | après toute modification (POST/PUT/DELETE), à la déconnexion, au rechargement de la page | `frontend/src/lib/apiCache.ts`, `useApi.ts` |
| Navigateur (fichiers statiques) | `/_next/static/*` de Next.js (noms avec empreinte) | géré par le navigateur | nouveau nom de fichier à chaque build | en-têtes `immutable` par défaut de Next.js |
| Navigateur (push) | rien : `push-sw.js` ne fait qu'afficher les notifications (aucun cache, aucune requête réseau) ; une fois le push activé, le SDK Firebase garde son jeton dans l'IndexedDB — l'exception documentée à « pas d'IndexedDB », imposée par Firebase, retirée quand le push est désactivé | une inscription par navigateur | désactivation du push ; la déconnexion désinscrit le navigateur | `frontend/public/push-sw.js`, `src/lib/push.ts` — voir [Firebase](firebase.md) |
| Navigateur (documentation) | ressources du thème avec empreinte : 1 an ; images/polices : 1 jour ; pages, index de recherche et notre widget : revalidés (`no-cache` + ETag → `304`) | géré par le navigateur | nouveau build | `documentation/nginx.conf.template` |
| Réseau | gzip : JSON de l'API > 1 Ko (Spring), documentation précompressée au build (`gzip_static`), réponses Next.js | — | — | `application.yml`, `documentation/Dockerfile` |
| Backend | données de référence : catégories, vocabulaire (statuts, transitions, priorités, droits), calendrier et objectifs SLA, modèles de notification — un instantané par service | un petit objet chacun, rechargé après `REFERENCE_CACHE_TTL` (10 min) | abandonné **après la validation** d'une modification d'un administrateur | `ticket/application/CategoryService`, `VocabularyService`, `sla/application/SlaService`, `notification/application/TemplateService` |
| Backend | compteurs des limites de débit (par utilisateur ou adresse, une minute) | 50 000 fenêtres, 2 min (Caffeine) | expirent seuls | `platform/observability/RateLimitFilter` |
| HTTP (API) | `GET /categories` et `GET /vocabulary` : `Cache-Control: private, max-age=300` + `ETag` → `304 Not Modified` | par client | le hachage du contenu change | `ticket/adapter/in/web/CategoryController`, `VocabularyController` |
| Assistant | résultats de la recherche du centre d'aide (`GET /api/assistant/search`) : `Cache-Control: public, max-age=60` (guide utilisateur public seulement) | géré par le navigateur, 1 min | — | `assistant/app/main.py` |
| Assistant | réponses à une même question (même site et même langue) | 256 réponses, 1 h (`ASSISTANT_ANSWER_CACHE_*`) | rechargement des index | `assistant/app/cache.py` |
| Assistant | embeddings des sections, sur disque (seulement avec un modèle d'embeddings, donc pas avec Ollama cloud par défaut) | un fichier par site/langue/modèle/empreinte du contenu | nouvelle documentation ou nouveau modèle | volume `assistant-cache` |
| Ollama (local seulement) | modèle chargé en RAM | 1 modèle (`OLLAMA_MAX_LOADED_MODELS`), déchargé après `OLLAMA_KEEP_ALIVE` | temps d'inactivité | `docker-compose.yml` |
| Build Docker | caches Maven, npm, uv et Next.js en **cache mounts** BuildKit | cache de build Docker | `docker builder prune` | les quatre Dockerfile |

Ce qui n'est **jamais** mis en cache : les données des tickets côté serveur (elles doivent toujours être à jour), les
réponses authentifiées de l'API dans le cache HTTP du navigateur (`no-store`), tout ce qui serait partagé entre
utilisateurs.

## Espace disque

| Source | Comment elle reste petite |
| --- | --- |
| Images Docker | builds multi-étapes ; les images d'exécution ne contiennent que ce qui tourne : JRE (pas JDK) sur Alpine pour le backend, le serveur standalone de Next.js, un environnement virtuel Python sans outils de build, nginx Alpine pour la documentation ; les images de base sont partagées entre services et tests (`postgres:16-alpine`, `node:22-alpine`, `eclipse-temurin:21-jdk-noble`, `python:3.12-slim-bookworm`) |
| Contexte de build | `.dockerignore` dans chaque dossier (liste d'autorisation à la racine du dépôt) |
| Couches d'image | jar Spring Boot en couches : une modification du code renvoie ~1 Mo, pas les 127 Mo de bibliothèques |
| Journaux des conteneurs | rotation : `LOG_MAX_SIZE` × `LOG_MAX_FILES` par conteneur (10 Mo × 3 par défaut) |
| Sites de documentation | construits une seule fois, dans l'image docs ; l'assistant lit leurs index en HTTP (pas de second build) |
| Ollama | par défaut : Ollama cloud (`OLLAMA_URL=https://ollama.com`) — pas d'image Ollama, **aucun modèle sur le disque**, pas de cache d'embeddings (`ASSISTANT_EMBED_MODEL` vide) ; avec l'Ollama installé sur l'ordinateur, les modèles déjà téléchargés sont réutilisés ; le mode conteneur (`--profile ollama`) coûte plusieurs Go de disque (l'image `ollama/ollama` plus environ 2,3 Go de modèles dans le volume `ollama`) — voir [Ollama : cloud, sur l'ordinateur ou dans Docker](assistant.md#ollama-cloud-sur-lordinateur-ou-dans-docker) |
| Base de test | en mémoire (`tmpfs`) |
| Sauvegardes | `scripts/backup.*` garde les 7 plus récentes |

### Faire le ménage

```powershell
.\scripts\clean.ps1          # build outputs, stopped containers, dangling images, build cache older than 7 days
.\scripts\clean.ps1 -Deps    # + node_modules and Python virtual environments
.\scripts\clean.ps1 -All     # + Docker volumes: database, attachments, Ollama models (data is lost)
docker system df             # what Docker uses
```

Sous macOS/Linux, utilisez `./scripts/clean.sh [--deps|--all]`.

## Règles pour le nouveau code

1. Ne mettez en cache que des données lues souvent et modifiées rarement ; jamais de données propres à un utilisateur
   côté serveur.
2. Chaque cache a une taille maximale et une durée de vie ; invalidez-le une fois la modification **commitée**.
3. Préférez la validation HTTP (`ETag`, `Cache-Control`) pour les données que les clients peuvent conserver.
4. Mesurez : l'assistant expose les succès du cache dans `/api/assistant/health` ; les métriques du backend sont sur
   `/actuator/prometheus`.
