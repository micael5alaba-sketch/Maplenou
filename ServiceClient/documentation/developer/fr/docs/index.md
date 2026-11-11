# Service client Maplenou

Voici la documentation développeur du MVP Service client et gestion des tickets de Maplenou : une API Spring Boot,
une application web Next.js, trois sites de documentation (utilisateur, personnel, développeur) en français et en
anglais, et un assistant de documentation fondé sur Ollama (Ollama cloud par défaut). Ce site contient le **guide du développeur**
(lancer, modifier, tester et exploiter le code). Le **référentiel du projet** (exigences, UX/UI, architecture,
validation, gouvernance) est tenu en anglais : voir [Référentiel du projet (anglais)](records.md).

## Objectif actuel

Construire le premier MVP fonctionnel du Service client et de la gestion des tickets.

**Statut :** CS-01 et CS-02 approuvés · CS-03 → CS-09 réalisés et validés, en attente d'approbation du responsable du projet.

## Guide du développeur

Commencez ici si vous découvrez le code. Toute la pile fonctionne avec Docker seul.

| Page | Contenu |
| --- | --- |
| [Démarrage](guide/getting-started.md) | Lancer l'application, la documentation et l'assistant avec Docker ; ports ; outils natifs facultatifs |
| [Visite du dépôt](guide/repository.md) | Contenu de chaque dossier et où placer les nouveautés |
| [Architecture](guide/architecture.md) | Monolithe modulaire, ports et adaptateurs, BFF, événements après commit |
| [Backend](guide/backend.md) | Paquets, flux d'une requête, erreurs, Flyway, cache, sécurité, notifications, ajout d'un endpoint |
| [Traitements du backend](guide/backend-processing.md) | Une requête pas à pas, les flux métier, tests manuels de l'API |
| [Tester l'API (Swagger, Postman)](guide/api-testing.md) | Swagger UI avec un jeton de démonstration, la collection Postman et newman |
| [Notifications push Firebase](guide/firebase.md) | Brancher un projet Firebase, push dans le navigateur, applications Flutter, dépannage |
| [Intégration à la plateforme Maplenou](guide/maplenou-integration.md) | ServiceClient dans le dépôt Maplenou : connexion avec les comptes Maplenou, rôles support, vraies commandes de Bakend, import des anciens tickets, déploiement |
| [Frontend](guide/frontend.md) | Structure App Router, proxy BFF, chargement des données, FR/EN et thèmes, accessibilité, ajout d'un écran |
| [Sites de documentation](guide/documentation.md) | Les six sites Zensical, `build.py`, règles de rédaction, publication |
| [Assistant de documentation](guide/assistant.md) | L'assistant RAG Ollama : recherche, API, configuration, Ollama cloud, sur l'ordinateur ou dans Docker, dépannage, cache des réponses, sécurité |
| [Tests](guide/testing.md) | Commandes pour vérifier le projet avec ou sans Docker, test de fumée |
| [Cache et espace disque](guide/caching.md) | Chaque cache et ses limites, des images Docker et des disques peu remplis, nettoyage |
| [Exploitation](guide/operations.md) | Variables de `.env`, secrets, santé, journaux, sauvegardes, espace disque, publication, mises à niveau |
| [Contribuer](guide/contributing.md) | Démarche, commits, définition de « terminé », maîtrise des changements |
| [Agents IA](guide/ai-agents.md) | Comment les agents de code IA travaillent dans ce dépôt et le reproduisent |

```bash
cp .env.example .env                                 # once (Windows: copy .env.example .env)
docker compose up -d --build --wait                  # app http://localhost:3001 · API http://localhost:8081
docker compose --profile docs up -d --build --wait   # + documentation http://localhost:8006 and its assistant
scripts/doctor.sh                                    # something does not start? (Windows: scripts\doctor.ps1)
./scripts/test.sh all                                # every test suite in containers
```

## Poursuivre le développement

Commencez par ces documents du référentiel (en anglais) :

1. [Project Continuity](/developer/en/project/PROJECT-CONTINUITY/)
2. [Development Matrix](/developer/en/project/DEVELOPMENT-MATRIX/)
3. [Project Plan](/developer/en/project/PROJECT-PLAN/)
4. [Architecture](/developer/en/architecture/) et [decisions](/developer/en/architecture/decisions/)
5. [References and Conventions](/developer/en/governance/REFERENCES/)
6. [Reproduction Guide](/developer/en/governance/REPRODUCTION/)
7. [Upgrade Plan](/developer/en/governance/UPGRADE-PLAN/)
