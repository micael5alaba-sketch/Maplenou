# Tester l'API avec Swagger UI et Postman

Trois outils appellent la même API REST. Tous ont besoin de la pile lancée avec le profil **demo**
(`SPRING_PROFILES_ACTIVE=demo` dans `.env`, la valeur par défaut), qui fournit les personas de démonstration et un
point d'accès pour obtenir un jeton.

| Outil | Idéal pour | Où |
| --- | --- | --- |
| **Swagger UI** | découvrir chaque point d'accès, ses paramètres et ses réponses ; essayer un appel | <http://localhost:8081/swagger-ui.html> |
| **Postman** | dérouler tout le scénario avec des vérifications automatiques ; garder ses propres requêtes | `backend/http/postman/` |
| **REST Client / client HTTP d'IntelliJ** | le même scénario depuis l'éditeur de code | `backend/http/support-api.http` |

Le port est `BACKEND_HOST_PORT` dans `.env` (par défaut `8081` ; 8080 est laissé à Bakend, l'API cœur de Maplenou).

## Obtenir un jeton (profil demo)

L'API attend `Authorization: Bearer <jeton>`. Avec le profil demo, `POST /api/v1/dev/token` en renvoie un pour une
persona de démonstration :

| Persona | Rôle | `userId` |
| --- | --- | --- |
| Jean K. | client | `11111111-1111-4111-8111-111111111111` |
| Sophie N. | agent support | `44444444-4444-4444-8444-444444444444` |
| Amina K. | responsable support | `66666666-6666-4666-8666-666666666666` |

`GET /api/v1/dev/users` liste les sept personas (vendeur, livreur, administrateur…). Ces deux points d'accès
n'existent pas en dehors du profil demo.

## Swagger UI

1. Ouvrez <http://localhost:8081/swagger-ui.html>.
2. Section **Demo login** (profil demo uniquement) → `POST /api/v1/dev/token` → **Try it out** → corps
   `{"userId": "11111111-1111-4111-8111-111111111111"}` → **Execute**. Copiez `accessToken` dans la réponse.
3. Cliquez sur **Authorize** (cadenas, en haut à droite), collez le jeton (sans `Bearer `), **Authorize**, **Close**.
4. N'importe quel point d'accès → **Try it out** → **Execute**. Les erreurs reviennent en
   `application/problem+json` avec un `code` stable (par exemple `CRITICAL_PRIORITY_RESERVED`).

La description lisible par machine est <http://localhost:8081/v3/api-docs> (aussi versionnée dans
`docs/architecture/openapi.json`). Les jetons expirent après la durée de vie configurée : demandez-en un nouveau quand
les appels renvoient `401`.

## Postman

### Importer

1. Postman → **Import** → sélectionnez les deux fichiers de `backend/http/postman/` :
    - `Maplenou-Support-API.postman_collection.json` — 28 requêtes dans 7 dossiers, avec des vérifications ;
    - `Maplenou-local.postman_environment.json` — `baseUrl = http://localhost:8081`.
2. Sélectionnez l'environnement **Maplenou — local (Docker)** (en haut à droite).

### Exécuter

- **Tout le scénario :** clic droit sur la collection → **Run collection** → **Run**. Attendu : 28 requêtes,
  36 vérifications réussies. Chaque exécution crée un nouveau ticket.
- **Une requête à la fois :** exécutez d'abord le dossier **0 · Setup** (il enregistre les trois jetons), puis
  n'importe quelle requête. Les variables (`jeanToken`, `ticketId`, `categoriesEtag`, `reopenId`…) sont remplies par
  les scripts de test et visibles dans l'onglet **Variables** de la collection.

| Dossier | Ce qu'il montre |
| --- | --- |
| 0 · Setup | santé, personas de démonstration, jetons de Jean, Sophie et Amina |
| 1 · Requester | catégories avec `ETag` puis `304` (cache), commandes, création de ticket, `422` sur la priorité Critique |
| 2 · Support | file d'attente, assignation, note interne, réponse, contexte de l'écosystème |
| 3 · Requester follow-up | conversation sans la note interne, notifications |
| 4 · Resolution and reopening | résolution, demande de réouverture et approbation, historique |
| 5 · Manager | tableau de bord, rapport, `403` pour un client |
| 6 · Push notifications | canal push, enregistrement d'appareil, notification push de test, désinscription (voir [Firebase](firebase.md)) |

Autre serveur : dupliquez l'environnement et changez `baseUrl`. Vous pouvez aussi générer une collection complète à
partir de la description OpenAPI : **Import → Link → `http://localhost:8081/v3/api-docs`** (sans scénario ni
vérifications).

### Ligne de commande (CI ou vérification rapide)

```bash
npx newman run backend/http/postman/Maplenou-Support-API.postman_collection.json \
  -e backend/http/postman/Maplenou-local.postman_environment.json
```

## Erreurs courantes

| Réponse | Signification |
| --- | --- |
| `401` | pas de jeton, jeton erroné ou expiré → obtenez-en un nouveau |
| `403 FORBIDDEN` | le rôle de la persona ne permet pas cette action (par exemple un client sur `/dashboard`) |
| `404` sur `/api/v1/dev/token` | le backend ne tourne pas avec le profil demo |
| `409 CONCURRENT_MODIFICATION` | le ticket a changé entre-temps → relisez-le |
| `422 <CODE>` | une règle métier a refusé la modification ; `rule` la nomme (BR-xxx) |
| Connexion refusée | le backend ne tourne pas ou utilise un autre port → `docker compose ps`, `scripts/doctor.ps1` |
