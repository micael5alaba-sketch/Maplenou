# Frontend

The web app (`frontend/`) is Next.js 16.4.3 with the App Router, React 19.3.0 and TypeScript. It serves the
requester screens (customers, sellers, delivery drivers) and the support back-office (agents, managers,
administrators), in French and English, with light, dark and system themes.

## App Router layout

| Route (folder under `src/app/`) | Who | Content |
| --- | --- | --- |
| `page.tsx` (`/`) | everyone | redirects to `/login`, `/support` or `/aide` from the session |
| `login/` | not signed in | `AUTH_MODE=maplenou`: Maplenou account (phone number + password, then the 6-digit code when the account has 2FA); `AUTH_MODE=demo`: one button per persona |
| `(requester)/aide/`, `aide/nouveau/`, `aide/tickets/`, `aide/tickets/[id]/` | requesters | help home (A1), create (A3), my tickets (A2), ticket detail (A4) |
| `(requester)/notifications/`, `(requester)/compte/` | requesters | notifications, account |
| `support/`, `support/tickets/`, `support/tickets/[id]/`, `support/mes-tickets/` | support staff | dashboard (B1), queue (B2/B3), ticket workspace (B4), my tickets |
| `support/notifications/`, `support/rapports/` | support staff | notifications, reports |
| `support/admin/agents/`, `support/admin/categories/`, `support/admin/calendrier/` | manager / admin | agents (with « Rôles support » and « Anciens tickets Maplenou » for administrators), categories, SLA calendar (notice of the SLA mode from `admin/sla-settings`) |
| `acces-refuse/`, `not-found.tsx` | everyone | access denied, 404 |
| `api/backend/[...path]/`, `api/session/`, `api/demo-users/`, `api/auth-config/` | server only | BFF route handlers; `api/auth-config` returns the sign-in mode (`AUTH_MODE`, read at run time, `no-store`) |

`(requester)` is a route group (no URL segment). Its `layout.tsx` and `support/layout.tsx` are server components
that read the session, redirect to `/login` when there is none, and keep each audience in its own area
(requesters get `/acces-refuse` for `/support`; support staff other than administrators are sent to `/support`).
They wrap pages in `SessionProvider` and the `RequesterShell` / `SupportShell` navigation. These redirects are for
navigation only — the API enforces every permission.

The root `layout.tsx` reads the language and theme on the server, sets `<html lang>` and `data-theme`, and
provides `I18nProvider` and `ToastProvider`.

## BFF proxy and session cookie

The browser never holds the access token:

1. `login/` calls `POST /api/session` with `{phoneNumber, password}` (Maplenou account), then `{code}` for the
   two-factor step, or with a persona id (demo). That route handler refuses another origin, calls the backend's
   `POST /api/v1/auth/maplenou/login` / `/mfa` (forwarding `X-Forwarded-For`) or `POST /api/v1/dev/token` (demo
   profile only); while a 2FA code is awaited, the core's MFA token waits in the httpOnly cookie `maplenou_mfa`
   (path `/api/session`, 5 minutes; missing → `MFA_EXPIRED`). It stores the resulting support token in the cookie `maplenou_session`
   (`httpOnly`, `SameSite=Lax`, `Secure` when `SESSION_COOKIE_SECURE=true`, expiring with the token).
   `GET /api/session` returns the decoded user; `DELETE /api/session` clears the cookie.
2. Every API call from the browser goes to `/api/backend/api/v1/support/...`. The handler
   `src/app/api/backend/[...path]/route.ts`:
    - rejects `..` path segments and anything outside `api/v1/support` (so `/api/v1/dev/**` and actuator are unreachable);
    - for non-GET/HEAD requests, rejects an `Origin` whose host differs from the request host (CSRF defence in depth);
    - returns `401` without a session cookie;
    - forwards only `content-type`, `accept`, `if-match` plus `Authorization: Bearer <token>` to `BACKEND_URL`,
      streams the body, and copies back a fixed list of response headers.
3. `src/lib/session.ts` decodes the token payload for routing and display only (`decodeToken`); nothing there is
   trusted for authorization.

`BACKEND_URL` (default `http://localhost:8081`) is a server-side variable; in Compose it is `http://backend:8080`.

## Data fetching

`src/lib/api.ts` is the only HTTP client:

| Export | Use |
| --- | --- |
| `API_BASE` | `/api/backend/api/v1/support` |
| `api.get / post / put / del` | JSON calls; `post`/`put` serialize the body |
| `api.multipart(path, jsonPart, json, files)` | one JSON part (`ticket` or `message`) plus `files` |
| `ApiError` | thrown for non-2xx responses; `problem` holds the RFC 9457 body, `code` its stable code |
| `toQuery(params)` | query strings, repeating array values (`status=A&status=B`) |
| `clientId()` | id for idempotent message sending (`clientMessageId`) |

A `401` redirects to `/login?expired=1`. In client components, load data with `useApi` (`src/lib/useApi.ts`):

```tsx
const list = useApi<Page<TicketSummary>>(`/tickets?size=10&sort=updatedAt,desc`);
// list.data, list.error (ApiError), list.loading, list.reload(), list.setData(d); pass null to skip loading
```

`useApi` keeps the previous data while reloading and ignores responses for a path that is no longer current.

### Client cache of API reads (`apiCache.ts`)

`useApi` goes through `src/lib/apiCache.ts`, an in-memory cache of GET responses keyed by path:

- **stale-while-revalidate:** a screen shows the last known response for its path at once, then `useApi` always
  fetches it again and replaces it;
- **deduplication:** identical requests made at the same time share one network call (`dedupe`);
- **bounded:** at most 100 responses, the least recently used one is dropped first; nothing is written to disk, and
  nothing is cached during server rendering (browser only);
- **cleared** by `clearApiCache()` after every change (any non-GET call through `apiFetch` in `api.ts`) and at
  sign-out (`logout()` in `components/session.tsx`), so a user never keeps outdated data after an action nor sees a
  previous user's data.

Do not add another client cache: use `useApi`, and call the API through `api.*` so changes clear the cache. The
unit tests are in `src/lib/apiCache.test.ts`; see also [Caching and disk space](caching.md).
Render its states with `Loading`, `ErrorState` (with `onRetry={list.reload}`) and `EmptyState` from
`components/ui.tsx`. Show action buttons only when the ticket's `allowedActions` contains the action.

## Browser push notifications (Firebase)

The **Notifications** screens (`/notifications` for requesters, `/support/notifications` for support) show the
<span class="ui">Push notifications</span> card (`components/PushNotifications.tsx`, strings in
`messages/push.ts`). Its states: browser not supported, not configured on the server, blocked by the browser, off,
on; its buttons <span class="ui">Turn on push notifications</span>, <span class="ui">Send a test notification</span>
and <span class="ui">Turn off</span>.

| File | Role |
| --- | --- |
| `src/lib/push.ts` | detects the state without prompting, asks for permission, gets the FCM token and registers it (`POST /notifications/devices`, platform `WEB`), turns push off, sends a test, unsubscribes the browser at sign-out |
| `src/app/api/push-config/route.ts` | returns the public `FIREBASE_WEB_*` values read from the server environment at run time (`Cache-Control: no-store`), or `{ configured: false }`; no rebuild when they change |
| `public/push-sw.js` | service worker: shows the push and, on click, opens the app on the ticket (`/?ticket=<id>`); no caching, no network request, no Firebase SDK |

The Firebase JavaScript SDK (`firebase` 12.19.0 in `package.json`) is loaded with a dynamic `import()` only when the
user turns push on or off, so other pages never download it. Once push is on, **Firebase keeps its token in the
browser's IndexedDB** — the one documented exception to the "no IndexedDB/localStorage" rule, required by Firebase;
the app itself stores nothing. `logout()` unsubscribes the browser so the next user does not get the previous user's
pushes. Setup and tests: [Firebase push notifications](firebase.md).

## Languages (FR/EN) and themes

This summarizes `frontend/I18N.md`, which is the reference.

- French is the default; English is complete. The choice is stored in the cookie `maplenou_locale`, the theme
  (`system`, `light`, `dark`) in `maplenou_theme` (one year, `SameSite=Lax`), so the server renders the right
  language and theme on the first paint. Without a cookie the language follows `Accept-Language`, then French
  (`negotiateLocale`).
- `components/Preferences.tsx` is the switch shown in the header, sidebar and login page. `usePreferences()` writes
  the cookie, updates `<html lang>` or `data-theme` and calls `router.refresh()`.

| File | Content |
| --- | --- |
| `src/lib/i18n/config.ts` | `LOCALES`, `THEMES`, cookie names, `negotiateLocale`, `INTL_LOCALE` |
| `src/lib/i18n/define.ts` | `defineMessages`, `format("{x}", vars)`, `plural(locale, n, one, other)` |
| `src/lib/i18n/client.tsx` | `I18nProvider`, `useMessages`, `useLabels`, `useFormat`, `useErrorText`, `useLocale`, `useDocsUrl`, `usePreferences` |
| `src/lib/i18n/server.ts` | `getLocale`, `getTheme`, `getMessages`, `DOCS_BASE_URL` |
| `src/lib/i18n/messages/*.ts` | one namespace per screen or component group; `common.ts`, `errors.ts`, `notifications.ts` |

Rules from `I18N.md`: no user-visible literal in components (including `aria-label`, `title`, placeholders, toasts);
`format()` for variables, never concatenation; `plural()` for counts; `useLabels()` for statuses, priorities, roles,
SLA states and history actions; `useFormat()` for dates, money, sizes and durations; `useErrorText()` for API errors
(never `e.message`); French typography (non-breaking space before `:` `;` `?` `!`, « » quotes); business data
(category names, subjects, messages) is shown as stored.

### Add a string or a namespace

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

`defineMessages` type-checks that `en` has exactly the keys of `fr`: a missing translation fails
`npm run typecheck`. Add a key to both languages in the same change.

The documentation link in the shells uses `useDocsUrl()` → `${DOCS_BASE_URL}/<site>/<locale>/`, where
`DOCS_BASE_URL` comes from the environment (`DOCS_PUBLIC_URL` in Compose).

## Design tokens

Colours, radii, shadows, spacing and fonts are CSS custom properties at the top of `src/app/globals.css`
(`--brand`, `--accent`, `--bg`, `--surface`, `--text`, `--text-secondary`, `--info`, `--success`, `--warning`,
`--serious`, `--danger`, `--neutral`, `--note`, `--focus`, `--radius*`, `--space-1…6`, `--font`, `--mono`…). Dark
values apply with `<html data-theme="dark">`, or when the operating system is dark and the user has not chosen
`light`. Never hard-code a colour in a component: reuse or add a token in both themes and keep WCAG AA contrast.
The documentation sites reuse the brand colours (`documentation/shared/stylesheets/maplenou.css`). The design rules
are in [Design system](../ux-ui/design-system.md).

## Accessibility rules

WCAG 2.2 is the baseline. The components already provide:

- a skip link to `#main` in both shells and a visible `:focus-visible` outline (`--focus`);
- `role="status"` + `aria-live="polite"` for loading states and toasts, `role="alert"` for errors;
- a native `<dialog>` (`ConfirmDialog`) for deliberate final actions (resolve, close);
- `.sr-only` labels for icon-only controls, `prefers-reduced-motion` respected for skeletons;
- status badges with text, never colour only.

When you build a screen: one `<h1>`, semantic headings, a `<label>` for every field, keyboard-reachable controls in a
logical order, tabs with `role="tablist"`/`role="tab"` and `aria-selected`, translated `aria-label`s, and usable
layouts on a phone (the e2e suite has a mobile project).

## Add a screen

1. Create `src/app/<area>/<route>/page.tsx` under `(requester)` or `support` so the right layout, session and
   shell apply. Use `"use client"` when the page uses hooks.
2. Create its namespace in `src/lib/i18n/messages/<screen>.ts` (FR and EN).
3. Load data with `useApi`, mutate with `api.*`, show errors with `useErrorText()` and `useToast()`.
4. Drive every action button from `allowedActions`; never re-implement a permission rule in the UI.
5. Style with existing classes and tokens from `globals.css`.
6. Add a link in `RequesterShell` or `SupportShell` if it is a navigation entry (label in `shell.ts`).
7. Add a unit/component test (`*.test.tsx`, Vitest + Testing Library) and, for a key journey, a Playwright step.
8. Document the screen in the user or staff guide, in both languages.

Quality gate: `npm run lint && npm run typecheck && npm test && npm run build` (or `./scripts/test.sh frontend`).
