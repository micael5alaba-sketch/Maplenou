# CLAUDE.md — Contexte projet Maplenou (backend)

Ce fichier donne à Claude Code tout le contexte nécessaire pour travailler sur ce projet sans
qu'il ait besoin de redemander les décisions déjà prises. À placer à la racine du repo backend.

---

## 1. Le projet en une phrase

Maplenou est une marketplace mobile trois couches pour l'Afrique de l'Ouest (Togo, Bénin), qui
connecte acheteurs et vendeurs locaux, avec livraison interne. Le paiement cible à terme est du
Mobile Money (T-Money, Flooz) via un agrégateur comme FedaPay + carte bancaire (Stripe étant
bloqué pour un compte marchand togolais) — **mais l'intégration réelle est reportée dans son
ensemble pour l'instant, voir §7**.

**Ce dépôt couvre le backend uniquement.** Il expose une API REST consommée par :
- une app unique Flutter (web + mobile) pour Client / Vendeur / Livreur (bascule d'interface selon le profil du compte)
- un back-office Next.js séparé, sur un lien distinct, réservé à l'Admin, avec sécurité renforcée

---

## 2. Stack technique

- Java 21, Spring Boot 4.1, Maven
- Spring Security + JWT (jjwt 0.12.6) — auth stateless, 2FA TOTP disponible sur tous les comptes
- Spring Data JPA + Hibernate + PostgreSQL 16
- Flyway pour les migrations (⚠️ `ddl-auto: validate`, jamais `update` — le schéma vit uniquement dans les migrations SQL versionnées)
- Redis (Lettuce) : blacklist JWT au logout, rate limiting sur `/api/auth/**`, verrouillage de
  compte après 5 échecs, cache des catégories/zones de livraison — **indispensable au démarrage**,
  pas une optimisation future (voir §10)
- springdoc-openapi / Swagger UI
- Lombok
- Cloudinary (médias, upload signé côté client), Firebase Admin SDK (notifications FCM)
- **Paiement (FedaPay/Stripe) explicitement reporté** — voir §7, mis à jour

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

- **Transporteur tiers** (Colissimo) : scaffold posé (`shipping/`, interface
  `CarrierShippingClient`) mais désactivé par défaut (`COLISSIMO_ENABLED=false`) — pas de vrai
  contrat/webservice branché, pas un simple "pas commencé"
- **Paiement réel FedaPay/Stripe** — voir §7, reporté explicitement par le porteur du projet
- Cumul de rôles au-delà de Client/Vendeur (ex. un livreur qui serait aussi vendeur) — non prévu

*(La messagerie client↔vendeur, initialement prévue ici comme repoussée, est en réalité déjà
implémentée — voir §8.)*

---

## 7. Paiement — état réel (mis à jour)

Le point bloquant Stripe (compte marchand indisponible au Togo) n'a pas été résolu — la décision
prise a été de **reporter l'intégration de paiement dans son ensemble**, pas seulement Stripe.
Concrètement aujourd'hui :

- Aucun SDK FedaPay ni Stripe dans `pom.xml`, aucune initiation de paiement, aucun split
  automatique vendeur.
- `PaymentWebhookController`/`PaymentWebhookService` ne font que **réagir à un webhook générique
  déjà confirmé** par une passerelle externe hypothétique (signature vérifiée via un secret
  partagé, `MessageDigest.isEqual` pour éviter les attaques par timing) — il n'y a rien en amont
  qui déclenche un vrai paiement.
- **Ne pas commencer l'intégration réelle tant que le porteur du projet n'a pas retranché la
  question Stripe vs agrégateur local (FedaPay ou autre) sur ce point précis.**

---

## 8. État actuel du code (déjà livré)

Ce qui suit (§1-9 de la roadmap originale) est **déjà codé et testé**, pas une roadmap à venir —
seul le paiement réel (§7) reste en dehors du périmètre actuel :

auth JWT + 2FA TOTP, profil/adresses, profil vendeur (`SellerProfile`), catalogue complet
(`Shop`/`Category`/`Product`/`ProductVariant`/`ProductImage`), panier/favoris, commandes
multi-vendeurs (`Order`/`SubOrder`/`SubOrderItem`) avec commission à 2 tranches, livraison interne
(`DeliveryZone`/`Delivery`), retours (`ReturnRequest`), avis (`ProductReview`/`ShopReview`), codes
promo (`PromoCode`), reversements vendeurs (`Payout`, cron hebdomadaire), notifications push
Firebase, admin (KPI, RGPD, audit), messagerie client↔vendeur/support avec filtrage
anti-coordonnées personnelles, pages CMS, capture newsletter, upload média Cloudinary.

Pour la liste exacte des modules et de ce qui est fait/pas fait à une date donnée, se référer à
`MANUEL.md` (§9 et §11) plutôt qu'à ce fichier — il est mis à jour à chaque changement de
périmètre, contrairement à ce `CLAUDE.md` qui décrit surtout les **décisions structurantes**
(pourquoi, pas l'état d'avancement).

---

## 9. Contraintes de performance et scalabilité (à respecter dès la première ligne de code, pas en correctif)

- **Aucun endpoint ne renvoie une liste sans pagination.** Jamais de `findAll()` brut exposé.
- Catalogue public (potentiellement des dizaines de milliers de produits) : pagination par
  **keyset/curseur** (basé sur `id`/`created_at`), pas par `OFFSET` qui devient lent à volume.
- Back-office à faible volume (boutiques, catégories) : `Pageable` standard de Spring Data suffit.
- Index composites à prévoir explicitement à chaque nouvelle table selon les filtres réels
  (ex. `(shop_id, status)` sur `Product`, déjà appliqué sur `categories` et `seller_profiles`).
- **Recherche catalogue** : full-text PostgreSQL natif (`tsvector` + index GIN), pas de `LIKE
  '%...%'`. Pas besoin d'Elasticsearch pour le lancement.
- **Cache / Redis** : la décision initiale ("pas de Redis pour l'instant") a été révisée — Redis
  est en réalité **déjà utilisé et indispensable au démarrage** (blacklist JWT, rate limiting,
  verrouillage de compte, cache des catégories/zones). Sans lui en local, l'authentification
  échoue en 403/500 silencieux (voir `MANUEL.md` §14, dépannage).
- **Rate limiting** : fait — `/api/auth/**` limité (défaut 10/min/IP, configurable via
  `AUTH_RATE_LIMIT_PER_MINUTE`).
- Pool de connexions HikariCP : dimensionné dans `application.properties`
  (`hikari.maximum-pool-size`/`minimum-idle`), à ajuster avant une vraie montée en charge.

## 10. RGPD — contrainte de conception dès maintenant

- **Jamais de `ON DELETE CASCADE` entre `User` et les entités liées à des commandes** (`Order`,
  `SubOrder`, factures, avis). Une suppression de compte doit être une **anonymisation
  traçable**, pas une suppression physique — obligation comptable sur les données de commande déjà
  facturées.
- Prévoir dès la conception un endpoint d'export des données personnelles et un flux de demande
  de suppression/anonymisation (portés par l'Admin, §5.10 du cahier).

## 11. Sécurité spécifique au back-office Admin

- L'Admin est un **back-office séparé, sur un lien distinct**, jamais mélangé avec l'app
  Client/Vendeur/Livreur.
- 2FA TOTP existe désormais **au niveau compte, pour tout utilisateur** (module `auth`,
  setup/activate/verify-login/disable) — mais rien de spécifique à l'Admin par-dessus. Le cahier
  des charges demande une authentification *renforcée spécifiquement* pour `/api/admin/**`
  (restriction IP/VPN, flux différent) : **toujours non implémenté à ce stade**, distinct du 2FA
  générique déjà en place.
- Droits internes différenciés entre comptes admin eux-mêmes (ex. un admin support ne doit pas
  pouvoir modifier les taux de commission) — pas encore fait, un seul rôle `ADMIN` pour tout.
- Toute action admin sensible (modération, remboursement, changement de rôle, changement de statut
  vendeur/boutique) est tracée dans `AuditLog` avec auteur et horodatage — **fait**
  (`audit.AuditAction`, voir `MODELE_DONNEES.md`).

## 12. Modèle de données détaillé

Le détail complet des entités (champs, types, relations, enums), à jour, vit dans
`MODELE_DONNEES.md` à la racine du dépôt — ne pas le dupliquer ici. Ce fichier inclut aussi un
diagramme de classes Mermaid exploitable directement.

## 13. Conventions de code à respecter

- Package racine : `com.maplenou.backend` — un sous-package par domaine métier
  (`auth`, `user`, `seller`, `catalog`, futur `order`, `delivery`, etc.), pas par couche technique.
- DTOs en `record` Java, jamais d'entité JPA exposée directement dans une réponse API.
- Vérification de propriété systématique sur toute écriture liée à une ressource (ex. un vendeur
  ne peut modifier que ses propres produits) — jamais uniquement un contrôle de rôle.
- Toute nouvelle table passe par une migration Flyway numérotée (`V5__...`, `V6__...`, jamais de
  modification rétroactive d'une migration déjà appliquée).
- Montants en FCFA : toujours `BigDecimal`, jamais `double`/`float`.
- Commentaires de code en français, cohérents avec le reste du projet.
