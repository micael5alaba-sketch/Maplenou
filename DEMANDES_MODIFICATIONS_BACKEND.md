# Remarques et demandes — intégration frontend Flutter

Ce document liste ce qui manque ou serait à revoir côté backend pour que l'app Flutter
(actuellement branchée sur les vraies routes de l'API) fonctionne correctement de bout en
bout. Rédigé côté frontend, à discuter ensemble avant implémentation.

---

## 1. Nouveau champ : prix barré / réduction (`oldPrice`)

Le frontend affiche déjà un badge de réduction et un prix barré sur les cartes produit, mais
rien ne l'alimente aujourd'hui — `Product` n'a pas de notion de prix avant réduction.

**Demandé :**
- Ajouter une colonne `old_price` (`BigDecimal`, nullable) sur `Product` (migration Flyway,
  ex. `V33__add_old_price_to_products.sql`).
- L'exposer sous le nom `oldPrice` dans `ProductSummaryResponse` et `ProductDetailResponse`,
  même convention que `basePrice`.
- L'accepter aussi dans `CreateProductRequest`/`UpdateProductRequest`, pour que le vendeur
  puisse le renseigner à la création/modification d'un produit.
- Pas besoin d'un champ pourcentage séparé — le frontend calcule le `%` lui-même à partir de
  `oldPrice` et `basePrice`.
- Règle d'affichage : si `oldPrice` est absent, `null`, ou inférieur/égal à `basePrice` → pas
  de promo affichée. Sinon → prix barré + badge "-X %".

## 2. SKU de variante généré automatiquement

`CreateVariantRequest.sku` est actuellement `@NotBlank` : c'est le vendeur (ou nous, en test
via Swagger) qui doit inventer une valeur, unique tous produits confondus (`UNIQUE` en base).
En pratique, personne n'a de nomenclature de SKU définie, et ça va vite provoquer des erreurs
de conflit ou des valeurs bâclées.

**Demandé :** rendre `sku` optionnel dans `CreateVariantRequest`. Si absent, le générer
côté serveur (ex. à partir du slug produit + un suffixe court aléatoire/séquentiel,
en garantissant l'unicité avant insertion). Le vendeur doit pouvoir le voir et le modifier
ensuite s'il a sa propre nomenclature interne, mais ne doit pas être obligé d'en fournir un à
la création.

## 3. Données de démonstration (seed)

Aucune donnée par défaut n'existe en base (catégories, boutiques, produits). Sur une base
fraîche, `GET /api/categories` et `GET /api/products` renvoient une liste vide — pas une
erreur, mais rien à afficher côté app tant que quelqu'un n'a pas créé de contenu à la main.
Pour l'instant j'ai injecté des données de test directement en SQL pour pouvoir avancer côté
UI, mais ce n'est pas une solution pérenne pour les autres devs qui cloneraient le projet.

**Demandé :** un jeu de données de démo pour le dev local — soit une migration Flyway dédiée
au profil `dev`, soit un jeu de requêtes Swagger documenté dans le `MANUEL.md` (créer un
compte, l'approuver vendeur, créer une boutique, quelques produits).

## 5. Catalogue : note moyenne absente de la liste

`GET /api/products` (`ProductSummaryResponse`) ne renvoie pas de note. Il faut appeler
`GET /api/products/{id}/reviews/summary` produit par produit pour l'avoir, ce qui fait un
appel réseau par carte affichée dans une grille. Ce serait plus simple d'avoir directement
`averageRating`/`reviewCount` dans le résumé catalogue.

## 6. Pas de notion de "produit populaire" ni de mise en avant

L'API de catalogue trie uniquement par date de création. Pas de champ `isFeatured`, pas de tri
par popularité/ventes. Si la home doit avoir une vraie section "populaires" (et pas juste
"derniers ajouts"), il faut se mettre d'accord sur comment ça se pilote.

## 7. Nouveau champ : image de catégorie (`imageUrl`)

`Category` n'a aujourd'hui ni icône ni photo. Le frontend affiche une icône générique à la
place d'une vraie vignette, ce qui n'est pas terrible visuellement (maquette d'origine : une
photo ronde par catégorie sur la home).

**Demandé :**
- Ajouter une colonne `image_url` (`VARCHAR(500)`, nullable) sur `Category` (migration Flyway,
  ex. `V34__add_image_url_to_categories.sql`) — même longueur/format que `logo_url` sur `Shop`
  et `url` sur `ProductImage`, puisque ce sera aussi une URL Cloudinary.
- L'exposer sous le nom `imageUrl` dans `CategoryResponse`.
- L'accepter dans `CreateCategoryRequest` (optionnel) et `UpdateCategoryRequest` (optionnel,
  pour pouvoir l'ajouter/changer après coup) — ce sont des routes déjà réservées à l'ADMIN
  (`@PreAuthorize("hasRole('ADMIN')")`), donc pas de nouvelle règle d'autorisation à écrire.
- Pas besoin de nouvel endpoint d'upload : le flux signé existant (`POST
  /api/media/upload-signature`) suffit — l'admin uploade l'image côté client vers Cloudinary,
  puis envoie l'URL obtenue dans `imageUrl` en créant/modifiant la catégorie, exactement comme
  pour les images produit.
- Règle d'affichage : si `imageUrl` est `null`, le frontend garde son icône générique en
  attendant — pas de valeur par défaut à générer côté serveur.

## 8. Page détail produit — ce qui est mocké côté frontend

J'ai construit une page détail produit générique (galerie, avis, spécifications, variantes,
livraison, produits similaires). Elle tourne pour l'instant avec des données mockées à trois
endroits ; le point commun n'est pas le même pour les trois, donc à traiter différemment :

**Avis clients — rien à faire côté backend, déjà tout prêt.** `ReviewController` expose déjà
`GET /api/products/{id}/reviews` et `GET /api/products/{id}/reviews/summary`. Ce n'est mocké
que parce que la page a été construite avant d'être branchée. Il suffira d'appeler ces deux
routes pour ce produit précis (pas de souci de N+1 ici vu qu'on est sur une seule fiche, pas
une liste — contrairement au point 5 plus haut sur le catalogue).

**Spécifications — vraiment absent, à ajouter.** Rien n'existe pour stocker des paires
label/valeur (Marque, Matière, Origine, Pointure, Garantie...) sur un produit. Proposition :
une colonne `specifications` en `JSONB` sur `Product` (liste ordonnée de `{label, value}`),
exposée telle quelle dans `ProductDetailResponse`, acceptée en écriture (optionnelle) dans
`CreateProductRequest`/`UpdateProductRequest`. Pas besoin d'une table à part ni de champs
prédéfinis — le composant frontend (`ProductSpecification`) affiche n'importe quelle paire
label/valeur, donc chaque vendeur peut mettre ce qui est pertinent pour son produit.

**Livraison et retours — pas un nouveau champ produit, plutôt une question de branchement.**
Le frontend affiche pour l'instant un délai/frais fixes identiques sur toutes les fiches, mais
ça existe déjà autrement dans le backend : `GET /api/delivery-zones` donne déjà `delivery_fee`
et `estimated_time_minutes` par zone (donc dépendant de l'adresse de l'acheteur, pas du
produit), et la politique de retour à 30 jours est une règle plateforme déjà actée (§5 du
`CLAUDE.md`), pas une donnée par produit. À se mettre d'accord : est-ce que la fiche produit
doit afficher les zones de livraison existantes (avec leurs frais), et est-ce que le texte de
politique de retour doit venir d'une page CMS (`ContentPageController`, déjà public via
`/api/pages/**`) plutôt que d'être codé en dur côté app ? Pas de nouveau endpoint à écrire dans
les deux cas, juste à choisir comment les brancher.

## 9. Écran Profil — ce qui est mocké côté frontend

Même logique que pour la fiche produit : certaines parties existent déjà côté backend et
n'attendent qu'à être branchées, d'autres manquent vraiment.

**Déjà existant, rien à ajouter — juste du branchement à faire plus tard :**
- Identité (`GET /api/users/me`) : nom, téléphone, email. J'ai utilisé `phoneVerified` pour le
  badge "Compte vérifié" — à confirmer avec lui que c'est la bonne notion (vérification du
  numéro de téléphone), ou si "vérifié" doit vouloir dire autre chose (KYC, email confirmé...).
- Adresses enregistrées (`AddressController`) déjà en place.
- Mes Favoris (`FavoriteController`) déjà en place.
- Mes Commandes (`GET /api/orders`) déjà en place — à vérifier qu'il est bien scopé à
  l'utilisateur connecté automatiquement (pas de paramètre `userId` à fournir).
- Déconnexion (`POST /api/auth/logout`) déjà géré (révocation des tokens access/refresh).

**Vraiment manquant, à ajouter :**
- **Mes Avis** — `ReviewController` ne liste que les avis *sur* un produit/une boutique donnée,
  pas ceux *écrits par* l'utilisateur connecté. Il faudrait une route du style
  `GET /api/users/me/reviews` (paginée), qui renvoie les `ReviewResponse` de l'utilisateur, tous
  produits/boutiques confondus.
- **Photo de profil** — `User` n'a pas de colonne `avatar_url`. Pour un vrai avatar (pas juste
  l'initiale du nom affichée en attendant), il faut l'ajouter, avec le même flux d'upload signé
  Cloudinary que pour les images produit/catégorie.
- **Compteur "commandes en cours"** — `GET /api/orders` existe mais rien ne renvoie directement
  "combien sont en cours". Soit calculable côté app à partir de la liste (en gérant la
  pagination), soit un petit endpoint dédié type `GET /api/orders/summary` qui renvoie le
  nombre par statut.

**À clarifier ensemble, pas forcément du code à écrire :**
- **Modes de paiement** — rien n'existe côté backend pour un "moyen de paiement enregistré" par
  utilisateur, ce qui est cohérent avec FedaPay facturé à chaque transaction (pas de wallet ni
  de carte enregistrée). Le texte "T-Money, Flooz, Carte" reste pour l'instant purement
  informatif (les moyens disponibles sur la plateforme), pas une donnée par utilisateur. Si on
  veut un vrai moyen de paiement préféré par défaut, ce sera un nouveau champ à ajouter — à
  décider si c'est utile pour le lancement.
- **Thème et Langue** — a priori une préférence locale à l'appareil (`shared_preferences`), pas
  besoin de backend. À revoir seulement si on veut la synchroniser entre appareils.

## 10. Formulaire "Devenir Vendeur" — champs sans équivalent backend

Nouvel écran (`SellerRegistrationScreen`, mocké pour l'instant). `POST /api/sellers/apply`
existe déjà mais `ApplySellerRequest` n'accepte que `shopName`. Le formulaire demande trois
champs de plus qui n'ont pour l'instant aucune colonne où atterrir :

- **Numéro de téléphone de l'entreprise**
- **Type de produits** (Mode, Beauté, Maison, Électronique, Alimentation, Bijoux, Chaussures,
  Accessoires, ou "Toutes catégories acceptées")
- **Adresse de la boutique**

**Question à trancher ensemble avant d'écrire le code :** ces informations appartiennent-elles
à la candidature (`SellerProfile`, remplie avant l'approbation admin) ou à la boutique
elle-même (`Shop`, qui n'existe qu'après approbation et a déjà `city`/`district`) ? Vu que
`Shop` a déjà une notion de localisation, l'adresse et le téléphone semblent plus à leur place
là — mais ça veut dire soit les collecter dès la candidature et les reporter sur le `Shop` à la
création, soit les redemander après approbation. Le "type de produits" ne correspond à rien
dans le modèle actuel (les catégories sont par produit, pas par boutique, cf. `CLAUDE.md` §13)
— si on veut le garder, c'est plutôt un champ informatif/libre pour aider l'admin à trier les
candidatures, pas un vrai lien vers `Category`.

## 11. Dashboard vendeur — le schéma suffit, il manque la couche requêtes/endpoint

Nouvel écran (`SellerDashboardScreen`, mocké). **Aucune colonne ni table manquante** — tout ce
qu'il affiche se calcule à partir de l'existant. Ce qui manque, c'est un contrôleur/service
"dashboard vendeur" (rien d'équivalent aujourd'hui : le module `admin` a bien un KPI
controller, mais il est global, réservé à l'admin, pas filtré par boutique) et les requêtes
d'agrégation dessous, qui n'existent pas encore dans `SubOrderRepository`/`ProductRepository`.

**Ce qui existe déjà et confirme que c'est faisable :**
- `SubOrder` a déjà `subtotal`, `commissionAmount`, `netAmount` (= gain vendeur par
  sous-commande) et `status` (`PENDING, PREPARING, READY_FOR_PICKUP, IN_DELIVERY, DELIVERED,
  CANCELLED, RETURN_REQUESTED, RETURNED`).
- `SubOrderRepository` a déjà une requête `findDeliveredUnpaidByShopIdAndPeriod` (utilisée par
  le job de reversement) qui calcule exactement "livré mais pas encore payé" sur une période —
  c'est la même logique que le "solde disponible", juste sans borne de période et sans endpoint
  pour le vendeur.
- `Payout` ne stocke que l'historique des reversements déjà effectués (`periodStart/End`,
  `amount`, `status`) — pas un solde courant, il faut le calculer.
- `ProductImage`, `Product.category` existent déjà pour retrouver la vignette et la catégorie
  d'un produit vendu.

**Ce qu'il faut ajouter (uniquement du code, pas de migration) :**
1. **CA hebdo + variation** : une requête `SUM(netAmount)` groupée par semaine, filtrée par
   `shop_id` et statut ≠ `CANCELLED`, sur `created_at`. N'existe pas encore.
2. **Commandes en cours / prêtes à expédier** : un `countByShopIdAndStatusIn(...)` sur
   `SubOrderRepository` (à définir ensemble : quels statuts comptent comme "en cours"). N'existe
   pas encore — `SubOrderController` n'expose que liste/détail/changement de statut.
3. **Solde disponible** : généraliser `findDeliveredUnpaidByShopIdAndPeriod` en version "sans
   borne de date" (tout ce qui est `DELIVERED` et pas encore rattaché à un `Payout`), exposé au
   vendeur — `PayoutController` n'a aujourd'hui que `GET /api/seller/payouts` (historique paginé).
4. **Courbe des ventes** : même donnée que le point 1, mais groupée par jour plutôt que par
   semaine, sur 7/14/28 jours. Aucune requête de ce type n'existe.
5. **Catégories les plus vendues** : jointure `SubOrderItem → ProductVariant → Product →
   Category`, somme des quantités par catégorie, filtrée par boutique. Rien de tel n'existe —
   c'est la partie la plus lourde à écrire des cinq.
6. **Produit le plus vendu** : même jointure que le point 5, mais groupée par produit avec
   `LIMIT 1` au lieu de par catégorie.

**Proposition concrète** : un `SellerDashboardController`/`Service` (dans `seller` ou `order`),
avec ces nouvelles requêtes dans `SubOrderRepository`, plutôt que de faire calculer ça côté
app à partir de dizaines d'appels — sur un catalogue qui grossit, ce serait beaucoup trop lent
et fragile côté client.

## 12. Gestion des Commandes (vendeur) — presque tout existe déjà, quelques trous précis

Nouvel écran (`OrdersManagementScreen`, mocké). Contrairement au dashboard, l'essentiel existe
déjà : `GET /api/sub-orders` liste et pagine déjà les sous-commandes du vendeur connecté, et
`PATCH /api/sub-orders/{id}/status` lui permet déjà de changer le statut. Trois trous précis :

1. **Pas de filtre par statut sur la liste.** `listMySubOrders` ne prend qu'un `Pageable` — pas
   de paramètre `status`. L'écran a des filtres ("À préparer", "En livraison"...) qui
   supposent un `GET /api/sub-orders?status=PREPARING&page=...` côté serveur plutôt que de tout
   récupérer et filtrer côté app.
2. **Pas de nom client sur `SubOrderResponse`.** Ni `SubOrder` ni `OrderResponse` n'exposent le
   nom de l'acheteur — seulement l'adresse de livraison. Il faut soit l'ajouter à
   `SubOrderResponse` (ex. `buyerName`, dérivé de `Order.buyer`), soit exposer un endpoint qui le
   joint.
3. **Pas de référence commande courte.** `SubOrderResponse.id` est un UUID brut — l'écran
   attend un numéro type `#CMD-8492`. À décider : tronquer l'UUID côté app (ex. les 6 premiers
   caractères), ou générer une vraie référence lisible côté serveur à la création de la commande.

**Question à trancher :** quel montant afficher sur la carte — `subtotal` (ce que le client a
payé pour les articles de cette boutique) ou `netAmount` (ce qui revient au vendeur après
commission) ? Les deux existent déjà sur `SubOrderResponse`, c'est juste à choisir.

Petite remarque en passant sur `PATCH /api/sub-orders/{id}/status` : le commentaire dans
`SubOrderController` dit "passe au statut CONFIRMED ou SHIPPED", des valeurs qui ne
correspondent à aucune entrée de l'enum `SubOrderStatus` actuel (`PENDING, PREPARING,
READY_FOR_PICKUP, IN_DELIVERY, DELIVERED, CANCELLED, RETURN_REQUESTED, RETURNED`) — commentaire
sans doute obsolète, à vérifier que les transitions réellement autorisées correspondent bien au
parcours `PREPARING → READY_FOR_PICKUP → IN_DELIVERY → DELIVERED` que l'écran suppose.

## 13. Catalogue vendeur — la notion de "stock" ne correspond pas au modèle actuel

Nouvel écran (`VendorCatalogScreen`, mocké). Ici le décalage est plus structurel que sur les
écrans précédents :

- **Le stock est par variante, pas par produit.** `ProductVariant.stockQuantity` existe, mais
  rien n'agrège un stock total au niveau `Product` — un produit avec plusieurs variantes n'a pas
  "un" chiffre de stock. L'écran affiche pourtant une seule quantité et un seul badge par
  produit. À décider ensemble : on agrège côté serveur (somme des variantes, ou stock de la
  variante par défaut ?), ou l'app va chercher le détail (`ProductDetailResponse.variants`) et
  fait la somme elle-même.
- **"Stock faible" n'existe pas.** `ProductStatus` n'a que `DRAFT/ACTIVE/OUT_OF_STOCK/ARCHIVED`
  — pas d'état intermédiaire. Soit on calcule ça côté app à partir d'un seuil sur le stock agrégé
  (ce que j'ai fait pour le mock : ≤ 5 = stock faible), soit le seuil doit être configurable côté
  vendeur/admin et calculé côté serveur.
- **Pas de recherche sur la liste du vendeur.** `GET /api/shops/mine/products` ne prend qu'un
  `Pageable`, pas de paramètre de recherche — alors que le catalogue public a déjà une recherche
  plein texte (`tsvector`, cf. `CLAUDE.md` §10). Faudrait la même chose ici, filtrée par boutique.
- **`ProductSummaryResponse` n'a pas de description.** La carte vendeur affiche une description
  courte, qui n'existe aujourd'hui que dans `ProductDetailResponse` (vue détail, plus lourde). À
  voir si on l'ajoute au résumé ou si l'app va chercher le détail pour chaque carte (pas idéal
  sur une liste).

**Ce qui existe déjà, pour le reste :** modifier (`PATCH .../products/{id}`) et supprimer
(`DELETE .../products/{id}`, soft delete) sont déjà là. Il n'y a en revanche aucun endpoint de
duplication — à ajouter si on veut vraiment ce raccourci, ou sinon le laisser être une création
manuelle pré-remplie côté app à partir des données déjà en main.

## 14. Ajout produit — bonne nouvelle sur les attributs, deux vrais trous ailleurs

Nouvel écran (`AddProductScreen`, mocké) : formulaire "intelligent" qui affiche des
caractéristiques différentes selon la catégorie (Pointure/Couleur/Matière pour des chaussures,
Volume/Famille olfactive pour un parfum, Marque/Stockage/RAM pour un téléphone, etc.), plus
variantes, prix, livraison, visibilité.

**Les attributs par catégorie ne demandent en fait rien de nouveau.** Le champ `specifications`
en JSONB proposé au point 8 (pour la fiche produit) suffit très bien ici : peu importe qui décide
des labels ("Pointure", "Volume"...), le backend n'a qu'à stocker des paires label/valeur. Le
"formulaire intelligent" (savoir que Téléphone → Marque/Stockage/RAM) vit entièrement côté app,
dans une table de correspondance catégorie → attributs — pas besoin d'un schéma d'attributs par
catégorie côté serveur pour l'instant. Un seul champ à ajouter, déjà demandé, pas deux.

**Ce qui colle déjà bien tel quel :** `ProductStatus` a déjà `DRAFT`/`ACTIVE` — exactement
Brouillon/Publier immédiatement, rien à changer. Les images passent par le flux Cloudinary
existant (`upload-signature` + `POST .../products/{id}/images`).

**Deux vrais trous :**
1. **Variantes multi-axes.** `ProductVariant.label` est un simple `String` — une variante, un
   label. L'écran permet d'ajouter plusieurs axes en même temps (Taille ET Couleur), ce qui en
   toute rigueur donne un produit cartésien (S+Noir, S+Blanc, M+Noir...), pas juste une liste de
   labels. À trancher : soit l'app concatène en un seul label par combinaison ("S / Noir") pour
   rester compatible avec le modèle actuel, soit `ProductVariant` a besoin d'une vraie structure
   d'attributs (une table `variant_attribute_values` ou un JSONB) pour représenter ça proprement.
2. **Rien au niveau produit pour la livraison.** Ni poids de colis, ni délai estimé, ni
   disponibilité de livraison n'existent sur `Product`/`ProductVariant` — seul `DeliveryZone`
   existe, et c'est une notion de zone géographique, pas de produit. Si ces trois champs sont
   vraiment voulus par produit (plutôt que gérés globalement par zone), il faut les ajouter à
   `Product` : `package_weight_kg` (nullable), `estimated_delivery_delay` (texte libre ou
   nullable), `delivery_available` (boolean, défaut `true`).

---

## Remarques mineures / cohérence (pas bloquant)

- `GET /api/categories` n'est pas paginé (`findByParentIsNullAndActiveTrueOrderByNameAsc`
  renvoie tout) — contredit la règle du `CLAUDE.md` §10 ("aucun endpoint ne renvoie une liste
  sans pagination"). Sans impact au volume actuel, à surveiller si le nombre de catégories
  grossit.
- `CLAUDE.md` est désynchronisé avec le code réel sur plusieurs points : il indique Java 17 /
  Spring Boot 3.3 (le `pom.xml` utilise Java 21 / Spring Boot 4.1.0), "pas de Redis pour
  l'instant" (Redis est déjà utilisé : rate limiting, verrouillage de compte, blacklist de
  tokens JWT), et "seulement l'étape 0 terminée" (panier, commandes, paiement, avis, audit,
  2FA, KPI admin sont déjà codés). À mettre à jour pour que la doc reste fiable.
- CORS n'autorise que `http://localhost:3000` et `http://localhost:4000` par défaut
  (`CORS_ALLOWED_ORIGINS`). Sans impact pour l'app mobile native, mais à étendre le jour où on
  teste en Flutter Web sur un autre port.
