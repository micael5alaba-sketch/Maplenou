# CS-09 Final Validation — Security and MVP Readiness

**Result:** PASSED for an internal demonstration
**Decision:** AWAITING PROJECT-OWNER APPROVAL
**Dependency:** CS-08
**Validation date:** 2026-10-08

## Security

All seven roadmap security items are implemented and tested — see the [security review](../architecture/security.md).
Additional manual checks on the running stack:

| Check | Expected | Observed |
| --- | --- | --- |
| State-changing request through the web proxy from a foreign `Origin` | 403 | 403 |
| Demo token endpoint through the web proxy | not reachable | 404 |
| Path traversal attempt through the proxy | rejected | 400 |
| API call without session | 401 | 401 |
| Session cookie | httpOnly, SameSite=Lax | confirmed |
| Security headers on pages | frame deny, nosniff, referrer policy, permissions policy | confirmed |

## UX improvements (roadmap "Amélioration UX")

| Item | Result |
| --- | --- |
| Messages d'erreur | Business messages in French from the API, field-level validation, retry actions |
| États de chargement | Skeletons with `role="status"`; buttons disabled while sending (no duplicates) |
| États vides | Distinguish "no ticket yet" from "no result for these filters" |
| Responsive mobile | Mobile-first requester app; support screens collapse to one column; tested at 390 px |
| Recherche et filtrage | FR-014 implemented (queue search + 8 filters + sort + pagination, filters kept in the URL) |
| Performance | Indexed queries, paginated lists, one query per list page (entity graph), production build |

## Final validation scenario (roadmap §5)

*CLIENT → Création du ticket → Catégorisation → Agent → Vérification de la commande → Vérification de la livraison →
Réponse au client → Notification → Résolution → Fermeture* — executed in a real browser by
`frontend/e2e/scenario.spec.ts` (including a reopening round-trip and the internal-note confidentiality check): **PASS**.

## Readiness

The MVP is **ready for an internal demonstration** (`docker compose up --build`, demo profile).
It is **not yet ready for a pilot** until: the real identity provider is connected, the core-service HTTP contract
is validated, Firebase is configured and tested on devices, the official holiday calendar is loaded, and the
"Before production" items of the security review are done.
