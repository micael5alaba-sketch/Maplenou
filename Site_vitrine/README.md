# Site_vitrine — site public de Maplenou

Site de présentation de la marketplace (acheter, vendre, livrer, service client), **séparé** de l'application de
tickets (`../ServiceClient`) et du back-office (`../Admin_frontend`). Il lit le catalogue public de Bakend
(catégories, quelques produits, boutiques, barème de livraison) et renvoie vers l'application et le service client.

Mise en page inspirée du *type* de site d'un grand opérateur (menu par public, bandeau d'accroche, cartes produits
en FCFA, bloc d'aide, recherche de boutiques, lettre d'information, réseaux sociaux) ; marque, textes, logo et
photos restent ceux de Maplenou.

*Public website of the marketplace, separate from the ticketing app. French is the reference language; every text
also exists in English (`src/lib/messages.ts`).*

| Page | Chemin |
| --- | --- |
| Accueil (accroche, nos services, bandeau vendeurs, produits notés, rayons, étapes, livraison, aide, boutiques, FAQ, lettre d'information + réseaux) | `/` |
| Boutiques (boutiques approuvées, filtre par ville, sans JavaScript) | `/boutiques` |
| Vendre (ouverture de boutique, commission 5 % / 3 % comprise dans le prix affiché, frais mensuel, calculateur) | `/vendre` |
| Livrer (4 documents, étapes d'une livraison) | `/livrer` |
| Service client (motifs, délais de première réponse 24 h/24, suivi d'un ticket, centre d'aide) | `/service-client` |

En-tête : une barre fine (centre d'aide, boutiques, application, préférences) puis un menu par public — Acheter,
Vendre, Livrer, Service client — qui ouvre un panneau de liens (Échap ou un clic ailleurs le ferme) ; sur téléphone,
un bouton « Menu » ouvre les mêmes rubriques en accordéon.

Langue (FR/EN) et thème (système, clair, sombre) : choisis dans l'en-tête et gardés dans un petit cookie
(`maplenou_locale`, `maplenou_theme`), sans stockage navigateur. Sans choix : langue du navigateur, sinon français.

## Images et couleurs

- **Visuels Maplenou** : `public/images/visuels/*.webp`, deux largeurs chacun (1600/800 px, ou 1080/540 px pour les
  carrés), choisies par le navigateur (`srcset`). Composant `src/components/Visual.tsx` ; le texte imprimé sur chaque
  visuel est repris dans son texte alternatif (`visuals` dans `src/lib/messages.ts`, FR/EN).
- Les **originaux** (PNG/JPEG, environ 20 Mo) sont gardés sur l'ordinateur dans `design/originals/`, ignoré par Git.
  Pour en ajouter un : déposez-le dans ce dossier, créez ses deux WebP (qualité 80) dans `public/images/visuels/`,
  puis déclarez-le dans `Visual.tsx`. Les images de l'application mobile (`../frontend/assets/images`) ne sont pas
  utilisées ici.
- **Thème sombre** : jaune Maplenou `#a76f07` pour les boutons (texte noir), les icônes et les grands titres ; le petit
  texte jaune utilise `#c0974c`, plus clair, pour garder le contraste WCAG AA (4,5:1) sur fond sombre.

## Ports de l'écosystème

| Port | Application |
| --- | --- |
| 3000 | Back-office admin (`Admin_frontend/maplenou`) |
| 3001 / 8081 | Service client : application de tickets / API (`ServiceClient`) |
| **3002** | **Ce site** |
| 8006 | Centre d'aide (documentation du service client, profil `docs`) |
| 8080 | Bakend (API Maplenou) |

## Démarrer avec Docker (rien d'autre à installer)

```powershell
cd Site_vitrine
copy .env.example .env          # Linux/macOS : cp .env.example .env
docker compose up -d --build --wait
# → http://localhost:3002
docker compose logs -f site     # journaux
docker compose down             # arrêter
```

Bakend doit tourner sur l'ordinateur (`Bakend/start-all.ps1`) pour afficher les rayons, produits et prix de
livraison réels ; sinon le site affiche son contenu fixe (rien ne casse).

## Démarrer sans Docker (Node 22)

```powershell
cd Site_vitrine
npm ci
# Bakend sur l'ordinateur : créez .env.local avec la ligne  MAPLENOU_API_URL=http://localhost:8080
npm run dev                     # http://localhost:3002 (rechargement à chaud)
```

## Tests

```powershell
npm run lint
npm run typecheck               # vérifie aussi que FR et EN ont les mêmes clés
npm test                        # commission, textes FR/EN, liste des villes
npm run build                   # build de production
```

## Configuration (`.env`)

| Variable | Rôle |
| --- | --- |
| `SITE_HOST_PORT` | Port de l'ordinateur (3002) |
| `SITE_PUBLIC_URL` | Adresse publique du site (plan du site, aperçus de liens) |
| `MAPLENOU_API_URL` | Bakend vu par le serveur du site (`http://host.docker.internal:8080` en Docker) |
| `APP_URL`, `ANDROID_APP_URL`, `IOS_APP_URL` | Liens « Ouvrir l'application » / magasins ; vides = boutons masqués |
| `SERVICE_CLIENT_URL` | Application de tickets (bouton « Ouvrir un ticket ») |
| `HELP_CENTER_URL` | Centre d'aide public (guide utilisateur, `…/user/fr/`) |
| `SOCIAL_FACEBOOK_URL`, `SOCIAL_INSTAGRAM_URL`, `SOCIAL_TIKTOK_URL`, `SOCIAL_WHATSAPP_URL`, `SOCIAL_X_URL`, `SOCIAL_YOUTUBE_URL` | Pages officielles de Maplenou ; vides = réseau masqué (pied de page et bloc « Restez informé ») |
| `LOG_MAX_SIZE`, `LOG_MAX_FILES` | Rotation des journaux du conteneur |

## Mise en ligne

- Mettre `SITE_PUBLIC_URL`, `APP_URL`, `SERVICE_CLIENT_URL` et `HELP_CENTER_URL` sur les adresses publiques.
- Le centre d'aide public ne doit publier que le guide utilisateur (`DOCS_SITES=user`, `ASSISTANT_SITES=user`
  dans `ServiceClient/.env`) : les guides équipe et développeur restent internes.
- Les données affichées viennent uniquement des points publics de Bakend (`GET /api/categories`,
  `/api/products`, `/api/shops`, `/api/delivery-pricing-tiers`) ; le site n'a besoin d'aucun secret.
- Le formulaire « Restez informé » envoie l'adresse à `POST /api/newsletter/subscribe` de Bakend (action serveur,
  fonctionne sans JavaScript) ; le site ne garde rien. Une adresse déjà inscrite reçoit la même réponse « merci ».
