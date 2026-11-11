# AGENTS.md — frontend (Next.js web app)

Read the root `AGENTS.md` and `frontend/I18N.md` first.

## Layout

- `src/app/(requester)/…` customer/seller/driver screens (`/aide`, `/notifications`, `/compte`), mobile-first.
- `src/app/support/…` back-office (dashboard, queue with CSV export, ticket workspace with `TicketTools`, reports,
  admin: agents, categories, SLA calendar and targets, statuses and priorities, replies and messages, permissions).
- `src/app/api/backend/[...path]` BFF proxy: adds the token from the httpOnly cookie; `src/app/api/session` sign-in.
- `src/components/` shared UI (`ui.tsx`, shells, `Preferences.tsx` language + theme switch).
- `src/lib/i18n/` languages (FR/EN), `src/lib/labels.ts` vocabularies and formatters, `src/lib/api.ts` client.
- `src/app/design/` design catalogue (tokens, components, states in both themes).

## Rules

- No user-visible literal in components: use a namespace in `src/lib/i18n/messages/` (`defineMessages({ fr, en })`).
  Hooks: `useMessages`, `useLabels`, `useFormat`, `useErrorText`. Never show `error.message` directly.
- The browser never sees the access token; call the API only through `api` / `useApi` (`/api/backend/...`).
- Colours, spacing and radii come from CSS variables in `src/app/globals.css`; both themes must stay AA-readable.
- Keep French wording identical to the CS-02 vocabulary; English is its translation.
- Statuses and priorities are configurable (`GET /vocabulary`, `src/lib/vocabulary.tsx`): never hard-code a list of
  codes; use `statusCodes()`, `openStatusCodes()`, `priorityCodes()` (higher rank = more urgent) and decide by
  `statusKind`. `useLabels()` already merges the administrators' FR/EN labels.
- Docs links: `useDocsUrl()("user" | "staff")` → `${DOCS_BASE_URL}/<site>/<locale>/`.
- Client cache: `src/lib/apiCache.ts` (stale-while-revalidate for `useApi` GETs, 100 entries LRU, request
  de-duplication, **memory only**). `api` clears it after every successful write and `session` clears it on logout.
  Never persist API data in localStorage, sessionStorage or IndexedDB.
- Text on coloured fills uses `--text-on-brand`, `--text-on-accent`, `--text-on-danger`; never `#fff`.
- Mockups: Claude Design canvas (`DESIGN_CANVAS_URL` in `src/app/design/page.tsx`, override with
  `NEXT_PUBLIC_DESIGN_CANVAS_URL`).
- Browser push: `src/lib/push.ts` (Firebase SDK loaded by dynamic import only when the user turns push on),
  `components/PushNotifications.tsx` (Notifications screens), `public/push-sw.js` (shows pushes, caches nothing),
  `/api/push-config` (public `FIREBASE_WEB_*` values read at run time — never `NEXT_PUBLIC_*` for them).
  Logout unsubscribes the browser. `/?ticket=<id>` redirects to the ticket for the signed-in role.
- Dockerfile: `deps` → `builder` (npm and `.next/cache` cache mounts) → `runtime` (standalone output, non-root).

## Guide screenshots

`node scripts/guide-screenshots.mjs` (demo stack running) retakes the 32 annotated pictures of the user and staff
guides; after a UI change that moves a marked element, update the `SHOTS` table and the legend of the page.

## Test

`npm ci --legacy-peer-deps && npm run lint && npm run typecheck && npm test && npm run build`;
`npx playwright test` against a running stack (French browser locale; one test switches language and theme).
Without Node: `scripts/test.sh frontend` / `scripts/test.sh e2e`.
