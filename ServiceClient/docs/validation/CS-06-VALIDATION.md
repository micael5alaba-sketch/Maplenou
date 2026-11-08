# CS-06 Validation — Frontend/Backend Integration

**Result:** PASSED (technical validation)
**Decision:** AWAITING PROJECT-OWNER APPROVAL
**Dependencies:** CS-04, CS-05
**Validation date:** 2026-10-08

| Roadmap item (Oct. week 2) | Result | Evidence |
| --- | --- | --- |
| Créer un ticket via l'API | PASS | e2e scenario (with attachment) |
| Afficher les tickets via l'API | PASS | requester list, support queue with filters |
| Ouvrir les détails | PASS | A4 and B4 |
| Envoyer des messages | PASS | e2e: agent and customer messages, idempotent retry id |
| Modifier le statut | PASS | e2e: information request, resolve, close with confirmation |
| Attribuer un agent | PASS | e2e: "Prendre en charge"; manager reassignment dropdown |
| Ajouter des notes internes | PASS | e2e: the note is visible to support and absent from the customer view |

Integration design: the browser calls a same-origin Next.js proxy (`/api/backend/...`) that adds the access token
from an httpOnly cookie; only `/api/v1/support/**` is reachable through it. Every button shown comes from the
`allowedActions` returned by the API, and the API re-checks each action.
