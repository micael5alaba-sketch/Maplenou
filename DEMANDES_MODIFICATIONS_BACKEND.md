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
