# Ecosystem Integration (CS-07)

**Status:** Implemented behind a port with a demo adapter. The HTTP contract below is a **proposal** to agree with
the Maplenou core team before switching `ECOSYSTEM_MODE=http`.

*Amended 2026-10-29 by DEC-CS10-17:* Bakend now implements this read-only API in its package
`com.maplenou.backend.supportbridge` (plus `GET /internal/v1/support-tickets` for the import of the former tickets);
setup, mapping and rollout are in [Maplenou platform integration](../guide/maplenou-integration.md).

## What support sees

From the ticket screen (B4), support staff see — without leaving the ticket (functional design §7–§9, §20):

| Flow (roadmap) | Context shown |
| --- | --- |
| Client → Ticket → Commande → Détails | order number, date, amount, status, seller |
| Vendeur → Ticket → Commande → Paiement → Commission / Reversement | payment status, provider (FedaPay/Stripe), commission, payout status and amount, seller balance |
| Client → Ticket → Commande → Livraison → Livreur | delivery number, driver, status, destination, time slot, last update |
| Remboursement | refund number, status, amount |

## Linking rules (BR-019)

When a ticket is created, a requester may attach a reference they take part in:

| Requester | May link | Derived automatically |
| --- | --- | --- |
| Customer | an order they bought, or a delivery of such an order | payment, delivery, seller, refund references |
| Seller | an order they sold | same |
| Delivery driver | a delivery assigned to them | the delivery's order and its references |

Any other reference is rejected with `REFERENCE_NOT_ACCESSIBLE` (the same answer whether it exists or not).
Context is read **only** by support staff (`GET /tickets/{id}/context`). Each section degrades independently: if a
module is unavailable the section shows "service momentanément indisponible" and the ticket stays usable.

## Port

`com.maplenou.support.context.EcosystemGateway` — read-only:
`findOrder`, `findPayment`, `findDelivery`, `findSeller`, `findRefund`, `ordersOf(userId)`, `deliveriesOf(driverId)`.

| Adapter | When | Notes |
| --- | --- | --- |
| `DemoEcosystemGateway` | `maplenou.ecosystem.mode=demo` (default) | Consistent sample data for the demo personas (orders `CMD-2026-0456`, `-0789`, `-0501`) |
| `HttpEcosystemGateway` | `mode=http` | Calls the core backend (Bakend) at `MAPLENOU_CORE_URL` with the header `X-Maplenou-Service-Token: <SUPPORT_SERVICE_TOKEN>`, 3 s timeout. *Amended 2026-10-29 by DEC-CS10-17: formerly `ECOSYSTEM_BASE_URL` with `Authorization: Bearer ECOSYSTEM_SERVICE_TOKEN`.* |

## Proposed HTTP contract (core backend → support module)

All endpoints are internal (service-to-service), JSON, `404` when unknown. Field names match the records in
`EcosystemGateway`.

| Endpoint | Response |
| --- | --- |
| `GET /internal/v1/orders/{ref}` | `{ref, customerId, customerName, sellerUserId, sellerRef, sellerName, orderedAt, amount, currency, status, paymentRef, deliveryRef, refundRef}` |
| `GET /internal/v1/payments/{ref}` | `{ref, orderRef, status, amount, currency, provider, commissionAmount, payoutStatus, payoutAmount, paidAt}` |
| `GET /internal/v1/deliveries/{ref}` | `{ref, orderRef, customerName, driverId, driverName, status, pickedUpAt, lastUpdateAt, destination, timeSlot}` |
| `GET /internal/v1/sellers/{ref}` | `{ref, sellerUserId, shopName, ownerName, accountStatus, balance, currency}` |
| `GET /internal/v1/refunds/{ref}` | `{ref, orderRef, status, amount, currency, requestedAt}` |
| `GET /internal/v1/users/{id}/orders` | orders bought or sold by the user, most recent first |
| `GET /internal/v1/drivers/{id}/deliveries` | deliveries assigned to the driver, most recent first |

Amounts are decimals in the given currency (`XOF` displayed as FCFA). User ids are the same UUIDs as the token `sub`.

## Out of scope for the MVP

Actions on other modules from the ticket (issue a refund, re-schedule a delivery, contact the driver from the
app) are not implemented: the functional design lists them as agent capabilities "selon les permissions", but
no approved rule defines them yet. They are recorded in the upgrade plan.
