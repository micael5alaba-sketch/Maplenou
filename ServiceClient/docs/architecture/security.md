# Security Review (CS-09)

Scope: roadmap "Sécurité" list and BR-073..BR-076, checked against the OWASP Authorization Cheat Sheet
(REFERENCES.md). Status: **controls implemented and tested — a production review is still required before a pilot**
(see "Before production").

| Roadmap item | Control | Evidence |
| --- | --- | --- |
| Contrôle d'accès basé sur les rôles | Roles from the token (`roles` claim); endpoint guards (`@PreAuthorize`) for dashboard, reports, administration | `AccessControlIT.ac024_*`, `requestersCannotUseSupportScreens` |
| Authentification des API | Every `/api/v1/**` call needs a valid JWT (signature, expiry, issuer); everything else is denied by default; stateless | `TicketCreationIT.ac002_*`, `AccessControlIT.invalidTokenIsRejected` |
| Autorisation d'accès aux tickets | `TicketAccessPolicy` decides each action from role + ownership + assignment (BR-071); requesters receive 404 for others' tickets; requester filters cannot widen scope | `TicketAccessPolicyTest`, `AccessControlIT`, `ReopenIT`, e2e "view but not act" |
| Protection des notes internes | Separate `visibility`; requester queries are forced to `PUBLIC`; internal attachments return 404 to requesters | `AccessControlIT.ac007_*`, e2e scenario |
| Sécurisation des pièces jointes | Extension allow-list + content signature check, 10 MB / 5 files / 50 MB, sanitized file names, server-generated storage keys, private storage, download only through the authorized endpoint with `attachment` disposition and `nosniff` | `AttachmentPolicyTest`, `AttachmentIT` |
| Journalisation et audit | Append-only `ticket_history` (database trigger) with actor, role, action, values, timestamp for every significant change | `HistoryImmutabilityIT`, `LifecycleIT.ac004_*` |
| Protection contre les accès non autorisés | Deactivated support accounts blocked; demo login only in the `demo` profile; dev endpoints unreachable through the web proxy; CSRF: httpOnly SameSite=Lax cookie + origin check on the proxy; security headers (frame-deny, nosniff, referrer policy) | `AccessControlIT.deactivatedSupportAccountIsBlocked`, manual proxy checks (validation report) |
| Fuite d'information par les erreurs | RFC 9457 messages without stack traces or SQL; generic 500 text | `ApiExceptionHandler`, `server.error.include-*: never` |
| Intégrité en cas d'échec | One transaction per command; attachments validated before any write and deleted if the transaction rolls back; notifications after commit | `AttachmentIT.ac017_*`, `NotificationIT.ac011_*` |
| Concurrence | Row lock per ticket command + optimistic version; idempotent message sending | `LifecycleIT.duplicateMessageRetriesAreIgnored` |

## Before production (not done in this repository)

1. Connect the real Maplenou identity provider (`spring.security.oauth2.resourceserver.jwt.issuer-uri`), remove
   `JWT_SECRET`, never run the `demo` profile; replace the demo login page with the provider's sign-in.
2. Serve both apps over HTTPS only; set `SESSION_COOKIE_SECURE=true`; restrict `CORS_ALLOWED_ORIGINS`.
3. Store secrets (database, Firebase, ecosystem service token) in a secret manager, not in compose files.
4. Add antivirus scanning for uploads (or move to Cloudinary authenticated delivery) and backups for attachments.
5. Add rate limiting on ticket creation and messages, centralized logs and alerts (observability items in the
   upgrade plan).
6. Run a dependency vulnerability scan (e.g. OWASP Dependency-Check / `npm audit`) in CI and an external penetration test before the pilot.
