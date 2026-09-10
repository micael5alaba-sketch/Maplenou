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
