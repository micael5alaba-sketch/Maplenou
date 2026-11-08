# Frontend

L'application web (`frontend/`) utilise Next.js 16.4.3 avec l'App Router, React 19.3.0 et TypeScript. Elle sert les
écrans des demandeurs (clients, vendeurs, livreurs) et le back-office du support (agents, responsables,
administrateurs), en français et en anglais, avec les thèmes clair, sombre et système.

## Structure App Router

| Route (dossier sous `src/app/`) | Pour qui | Contenu |
| --- | --- | --- |
| `page.tsx` (`/`) | tout le monde | redirige vers `/login`, `/support` ou `/aide` selon la session |
| `login/` | non connecté | `AUTH_MODE=maplenou` : compte Maplenou (numéro de téléphone + mot de passe, puis le code à 6 chiffres si le compte a la 2FA) ; `AUTH_MODE=demo` : un bouton par persona |
| `(requester)/aide/`, `aide/nouveau/`, `aide/tickets/`, `aide/tickets/[id]/` | demandeurs | accueil de l'aide (A1), création (A3), mes tickets (A2), détail d'un ticket (A4) |
| `(requester)/notifications/`, `(requester)/compte/` | demandeurs | notifications, compte |
| `support/`, `support/tickets/`, `support/tickets/[id]/`, `support/mes-tickets/` | personnel du support | tableau de bord (B1), file d'attente (B2/B3), espace de traitement (B4), mes tickets |
| `support/notifications/`, `support/rapports/` | personnel du support | notifications, rapports |
| `support/admin/agents/`, `support/admin/categories/`, `support/admin/calendrier/` | responsable / administrateur | agents (avec « Rôles support » et « Anciens tickets Maplenou » pour les administrateurs), catégories, calendrier SLA (avis sur le mode SLA venant de `admin/sla-settings`) |
| `acces-refuse/`, `not-found.tsx` | tout le monde | accès refusé, 404 |
| `api/backend/[...path]/`, `api/session/`, `api/demo-users/`, `api/auth-config/` | serveur uniquement | gestionnaires de route du BFF ; `api/auth-config` renvoie le mode de connexion (`AUTH_MODE`, lu à l'exécution, `no-store`) |

`(requester)` est un groupe de routes (sans segment d'URL). Son `layout.tsx` et `support/layout.tsx` sont des
composants serveur qui lisent la session, redirigent vers `/login` en son absence et gardent chaque public dans sa
zone (les demandeurs reçoivent `/acces-refuse` pour `/support` ; le personnel du support autre que les
administrateurs est renvoyé vers `/support`). Ils enveloppent les pages dans `SessionProvider` et dans la navigation
`RequesterShell` / `SupportShell`. Ces redirections ne servent qu'à la navigation — l'API impose chaque permission.

Le `layout.tsx` racine lit la langue et le thème côté serveur, définit `<html lang>` et `data-theme`, et fournit
`I18nProvider` et `ToastProvider`.

## Proxy BFF et cookie de session

Le navigateur ne détient jamais le jeton d'accès :

1. `login/` appelle `POST /api/session` avec `{phoneNumber, password}` (compte Maplenou), puis `{code}` pour
   l'étape à deux facteurs, ou avec l'identifiant d'un persona (démonstration). Ce gestionnaire de route refuse une
   autre origine, appelle `POST /api/v1/auth/maplenou/login` / `/mfa` du backend (en transmettant
   `X-Forwarded-For`) ou `POST /api/v1/dev/token` (profil de démonstration uniquement) ; pendant l'attente d'un code
   2FA, le jeton MFA du cœur attend dans le cookie httpOnly `maplenou_mfa` (chemin `/api/session`, 5 minutes ; absent
   → `MFA_EXPIRED`). Il stocke le jeton support obtenu dans le cookie
   `maplenou_session` (`httpOnly`, `SameSite=Lax`, `Secure` si `SESSION_COOKIE_SECURE=true`, expirant avec le jeton).
   `GET /api/session` renvoie l'utilisateur décodé ; `DELETE /api/session` efface le cookie.
2. Chaque appel d'API du navigateur va vers `/api/backend/api/v1/support/...`. Le gestionnaire
   `src/app/api/backend/[...path]/route.ts` :
    - rejette les segments `..` et tout ce qui est hors de `api/v1/support` (donc `/api/v1/dev/**` et actuator sont inaccessibles) ;
    - pour les requêtes autres que GET/HEAD, rejette une `Origin` dont l'hôte diffère de celui de la requête (défense CSRF en profondeur) ;
    - renvoie `401` sans cookie de session ;
    - ne transmet à `BACKEND_URL` que `content-type`, `accept`, `if-match` plus `Authorization: Bearer <token>`,
      diffuse le corps en flux et ne recopie qu'une liste fixe d'en-têtes de réponse.
3. `src/lib/session.ts` décode le contenu du jeton pour la navigation et l'affichage uniquement (`decodeToken`) ; rien
   n'y fait foi pour l'autorisation.

`BACKEND_URL` (par défaut `http://localhost:8081`) est une variable côté serveur ; dans Compose elle vaut
`http://backend:8080`.

## Chargement des données

`src/lib/api.ts` est le seul client HTTP :

| Export | Usage |
| --- | --- |
| `API_BASE` | `/api/backend/api/v1/support` |
| `api.get / post / put / del` | appels JSON ; `post`/`put` sérialisent le corps |
| `api.multipart(path, jsonPart, json, files)` | une partie JSON (`ticket` ou `message`) plus `files` |
| `ApiError` | levée pour les réponses non 2xx ; `problem` contient le corps RFC 9457, `code` son code stable |
| `toQuery(params)` | chaînes de requête, en répétant les valeurs de tableau (`status=A&status=B`) |
| `clientId()` | identifiant pour l'envoi idempotent des messages (`clientMessageId`) |

Une réponse `401` redirige vers `/login?expired=1`. Dans les composants client, chargez les données avec `useApi`
(`src/lib/useApi.ts`) :

```tsx
const list = useApi<Page<TicketSummary>>(`/tickets?size=10&sort=updatedAt,desc`);
// list.data, list.error (ApiError), list.loading, list.reload(), list.setData(d); pass null to skip loading
```

`useApi` garde les données précédentes pendant le rechargement et ignore les réponses d'un chemin qui n'est plus le
chemin courant. Affichez ses états avec `Loading`, `ErrorState` (avec `onRetry={list.reload}`) et `EmptyState` de
`components/ui.tsx`. N'affichez un bouton d'action que si `allowedActions` du ticket contient l'action.

### Cache client des lectures d'API (`apiCache.ts`)

`useApi` passe par `src/lib/apiCache.ts`, un cache en mémoire des réponses GET indexé par chemin :

- **stale-while-revalidate :** un écran affiche tout de suite la dernière réponse connue pour son chemin, puis
  `useApi` la recharge systématiquement et la remplace ;
- **déduplication :** des requêtes identiques lancées en même temps partagent un seul appel réseau (`dedupe`) ;
- **borné :** au plus 100 réponses, la moins récemment utilisée est retirée en premier ; rien n'est écrit sur disque,
  et rien n'est mis en cache pendant le rendu serveur (navigateur uniquement) ;
- **vidé** par `clearApiCache()` après chaque modification (tout appel autre que GET via `apiFetch` dans `api.ts`) et
  à la déconnexion (`logout()` dans `components/session.tsx`), pour qu'un utilisateur ne garde jamais de données
  périmées après une action ni ne voie les données d'un utilisateur précédent.

N'ajoutez pas d'autre cache client : utilisez `useApi`, et appelez l'API via `api.*` pour que les modifications
vident le cache. Les tests unitaires sont dans `src/lib/apiCache.test.ts` ; voir aussi
[Cache et espace disque](caching.md).

## Notifications push dans le navigateur (Firebase)

Les écrans <span class="ui">Notifications</span> (`/notifications` pour les demandeurs, `/support/notifications`
pour le support) affichent la carte <span class="ui">Notifications push</span> (`components/PushNotifications.tsx`,
textes dans `messages/push.ts`). Ses états : navigateur non pris en charge, non configuré sur le serveur, bloqué par
le navigateur, désactivé, activé ; ses boutons <span class="ui">Activer les notifications push</span>,
<span class="ui">Envoyer une notification de test</span> et <span class="ui">Désactiver</span>.

| Fichier | Rôle |
| --- | --- |
| `src/lib/push.ts` | détecte l'état sans rien demander, demande l'autorisation, obtient le jeton FCM et l'enregistre (`POST /notifications/devices`, plateforme `WEB`), désactive le push, envoie un test, désinscrit le navigateur à la déconnexion |
| `src/app/api/push-config/route.ts` | renvoie les valeurs publiques `FIREBASE_WEB_*` lues dans l'environnement du serveur à l'exécution (`Cache-Control: no-store`), ou `{ configured: false }` ; aucune reconstruction quand elles changent |
| `public/push-sw.js` | service worker : affiche la notification push et, au clic, ouvre l'application sur le ticket (`/?ticket=<id>`) ; aucun cache, aucune requête réseau, pas de SDK Firebase |

Le SDK JavaScript de Firebase (`firebase` 12.19.0 dans `package.json`) est chargé par un `import()` dynamique
seulement quand l'utilisateur active ou désactive le push : les autres pages ne le téléchargent jamais. Une fois le
push activé, **Firebase garde son jeton dans l'IndexedDB du navigateur** — la seule exception documentée à la règle
« pas d'IndexedDB/localStorage », imposée par Firebase ; l'application elle-même n'enregistre rien. `logout()`
désinscrit le navigateur pour que l'utilisateur suivant ne reçoive pas les notifications push du précédent.
Configuration et tests : [Notifications push Firebase](firebase.md).

## Langues (FR/EN) et thèmes

Ceci résume `frontend/I18N.md`, qui fait référence.

- Le français est la langue par défaut ; l'anglais est complet. Le choix est stocké dans le cookie
  `maplenou_locale`, le thème (`system`, `light`, `dark`) dans `maplenou_theme` (un an, `SameSite=Lax`), pour que le
  serveur affiche la bonne langue et le bon thème dès le premier rendu. Sans cookie, la langue suit
  `Accept-Language`, puis le français (`negotiateLocale`).
- `components/Preferences.tsx` est le sélecteur affiché dans l'en-tête, la barre latérale et la page de connexion.
  `usePreferences()` écrit le cookie, met à jour `<html lang>` ou `data-theme` et appelle `router.refresh()`.

| Fichier | Contenu |
| --- | --- |
| `src/lib/i18n/config.ts` | `LOCALES`, `THEMES`, noms des cookies, `negotiateLocale`, `INTL_LOCALE` |
| `src/lib/i18n/define.ts` | `defineMessages`, `format("{x}", vars)`, `plural(locale, n, one, other)` |
| `src/lib/i18n/client.tsx` | `I18nProvider`, `useMessages`, `useLabels`, `useFormat`, `useErrorText`, `useLocale`, `useDocsUrl`, `usePreferences` |
| `src/lib/i18n/server.ts` | `getLocale`, `getTheme`, `getMessages`, `DOCS_BASE_URL` |
| `src/lib/i18n/messages/*.ts` | un espace de noms par écran ou groupe de composants ; `common.ts`, `errors.ts`, `notifications.ts` |

Règles de `I18N.md` : aucun texte visible écrit en dur dans les composants (y compris `aria-label`, `title`,
placeholders, toasts) ; `format()` pour les variables, jamais de concaténation ; `plural()` pour les nombres ;
`useLabels()` pour les statuts, priorités, rôles, états SLA et actions d'historique ; `useFormat()` pour les dates,
montants, tailles et durées ; `useErrorText()` pour les erreurs d'API (jamais `e.message`) ; typographie française
(espace insécable avant `:` `;` `?` `!`, guillemets « ») ; les données métier (noms de catégories, sujets, messages)
sont affichées telles qu'enregistrées.

### Ajouter un texte ou un espace de noms

```ts
// src/lib/i18n/messages/myScreen.ts
import { defineMessages } from "../define";

export const myScreen = defineMessages({
  fr: { title: "Mes réglages", saved: "{count} réglage enregistré", savedMany: "{count} réglages enregistrés" },
  en: { title: "My settings", saved: "{count} setting saved", savedMany: "{count} settings saved" },
});
```

```tsx
const t = useMessages(myScreen);               // client component
const locale = useLocale();
plural(locale, n, t.saved, t.savedMany);
// server component: const t = await getMessages(myScreen);
```

`defineMessages` vérifie au typage que `en` a exactement les clés de `fr` : une traduction manquante fait échouer
`npm run typecheck`. Ajoutez une clé dans les deux langues dans le même changement.

Le lien vers la documentation dans les coquilles de navigation utilise `useDocsUrl()` →
`${DOCS_BASE_URL}/<site>/<locale>/`, où `DOCS_BASE_URL` vient de l'environnement (`DOCS_PUBLIC_URL` dans Compose).

## Jetons de design

Couleurs, rayons, ombres, espacements et polices sont des propriétés CSS personnalisées en tête de
`src/app/globals.css` (`--brand`, `--accent`, `--bg`, `--surface`, `--text`, `--text-secondary`, `--info`,
`--success`, `--warning`, `--serious`, `--danger`, `--neutral`, `--note`, `--focus`, `--radius*`, `--space-1…6`,
`--font`, `--mono`…). Les valeurs sombres s'appliquent avec `<html data-theme="dark">`, ou lorsque le système
d'exploitation est en mode sombre et que l'utilisateur n'a pas choisi `light`. Ne codez jamais une couleur en dur dans
un composant : réutilisez ou ajoutez un jeton dans les deux thèmes et gardez un contraste WCAG AA. Les sites de
documentation reprennent les couleurs de la marque (`documentation/shared/stylesheets/maplenou.css`). Les règles de
design sont dans le [Design system](/developer/en/ux-ui/design-system/).

## Règles d'accessibilité

WCAG 2.2 est la base. Les composants fournissent déjà :

- un lien d'évitement vers `#main` dans les deux coquilles et un contour `:focus-visible` visible (`--focus`) ;
- `role="status"` + `aria-live="polite"` pour les chargements et les toasts, `role="alert"` pour les erreurs ;
- un `<dialog>` natif (`ConfirmDialog`) pour les actions définitives (résoudre, clôturer) ;
- des libellés `.sr-only` pour les contrôles composés d'une icône, et le respect de `prefers-reduced-motion` pour les squelettes ;
- des badges de statut avec du texte, jamais la couleur seule.

Quand vous construisez un écran : un seul `<h1>`, des titres sémantiques, un `<label>` pour chaque champ, des
contrôles accessibles au clavier dans un ordre logique, des onglets avec `role="tablist"`/`role="tab"` et
`aria-selected`, des `aria-label` traduits, et une mise en page utilisable sur téléphone (la suite e2e a un projet
mobile).

## Ajouter un écran

1. Créez `src/app/<zone>/<route>/page.tsx` sous `(requester)` ou `support` pour que la bonne mise en page, la session
   et la coquille s'appliquent. Utilisez `"use client"` si la page utilise des hooks.
2. Créez son espace de noms dans `src/lib/i18n/messages/<écran>.ts` (FR et EN).
3. Chargez les données avec `useApi`, modifiez avec `api.*`, affichez les erreurs avec `useErrorText()` et `useToast()`.
4. Pilotez chaque bouton d'action par `allowedActions` ; ne réimplémentez jamais une règle de permission dans l'interface.
5. Stylez avec les classes et jetons existants de `globals.css`.
6. Ajoutez un lien dans `RequesterShell` ou `SupportShell` s'il s'agit d'une entrée de navigation (libellé dans `shell.ts`).
7. Ajoutez un test unitaire/de composant (`*.test.tsx`, Vitest + Testing Library) et, pour un parcours clé, une étape Playwright.
8. Documentez l'écran dans le guide utilisateur ou personnel, dans les deux langues.

Contrôle qualité : `npm run lint && npm run typecheck && npm test && npm run build` (ou `./scripts/test.sh frontend`).
