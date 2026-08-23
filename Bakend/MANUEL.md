# Manuel d'utilisation — Backend Maplenou

Ce document donne à l'équipe projet tout ce qu'il faut pour démarrer, comprendre et faire évoluer
le backend : installation, configuration, modules, rôles, tests, état d'avancement et déploiement.

---

## 1. Vue d'ensemble

Maplenou est une marketplace mobile pour le Togo/Bénin : des vendeurs tiers vendent à des clients,
livrés par des livreurs internes à la plateforme. Ce dépôt est **le backend uniquement** — une API
REST consommée par une app Flutter unique (Client/Vendeur/Livreur, bascule d'interface selon le
rôle du compte) et un back-office Next.js séparé pour l'Admin.

**Package racine du code** : `com.maplenou.backend`, organisé par domaine métier (pas par couche
technique) dans `src/main/java/com/maplenou/backend/`.

---

## 2. Stack technique

| Composant | Techno |
|---|---|
| Langage / build | Java 21, Maven |
| Framework | Spring Boot 4.1 |
| Sécurité | Spring Security + JWT (jjwt 0.12.6), stateless |
| Base de données | PostgreSQL 16, migrations Flyway (32 fichiers à ce jour) |
| Cache / sessions courtes | Redis (blacklist JWT, rate limiting, verrouillage compte, cache catégories/zones) |
| Documentation API | springdoc-openapi (Swagger UI) |
| Médias | Cloudinary (upload signé côté client) |
| Notifications | Firebase Admin SDK (push FCM) |
| 2FA | TOTP (`dev.samstevens.totp`) |

---

## 3. Prérequis pour lancer le projet en local

1. **Java 21** installé
2. **PostgreSQL 16+** démarré, avec une base `maplenou_db` (créée automatiquement si elle
   n'existe pas selon votre install, sinon `CREATE DATABASE maplenou_db;`)
3. **Redis** démarré sur le port 6379 — **indispensable**, l'authentification entière en dépend
   (rate limiting, blacklist JWT, verrouillage compte). Sans lui, le login/register/2FA renvoient
   des 403/500 qui ressemblent à un bug applicatif mais qui n'en sont pas.
   - En local sans Redis installé : télécharger un binaire portable (ex. releases
     [tporadowski/redis](https://github.com/tporadowski/redis/releases), fichier `.zip`) et lancer
     `redis-server.exe --port 6379` depuis le dossier extrait. Ne persiste pas après fermeture —
     pour un usage durable, installer Redis comme service Windows ou utiliser Docker/WSL.
4. Le wrapper Maven est inclus (`mvnw` / `mvnw.cmd`), pas besoin d'installer Maven séparément.

---

## 4. Configuration (variables d'environnement)

Toutes les valeurs sensibles ont un **fallback de développement** codé dans
`src/main/resources/application.properties` (fonctionne sans rien configurer en local), mais
**doivent être surchargées en production** via ces variables d'environnement :

| Variable | Rôle | Défaut dev |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Connexion PostgreSQL | `localhost:5432/maplenou_db`, `postgres` |
| `JWT_SECRET` | Clé de signature JWT (256 bits min., sinon l'appli refuse de démarrer) | clé de dev incluse |
| `WEBHOOK_SECRET` | Secret partagé pour valider les webhooks de paiement | secret de dev |
| `CORS_ALLOWED_ORIGINS` | Origines autorisées (front Flutter web + Next.js admin) | `localhost:3000,localhost:4000` |
| `SWAGGER_ENABLED` | `false` en production pour masquer la doc API publique | `true` |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, `REDIS_SSL_ENABLED` | Connexion à une instance Redis managée (Upstash, Redis Cloud, AWS ElastiCache…) | `localhost:6379`, sans mot de passe, sans TLS |
| `AUTH_RATE_LIMIT_PER_MINUTE` | Nb de requêtes/min autorisées sur `/api/auth/**` par IP | `10` |
| `CLOUDINARY_URL` | Format `cloudinary://<api_key>:<api_secret>@<cloud_name>` | placeholder non fonctionnel |
| `COLISSIMO_ENABLED`, `COLISSIMO_CONTRACT_NUMBER`, `COLISSIMO_API_KEY` | Intégration transporteur (voir §8, non fonctionnelle tant que désactivée) | désactivé |

Le compte de service Firebase doit être placé dans
`src/main/resources/firebase-service-account.json` (fichier réel téléchargé depuis la console
Firebase — actuellement présent mais à vérifier qu'il s'agit bien du vrai projet en production).

---

## 5. Démarrer le serveur en local

```bash
# Windows (PowerShell ou Git Bash)
./mvnw.cmd spring-boot:run

# Linux/Mac
./mvnw spring-boot:run
```

Le serveur démarre sur `http://localhost:8080`. Au démarrage, Flyway applique automatiquement
toutes les migrations manquantes (le schéma vit **uniquement** dans les fichiers de migration,
`ddl-auto=validate` — ne jamais passer à `update` ou `create`).

**Documentation API interactive** une fois lancé : `http://localhost:8080/swagger-ui.html`

---

## 6. Compte super admin (pré-créé)

| Champ | Valeur |
|---|---|
| Téléphone | `+22890000000` |
| Mot de passe | `Micael2005@` |

Utiliser `POST /api/auth/login` avec ces identifiants pour obtenir un token avec le rôle `ADMIN`
et accéder à tous les endpoints `/api/admin/**`.

---

## 7. Rôles et modèle de compte

**Un seul compte par utilisateur, pas de compte séparé par rôle.** `User.role` (enum `ADMIN` /
`DELIVERY_AGENT`) est nullable — la grande majorité des comptes n'ont pas de rôle explicite : ils
sont acheteurs par défaut. Le statut vendeur n'est pas un rôle mais un profil optionnel
(`SellerProfile`, 1-1 avec `User`) : dès qu'il existe avec `status = APPROVED`, le compte obtient
les droits vendeur **sur le même compte**, sans double inscription.

Autorités calculées dynamiquement (`User.getAuthorities()`) :
- `ROLE_USER` toujours
- `ROLE_ADMIN` ou `ROLE_DELIVERY_AGENT` si `role` est renseigné
- `ROLE_SELLER` si `sellerProfile != null && status == APPROVED`

Un rôle `DELIVERY_AGENT` ne peut être attribué que par un admin (`PATCH
/api/admin/users/{id}/role`) — il n'y a pas d'auto-inscription livreur.

---

## 8. Modules du backend

| Package | Rôle |
|---|---|
| `auth` | Inscription, connexion, refresh token, logout, 2FA TOTP (setup/activate/verify-login/disable) |
| `user` | Profil (`/api/users/me`), adresses, gestion admin des utilisateurs (liste/ban/rôle) |
| `seller` | Candidature vendeur, validation admin (PENDING/APPROVED/SUSPENDED/REJECTED) |
| `catalog` | Catégories, boutiques (`Shop`), produits, variantes, images |
| `cart` | Panier, favoris |
| `order` | Commandes, sous-commandes par boutique, commission à 2 tranches, webhook de paiement |
| `delivery` | Zones de livraison, affectation livreur, statuts, preuve de livraison, navigation |
| `returns` | Demandes de retour (fenêtre 30 jours), décision vendeur/admin, restauration de stock |
| `review` | Avis produits et boutiques (liés à un achat livré vérifié) |
| `promo` | Codes promo plateforme ou boutique, validation avant commande |
| `payout` | Reversements vendeurs (cron hebdomadaire lundi 2h UTC + génération manuelle admin) |
| `admin` | Dashboard KPI, RGPD (anonymisation/auto-suppression) |
| `audit` | Journal des actions sensibles (qui a fait quoi, quand) |
| `notification` | Tokens FCM pour notifications push |
| `security` | JWT, blacklist Redis, rate limiting, verrouillage compte |
| `media` | Upload signé Cloudinary, suppression modérée |
| `messaging` | Messagerie client↔boutique et utilisateur↔support, filtre anti-coordonnées personnelles |
| `content` | Pages de contenu statiques (CGU/CGV/FAQ) |
| `newsletter` | Capture d'emails (pas d'envoi automatique) |
| `shipping` | Scaffold transporteur tiers (Colissimo) — voir §10 |

---

## 9. Base de données

- 32 migrations Flyway (`src/main/resources/db/migration/V1__...` à `V32__...`)
- **Ne jamais modifier une migration déjà appliquée** — toujours en créer une nouvelle, numérotée
- Montants en FCFA : toujours `BigDecimal`, jamais `double`/`float`
- Anonymisation plutôt que suppression physique pour les comptes liés à des commandes (obligation
  comptable + RGPD) — voir `GdprService`

---

## 10. Ce qui est fait / pas fait

**Fait et testé** : auth/2FA, catalogue, panier, commandes/paiement (webhook générique),
livraison interne, retours, avis, codes promo, reversements, KPI admin, RGPD, audit,
notifications push, upload médias Cloudinary, messagerie, pages CMS, capture newsletter.

**Explicitement reporté** (décision assumée avec le porteur du projet) :
- **Paiement réel FedaPay/Stripe** : le webhook actuel réagit à une confirmation déjà faite par
  une passerelle externe, mais aucune intégration FedaPay/Stripe n'existe (pas de SDK, pas
  d'initiation de paiement, pas de split automatique vendeur). `PaymentWebhookController` contient
  un `TODO` explicite pour remplacer le secret partagé par une vérification HMAC-SHA256 FedaPay.
- **Transporteur tiers (Colissimo)** : interface `CarrierShippingClient` posée et branchée dans le
  flux vendeur (`POST /api/shops/mine/sub-orders/{id}/shipping/label`), mais désactivée par défaut
  (`COLISSIMO_ENABLED=false`) — répond avec une erreur claire tant qu'un vrai contrat n'est pas
  fourni.
- **Newsletter** : capture d'emails uniquement, aucun envoi (l'outil d'emailing sera choisi plus
  tard).

**Non commencé** :
- CI/CD (les tests ne se lancent pas automatiquement sur push/PR)
- Tests de charge

---

## 11. Lancer les tests

**Prérequis : PostgreSQL ET Redis démarrés localement** (ce projet n'utilise pas Testcontainers —
les tests tournent contre les mêmes services réels que l'application).

```bash
./mvnw.cmd test                                    # toute la suite
./mvnw.cmd test "-Dtest=NomDeLaClasse"              # une seule classe
```

- **Tests unitaires** (rapides, pas de serveur HTTP) : `PersonalDataFilterTest`,
  `CloudinaryUrlUtilsTest`, `TotpServiceTest`, `JwtServiceTest`, `CommissionServiceTest`,
  `PromoCodeServiceTest` (Mockito)
- **Tests d'intégration** (démarrent un vrai serveur, appels HTTP réels) dans
  `src/test/java/com/maplenou/backend/integration/` : `AuthAndUserIntegrationTest`,
  `CatalogAndCartIntegrationTest`, `OrderLifecycleIntegrationTest` (parcours complet
  commande→paiement→livraison→avis→retour→payout), `NewFeaturesIntegrationTest`

État actuel : **69 tests, tous verts**, couvrant les 31 contrôleurs du projet.

---

## 12. Déploiement / hébergement

1. Provisionner PostgreSQL et Redis managés (ex. Redis : Upstash, Redis Cloud, AWS ElastiCache —
   toute instance hors `localhost` nécessite `REDIS_SSL_ENABLED=true` si elle exige TLS)
2. Définir toutes les variables d'environnement du §4 (ne jamais laisser les valeurs de dev en
   production, notamment `JWT_SECRET` et `DB_PASSWORD`)
3. `SWAGGER_ENABLED=false` en production
4. Fournir le vrai `firebase-service-account.json` du projet Firebase de production
5. Build : `./mvnw.cmd clean package` produit un jar exécutable dans `target/`

---

## 13. Dépannage courant

| Symptôme | Cause probable |
|---|---|
| Login/register renvoient 403 vide ou 500 | Redis n'est pas démarré |
| `IllegalStateException: app.jwt.secret doit faire au moins 256 bits` au démarrage | `JWT_SECRET` trop court (< 32 caractères) |
| `Web server failed to start... Port 8080 was already in use` | Une instance précédente tourne encore — l'arrêter avant de relancer |
| 429 `Trop de tentatives` pendant des tests répétés | Rate limit `/api/auth/**` atteint (10/min par défaut) — attendre 1 min ou augmenter `AUTH_RATE_LIMIT_PER_MINUTE` en dev |
| Upload média échoue en 503 | `CLOUDINARY_URL` non configuré (normal tant que le compte Cloudinary réel n'est pas branché) |
| Appel transporteur échoue en 503 | Colissimo désactivé (`COLISSIMO_ENABLED=false`, normal tant qu'aucun contrat réel) |
