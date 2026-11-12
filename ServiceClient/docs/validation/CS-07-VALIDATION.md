# CS-07 Validation — Ecosystem Integration (orders, payments, deliveries)

**Result:** PASSED with the demo adapter
**Decision:** AWAITING PROJECT-OWNER APPROVAL — and agreement on the HTTP contract with the core team
**Dependency:** CS-05
**Validation date:** 2026-10-08

| Roadmap flow (Oct. week 3) | Result | Evidence |
| --- | --- | --- |
| Client → Ticket → Commande → Détails | PASS | `ContextIT.ac009_*`, B4 "Commande" panel |
| Vendeur → Ticket → Commande → Paiement → Commission / Reversement | PASS | `ContextIT.sellerLinksAnOrderTheySold`, `ScenarioIT.seller_*` |
| Client → Ticket → Commande → Livraison → Livreur | PASS | `ContextIT.driverLinksTheirDeliveryAndTheOrderIsDerived`, `ScenarioIT.driver_*` |
| Agent consults this information from the ticket | PASS | e2e scenario checks order and driver on B4 |
| References restricted to what the requester takes part in (BR-019) | PASS | `ContextIT.br019_*` |
| Unavailable module does not break the ticket | PASS by design — each section degrades independently (`TicketContextService`) |

## Limitation

The Maplenou core services (orders, payments, deliveries) are not in this repository, so the integration runs on
`DemoEcosystemGateway`. `HttpEcosystemGateway` implements the [proposed contract](../architecture/ecosystem-integration.md);
it must be validated against the real services before the pilot.
