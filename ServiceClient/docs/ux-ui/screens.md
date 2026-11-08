# Design deliverables — mockups and screens

The design system of CS-02 (`design-system.md`) exists in three synchronised forms:

| Form | Where | Content |
| --- | --- | --- |
| **Design canvas (Claude Design)** | [Maplenou Service client — Mockups](https://claude.ai/artifact/GEnhzdFMLMLW8kWWd6hhcm) (owner's Claude account, private until shared) | 163 artboards on five pages: *Overview* (cover, colours, status badges, the six screen components) and one page per language × theme — *English · Light*, *English · Dark*, *Français · Clair*, *Français · Sombre*. Each language page holds thirteen screens at desktop 1440×900, tablet 834×1112 and mobile 390×844 — requester: help home (with the help search), new ticket, my tickets, resolved ticket (rating and reopening), notifications (push), account; support: dashboard (with satisfaction), queue (CSV export), ticket workspace (canned replies, assistant draft, actions on Maplenou), reports, statuses and priorities, permissions per role, replies and messages (CS-12). A page never mixes languages or themes. Components: `HelpHome`, `NewTicket`, `SupportDashboard`, `TicketWorkspace`, `RequesterScreens` and `SupportScreens` (`screen` property). |
| **Design catalogue in the app** | `/design` (`frontend/src/app/design/page.tsx`) | every token and shared component rendered by the real CSS and React components, switchable between FR/EN and light/dark/system |
| **Screens as built** | this page (`docs/ux-ui/screens/`) | screenshots of the running application and documentation, both languages and both themes |

The canvas uses the colour values of `frontend/src/app/globals.css` (light and dark) and the texts of
`frontend/src/lib/i18n/messages/`. When a token or a label changes, update the code first, then the matching
screen component of the canvas (`HelpHome`, `NewTicket`, `SupportDashboard`, `TicketWorkspace`, `RequesterScreens`, `SupportScreens`): every artboard
of every page is an instance of these four components with `locale`, `theme` and `device` set, so one change
reaches all 48 screens.

!!! info "Figma file retired"
    The earlier Figma file (iteration 2) is replaced by the design canvas on the owner's request (iteration 3). It
    is no longer updated and may be deleted by its owner.

!!! note "How the screenshots were produced"
    With the demo stack (`docker compose up`), Playwright opens each screen with the `maplenou_locale` and
    `maplenou_theme` cookies set, at 1440×900 (desktop) or 390×844 (mobile). They show demo data only.

## Screens as built

| Screen | Image |
| --- | --- |
| 01 Login — FR light | ![01 Login — FR light](screens/01-login-fr-light.png){ width="360" loading=lazy } |
| 02 Login — EN dark | ![02 Login — EN dark](screens/02-login-en-dark.png){ width="360" loading=lazy } |
| 03 Requester help home — FR light (mobile) | ![03 Requester help home — FR light (mobile)](screens/03-requester-help-home-fr-light-mobile.png){ width="360" loading=lazy } |
| 04 Requester help home — EN dark (mobile) | ![04 Requester help home — EN dark (mobile)](screens/04-requester-help-home-en-dark-mobile.png){ width="360" loading=lazy } |
| 05 Create ticket — FR light (mobile) | ![05 Create ticket — FR light (mobile)](screens/05-create-ticket-fr-light-mobile.png){ width="360" loading=lazy } |
| 06 My tickets — EN light (mobile) | ![06 My tickets — EN light (mobile)](screens/06-my-tickets-en-light-mobile.png){ width="360" loading=lazy } |
| 07 Notifications — FR dark (mobile) | ![07 Notifications — FR dark (mobile)](screens/07-notifications-fr-dark-mobile.png){ width="360" loading=lazy } |
| 08 Account & preferences — EN light (mobile) | ![08 Account & preferences — EN light (mobile)](screens/08-account-preferences-en-light-mobile.png){ width="360" loading=lazy } |
| 09 Support dashboard — FR light | ![09 Support dashboard — FR light](screens/09-support-dashboard-fr-light.png){ width="360" loading=lazy } |
| 10 Support dashboard — EN dark | ![10 Support dashboard — EN dark](screens/10-support-dashboard-en-dark.png){ width="360" loading=lazy } |
| 11 Queue — EN light | ![11 Queue — EN light](screens/11-queue-en-light.png){ width="360" loading=lazy } |
| 12 Ticket workspace — FR light | ![12 Ticket workspace — FR light](screens/12-ticket-workspace-fr-light.png){ width="360" loading=lazy } |
| 12b Ticket workspace — EN dark | ![12b Ticket workspace — EN dark](screens/12b-ticket-workspace-en-dark.png){ width="360" loading=lazy } |
| 13 Reports — FR light | ![13 Reports — FR light](screens/13-reports-fr-light.png){ width="360" loading=lazy } |
| 14 Admin categories — EN dark | ![14 Admin categories — EN dark](screens/14-admin-categories-en-dark.png){ width="360" loading=lazy } |
| 15 Docs user guide — FR light with assistant | ![15 Docs user guide — FR light with assistant](screens/15-docs-user-guide-fr-light-with-assistant.png){ width="360" loading=lazy } |
| 16 Docs staff guide — EN dark with assistant | ![16 Docs staff guide — EN dark with assistant](screens/16-docs-staff-guide-en-dark-with-assistant.png){ width="360" loading=lazy } |
