# CS-08 Validation — Notifications and Testing

**Result:** PASSED (technical validation)
**Decision:** AWAITING PROJECT-OWNER APPROVAL
**Dependencies:** CS-06, CS-07
**Validation date:** 2026-10-08

## Notifications (Firebase)

| Roadmap trigger | Result |
| --- | --- |
| Nouveau ticket | PASS — requester confirmation + managers |
| Réponse de l'agent | PASS |
| Changement de statut | PASS |
| Demande d'informations supplémentaires | PASS |
| Ticket résolu | PASS (and closed, reopening decisions, assignment, escalation) |
| Failure isolation (BR-020, AC-011) | PASS — `NotificationIT.ac011_*`, `NotificationServiceTest` |

Push uses Firebase Cloud Messaging when `FIREBASE_CREDENTIALS_FILE` is set; it was tested with a simulated provider
(success, failure with retries, no device). **A real Firebase project and device test remain to be done** once
credentials and the Flutter apps are available. See [Notifications](../architecture/notifications.md).

## Roadmap scenarios

| Scenario | Automated test | Result |
| --- | --- | --- |
| Client crée un ticket → reçoit une réponse → fournit des informations → résolu → fermé | `ScenarioIT.customer_*` + browser e2e | PASS |
| Vendeur : problème de paiement → ticket → vérification du paiement → résolution | `ScenarioIT.seller_*` | PASS |
| Livreur : client absent → signalement → analyse → nouvelle instruction | `ScenarioIT.driver_*` + browser `@mobile` | PASS |

## Test suite

| Suite | Count | Result |
| --- | ---: | --- |
| Backend unit tests (JUnit) | 53 | PASS |
| Backend integration tests (HTTP + security + PostgreSQL 16) | 62 | PASS |
| Frontend unit/component tests (Vitest + Testing Library) | 22 | PASS |
| Browser end-to-end (Playwright, desktop + mobile) | 4 | PASS |
| **Total** | **141** | **PASS** |

CI runs all of them: `backend.yml`, `frontend.yml`, `e2e.yml` (and `docs.yml` for MkDocs).
