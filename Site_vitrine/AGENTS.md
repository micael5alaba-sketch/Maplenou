# AGENTS.md — Site_vitrine (public website)

Public marketing website of the Maplenou marketplace. Separate from the ticketing app (`../ServiceClient`) and the
admin back-office (`../Admin_frontend`). Next.js 16.4 (App Router, standalone output), React 19.3, TypeScript, no
UI library, self-hosted fonts (`@fontsource-variable/*`, no Google Fonts request).

## Rules

- **Facts, not marketing invention.** Prices, commissions, delays, zones and documents come from Bakend
  (`Bakend/CLAUDE.md`, `order/CommissionService`; fees: the owner's document "Maplenou - Exigences des nouveaux
  frais vendeurs et commissions acheteurs", not stored in the repository) and the customer-service rules
  (`ServiceClient/docs`, `SlaTargets`). Change a fact here only when it changed there. The example receipt on the
  home page is labelled "Exemple".
- **Two languages.** Every text lives in `src/lib/messages.ts` (`fr` is the reference, `en` has the same keys —
  enforced by the type and `messages.test.ts`). No text in components.
- **Themes:** light, dark, system — CSS variables in `src/app/globals.css`, never raw colours in components. Dark
  accent = Maplenou yellow `#a76f07` (black text on it); small yellow text uses `--accent-ink` `#c0974c` (AA contrast).
- **Images:** marketing visuals through `components/Visual.tsx` (WebP in `public/images/visuels`, 2 widths, alt text
  in `messages.ts` → `visuals`). Originals stay in `design/originals/` (git-ignored). Never take images from
  `../frontend/assets` (mobile app).
- **Live data is optional.** `src/lib/catalog.ts` reads only public Bakend endpoints, with a 2.5 s timeout and a
  5-minute Next.js cache; a missing API must never break a page (fallback content).
- **Preferences** live in two cookies (`maplenou_locale`, `maplenou_theme`); no localStorage/IndexedDB.
- **Configuration** comes from `.env` (`.env.example` documents every variable); `docker-compose.yml` reads each
  value as `${VAR:?…}`. Port 3002 (3000/3001/8080/8081/8006 belong to the other apps; never use 8000).
- **Layout** follows the *type* of a large telco site (audience mega-menu, dark hero band, product cards in FCFA,
  help block, shop finder `/boutiques`, newsletter + socials). Never copy another brand's logo, texts or photos,
  and never invent customer reviews (ratings shown come from Bakend `averageRating`/`reviewCount`).
- **Forms work without JavaScript:** shop filter is a GET form; the newsletter is a Server Action
  (`src/app/actions.ts`) that redirects with `?newsletter=ok|invalid|error#newsletter`.
- Accessibility: WCAG 2.2 AA (labels, skip link, `aria-current`, focus ring, reduced motion).
- Pinned versions; `npm ci` must stay reproducible.

## Check before committing

`npm run lint && npm run typecheck && npm test && npm run build`, then look at `/`, `/vendre`, `/livrer`,
`/service-client`, `/boutiques` (and the mega-menu open) in light and dark, desktop and 390 px wide (no horizontal scroll).
