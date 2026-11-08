# CS-04 Validation — Frontend Foundation

**Result:** PASSED (technical validation)
**Decision:** AWAITING PROJECT-OWNER APPROVAL
**Dependency:** CS-02 (approved)
**Validation date:** 2026-10-08

## Delivered

Next.js 16 / React 19 / TypeScript application in `frontend/`:

| CS-02 screen | Route |
| --- | --- |
| A1 Help & Support (role-specific: client, vendeur, livreur) | `/aide` |
| A2 My Tickets | `/aide/tickets` |
| A3 Create Ticket | `/aide/nouveau` |
| A4 Requester Ticket Detail | `/aide/tickets/[id]` |
| B1 Support Dashboard | `/support` |
| B2 Ticket Queue (search + filters) | `/support/tickets` |
| B3 Support My Tickets | `/support/mes-tickets` |
| B4 Support Ticket Detail (context, conversation, internal notes, history, attachments, actions, SLA) | `/support/tickets/[id]` |
| C1 Notifications | `/notifications`, `/support/notifications` |
| C2 Profile / Account | `/compte` |
| C3 Access Denied | `/acces-refuse` |
| Administration (IA "Rapports", "Paramètres") | `/support/rapports`, `/support/admin/agents`, `/support/admin/categories`, `/support/admin/calendrier` |

Design system: semantic tokens (light and dark), status/priority/SLA badges that always carry a text label,
buttons, forms with labels and inline validation, alerts, empty/loading/error states with retry, toasts,
confirmation dialogs for final actions, single-hue charts with values as text.

**Note on sequencing:** the roadmap allowed CS-04 to use test data first. Because CS-05 was delivered in the same
iteration, the screens were wired directly to the real API (CS-06) instead of a throw-away mock layer.

## Checks (CS-02 acceptance criteria)

| Area | Result | Evidence |
| --- | --- | --- |
| Screen coverage (AC-SI-001…010) | PASS | routes above |
| Navigation and IA (AC-IA-001…012) | PASS | requester bottom navigation, support sidebar by role, breadcrumbs on detail |
| Behaviour and states (AC-SB-001…022) | PASS | loading/empty/error/retry states, deliberate confirmations, duplicate-safe sending |
| Wireframes (AC-WF-001…017) | PASS | screenshots reviewed at 390 px, 420 px and 1440 px |
| Design system (AC-DS-001…020) | PASS | `globals.css`, `components/ui.tsx` |
| Accessibility baseline WCAG 2.2 (AC-UX-013) | PASS for the baseline: semantic headings, labels, focus outlines, skip link, keyboard-reachable controls, non-colour status, reduced motion, forced colours. A full audit with assistive technologies is still recommended. |
| Mobile (AC-UX-014) | PASS | Playwright `@mobile` scenario on a Pixel 7 profile |
| Lint, type-check, unit tests, production build | PASS | 22 Vitest tests |
