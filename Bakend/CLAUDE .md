# CLAUDE.md — Contexte projet Maplenou (backend)

Ce fichier donne à Claude Code tout le contexte nécessaire pour travailler sur ce projet sans
qu'il ait besoin de redemander les décisions déjà prises. À placer à la racine du repo backend.

---

## 1. Le projet en une phrase

Maplenou est une marketplace mobile trois couches pour l'Afrique de l'Ouest (Togo, Bénin), qui
connecte acheteurs et vendeurs locaux, avec livraison interne et paiement Mobile Money (T-Money,
Flooz via FedaPay) + carte bancaire (Stripe — **actuellement bloqué, voir §7**).

**Ce dépôt couvre le backend uniquement.** Il expose une API REST consommée par :
- une app unique Flutter (web + mobile) pour Client / Vendeur / Livreur (bascule d'interface selon le profil du compte)
- un back-office Next.js séparé, sur un lien distinct, réservé à l'Admin, avec sécurité renforcée

---

## 2. Stack technique

- Java 17, Spring Boot 3.3, Maven
- Spring Security + JWT (jjwt) — auth stateless
- Spring Data JPA + Hibernate + PostgreSQL 16
- Flyway pour les migrations (⚠️ `ddl-auto: validate`, jamais `update` — le schéma vit uniquement dans les migrations SQL versionnées)
- springdoc-openapi / Swagger UI
- Lombok
- Cloudinary (médias), Firebase Admin SDK (notifications FCM), FedaPay (paiement)

---

## 3. Décision structurante n°1 : modèle de compte

**Un compte utilisateur unique, avec des identifiants uniques (téléphone + mot de passe).**
Pas de compte séparé par rôle. Concrètement :

- `User.role` (enum `Role { ADMIN, DELIVERY_AGENT }`) est **nullable**. La grande majorité des
  comptes n'ont pas de rôle explicite — ils sont acheteurs par défaut, sans avoir besoin d'un
  rôle `BUYER`.
- `SellerProfile` est une entité **optionnelle, liée 1-1 à `User`**. Sa présence (avec
  `status = APPROVED`) donne les droits vendeur, sur le même compte, sans duplication.
- `User.getAuthorities()` calcule dynamiquement : `ROLE_USER` toujours, `ROLE_ADMIN` ou
  `ROLE_DELIVERY_AGENT` si `role` est renseigné, et `ROLE_SELLER` si `sellerProfile != null &&
  status == APPROVED`. **Conséquence pratique : tout `@PreAuthorize("hasRole('SELLER')")` marche
  normalement sans logique spéciale**, la résolution se fait entièrement dans `getAuthorities()`.
- Un vendeur peut utiliser son propre livreur, mais cette personne doit être un compte séparé
  avec `role = DELIVERY_AGENT` enregistré sur la plateforme (pas le compte vendeur lui-même).

**Ne jamais réintroduire `BUYER`/`SELLER` comme valeurs de l'enum `Role`** — c'est l'ancien modèle,
explicitement abandonné.

---

## 4. Décision structurante n°2 : commande multi-vendeurs

Un panier (et donc une commande) peut contenir des produits de plusieurs boutiques.

- `Order` = la commande globale, un seul paiement client
- `SubOrder` = une par vendeur concerné, avec son propre statut de préparation/livraison, sa
  propre commission, son propre montant net vendeur
- Un vendeur ne voit et ne modifie **que ses propres `SubOrder`**, jamais celles d'un autre
  vendeur dans la même commande (vérification de propriété systématique, pas juste de rôle)

**Statuts `Order`** : `CREATED → PAID → CLOSED` ; branches `CANCELLED`, `PAYMENT_FAILED`,
`PARTIALLY_REFUNDED`, `REFUNDED`.

**Statuts `SubOrder`** : `PENDING → PREPARING → READY_FOR_PICKUP → IN_DELIVERY → DELIVERED` ;
branches `CANCELLED`, `RETURN_REQUESTED`, `RETURNED`.

---

## 5. Règles métier validées par le chef de projet (à coder telles quelles)

### Commission (barème à deux tranches, pas un taux unique)
- Prix minimum de mise en vente d'un produit : **500 FCFA**
- 500 à 20 000 FCFA inclus → **25 %**
- Plus de 20 000 FCFA → **22,5 %**
- Point encore ouvert : commission sur le produit seul, ou aussi sur les frais de livraison ?
  → **par défaut, calculer sur le sous-total produits uniquement**, dans une méthode isolée
  et facilement ajustable.

### Reversement vendeur
- Hebdomadaire pour la version test.
- Point encore ouvert : déclenché après paiement client ou après livraison confirmée ?
  → **recommandation retenue : après `SubOrder.DELIVERED`**, pas après `PAID` (évite d'avoir à
  réclamer de l'argent à un vendeur en cas de remboursement).

### Retour / remboursement
- Fenêtre de **30 jours après livraison**.
- Entité `ReturnRequest` à créer avec : `delivery_date`, `return_deadline`, `requested_at`,
  `reason`, `product_condition`, `decision`, `decided_at`, `refund_status`.
- Conditions précises de remboursement non définies → décision manuelle vendeur/admin au cas par
  cas pour l'instant.

### Zones de livraison
- Lancement : Grand Lomé, extensible jusqu'à Dagué (Adamavo, Baguida, Avépozo, Kpogan…).
- Entité `DeliveryZone` : `name`, `delivery_fee`, `estimated_time_minutes`, `is_active`, gérable
  dynamiquement depuis l'admin (CRUD complet, pas de valeurs codées en dur).

### Stock
- **Pas de réservation permanente au panier.** Le stock ne bouge qu'à la confirmation du
  paiement.
- Premier paiement confirmé gagne en cas de concurrence sur le dernier exemplaire.
- **Décrémentation obligatoirement atomique** : requête du type
  `UPDATE product_variant SET stock_quantity = stock_quantity - :qty WHERE id = :id AND
  stock_quantity >= :qty`, jamais un `SELECT` puis `UPDATE` séparés.
- Une courte réservation expirante pendant le processus de paiement est recommandée (quelques
  minutes) mais pas encore tranchée formellement.

---

## 6. Ce qui est explicitement hors périmètre pour l'instant

- **Transporteur tiers** (type Colissimo) — mis de côté, livraison 100 % interne pour le moment
- **Messagerie client↔vendeur** avec filtrage anti-contournement — repoussée après stabilisation
  du cœur transactionnel (catalogue → paiement → livraison)
- Cumul de rôles au-delà de Client/Vendeur (ex. un livreur qui serait aussi vendeur) — non prévu

---

## 7. Point bloquant non résolu

**Stripe n'est pas disponible pour un compte marchand basé au Togo.** Le cahier des charges
prévoit Stripe pour le paiement carte, mais ce n'est actuellement pas ouvrable depuis le Togo.
Deux pistes possibles, à trancher avec le chef de projet : ouvrir l'entité légale ailleurs, ou
remplacer Stripe par un agrégateur local supportant aussi le paiement carte (FedaPay le supporte
peut-être déjà, à vérifier). **Ne pas commencer l'intégration carte tant que ce n'est pas tranché.**

---

## 8. État actuel du code (déjà livré)

**Étape 0 — Fondation : terminée.**

- `User`, `Role` (ADMIN/DELIVERY_AGENT), `Address`
- `SellerProfile` (profil vendeur optionnel, statuts `PENDING/APPROVED/SUSPENDED/REJECTED`)
- Auth JWT complète : `/api/auth/register`, `/api/auth/login`, `/api/auth/refresh`
- `/api/sellers/apply`, `/api/sellers/me`
- `/api/users/me` (GET/PUT)
- Module `Category` complet (CRUD admin + lecture publique, slugify, arborescence parent/enfant)
- 4 migrations Flyway : `users`, `addresses`, `seller_profiles`, `categories`
- Sécurité : `SecurityConfig` (JWT stateless, CORS, endpoints publics explicites),
  `GlobalExceptionHandler`, Swagger avec bearer auth

## 9. Roadmap — étapes suivantes (non commencées)

| Étape | Contenu |
|---|---|
| 1 | Catalogue : `Shop`, `Product`, `ProductVariant`, `ProductImage` |
| 2 | `Cart`, `CartItem`, `Favorite` |
| 3 | `Order`, `SubOrder`, `SubOrderItem`, commission à 2 tranches, décrémentation atomique du stock |
| 4 | Paiement FedaPay (+ alternative carte, voir §7), webhooks idempotents |
| 5 | `DeliveryZone`, `Delivery`, affectation, statuts, preuve de livraison |
| 6 | `ReturnRequest` |
| 7 | `ProductReview`, `ShopReview` |
| 8 | `PromoCode` |
| 9 | Notifications (Firebase Admin SDK, centre persistant) |
| 10 | Admin : modération, KPIs, RGPD, droits internes, 2FA |

---

## 10. Contraintes de performance et scalabilité (à respecter dès la première ligne de code, pas en correctif)

- **Aucun endpoint ne renvoie une liste sans pagination.** Jamais de `findAll()` brut exposé.
- Catalogue public (potentiellement des dizaines de milliers de produits) : pagination par
  **keyset/curseur** (basé sur `id`/`created_at`), pas par `OFFSET` qui devient lent à volume.
- Back-office à faible volume (boutiques, catégories) : `Pageable` standard de Spring Data suffit.
- Index composites à prévoir explicitement à chaque nouvelle table selon les filtres réels
  (ex. `(shop_id, status)` sur `Product`, déjà appliqué sur `categories` et `seller_profiles`).
- **Recherche catalogue** : full-text PostgreSQL natif (`tsvector` + index GIN), pas de `LIKE
  '%...%'`. Pas besoin d'Elasticsearch pour le lancement.
- **Cache** : décision actée — **pas de Redis pour l'instant**. On l'introduira seulement quand
  le volume du catalogue le justifiera réellement, pas par anticipation.
- **Rate limiting** : à ajouter sur `/api/auth/**` avant mise en production (pas encore fait).
- Pool de connexions HikariCP à dimensionner explicitement avant la mise en charge, pas laissé aux
  valeurs par défaut.

## 11. RGPD — contrainte de conception dès maintenant

- **Jamais de `ON DELETE CASCADE` entre `User` et les entités liées à des commandes** (`Order`,
  `SubOrder`, factures, avis). Une suppression de compte doit être une **anonymisation
  traçable**, pas une suppression physique — obligation comptable sur les données de commande déjà
  facturées.
- Prévoir dès la conception un endpoint d'export des données personnelles et un flux de demande
  de suppression/anonymisation (portés par l'Admin, §5.10 du cahier).

## 12. Sécurité spécifique au back-office Admin

- L'Admin est un **back-office séparé, sur un lien distinct**, jamais mélangé avec l'app
  Client/Vendeur/Livreur.
- Authentification renforcée exigée par le cahier : **2FA à prévoir**, restriction IP/VPN à
  évaluer — donc un flux d'auth différent (ou des contrôles additionnels) sur `/api/admin/**`,
  pas le même filtre JWT nu que les autres rôles. Non implémenté à ce stade.
- Droits internes différenciés entre comptes admin eux-mêmes à prévoir en étape 10 (ex. un admin
  support ne doit pas pouvoir modifier les taux de commission) — pas un simple rôle `ADMIN`
  unique pour tout.
- Toute action admin sensible (modération, remboursement, changement de taux, changement de
  rôle) doit être tracée dans un journal d'audit avec auteur et horodatage.

## 13. Détail des entités prévues pour les étapes à venir

Pour éviter d'improviser un schéma différent de ce qui a déjà été validé en diagramme ER, voici
les champs prévus pour chaque entité pas encore codée :

- **Shop** : `id`, `owner_id` (→ SellerProfile/User), `name`, `slug` (UK), `description`,
  `logo_url`, `cover_url`, `status` (PENDING/APPROVED/SUSPENDED/REJECTED), `city`, `district`
- **Product** : `id`, `shop_id`, `category_id`, `name`, `slug` (UK), `description`, `base_price`
  (`BigDecimal`, min. 500 FCFA), `status` (DRAFT/ACTIVE/OUT_OF_STOCK/ARCHIVED), `is_deleted`
  (soft delete — jamais de suppression physique d'un produit déjà commandé)
- **ProductVariant** : `id`, `product_id`, `label`, `price_override`, `stock_quantity`, `sku` (UK)
- **ProductImage** : `id`, `product_id`, `url`, `position`
- **Cart / CartItem** : panier actif unique par utilisateur ; lignes potentiellement multi-boutiques
- **Order** : `id`, `buyer_id`, `total_amount`, `status`, `created_at`
- **SubOrder** : `id`, `order_id`, `shop_id`, `subtotal`, `commission_amount`, `net_amount`, `status`
- **SubOrderItem** : `id`, `sub_order_id`, `product_variant_id`, `unit_price`, `quantity`
- **CommissionTier** (remplace l'idée initiale de taux unique) : tranches de prix →
  500–20 000 FCFA inclus = 25 %, > 20 000 FCFA = 22,5 %
- **Payout** : `id`, `shop_id`, `period_start`, `period_end`, `amount`, `status` — déclenché sur
  les `SubOrder` passées `DELIVERED` (voir §5)
- **DeliveryZone** : `id`, `name`, `delivery_fee`, `estimated_time_minutes`, `is_active`
- **Delivery** : `id`, `sub_order_id` (UK), `agent_id`, `status`, `proof_type`
- **ReturnRequest** : voir détail au §5
- **ProductReview** / **ShopReview** : `id`, `product_id`/`shop_id`, `user_id`, `rating`, `comment`
  — liés à un achat vérifié (`SubOrder DELIVERED`)
- **PromoCode** : `id`, `code` (UK), `scope_type` (PLATFORM/SHOP), `scope_id` (nullable),
  `discount_percent`, `is_active`

## 14. Conventions de code à respecter

- Package racine : `com.maplenou.backend` — un sous-package par domaine métier
  (`auth`, `user`, `seller`, `catalog`, futur `order`, `delivery`, etc.), pas par couche technique.
- DTOs en `record` Java, jamais d'entité JPA exposée directement dans une réponse API.
- Vérification de propriété systématique sur toute écriture liée à une ressource (ex. un vendeur
  ne peut modifier que ses propres produits) — jamais uniquement un contrôle de rôle.
- Toute nouvelle table passe par une migration Flyway numérotée (`V5__...`, `V6__...`, jamais de
  modification rétroactive d'une migration déjà appliquée).
- Montants en FCFA : toujours `BigDecimal`, jamais `double`/`float`.
- Commentaires de code en français, cohérents avec le reste du projet.
